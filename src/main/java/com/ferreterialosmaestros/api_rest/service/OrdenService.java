package com.ferreterialosmaestros.api_rest.service;

import com.ferreterialosmaestros.api_rest.model.Orden;
import com.ferreterialosmaestros.api_rest.model.CuentaBancaria;
import com.ferreterialosmaestros.api_rest.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OrdenService {
    private final OrdenRepository ordenes;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final ConfiguracionRepository configuracion;

    public record Linea(@NotNull @Positive Long productoId, @Positive @Max(10000) int cantidad) {}
    public record Nueva(@NotEmpty @Size(max=100) List<@NotNull @Valid Linea> items,
        @NotNull @Pattern(regexp="retiro|despacho") String tipoEntrega,
        @Size(max=250) String direccion,
        @NotNull @Pattern(regexp="normal|transferencia|cuenta_corriente") String formaPago) {}
    public record Resumen(Long id, String clienteCorreo, Instant fechaISO, String tipoEntrega, String direccion,
        String estado, String formaPago, String estadoPago, CuentaBancaria datosTransferencia, long total, List<Orden.Detalle> items) {}

    public OrdenService(OrdenRepository ordenes, ProductoRepository productos, UsuarioRepository usuarios, ConfiguracionRepository configuracion) {
        this.ordenes = ordenes;
        this.productos = productos;
        this.usuarios = usuarios;
        this.configuracion = configuracion;
    }

    @Transactional
    public Resumen crear(String correo, Nueva datos) {
        if ("despacho".equals(datos.tipoEntrega()) && (datos.direccion() == null || datos.direccion().trim().length() < 5))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa una dirección de despacho válida");
        var cantidades = new TreeMap<Long, Integer>();
        for (var item : datos.items()) {
            if (cantidades.putIfAbsent(item.productoId(), item.cantidad()) != null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto repetido en la orden");
        }

        var orden = new Orden();
        orden.cliente = usuarios.bloquearPorCorreo(correo).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        orden.tipoEntrega = datos.tipoEntrega();
        orden.direccion = "despacho".equals(datos.tipoEntrega()) ? datos.direccion().trim() : "";
        orden.formaPago = datos.formaPago();
        if ("transferencia".equals(orden.formaPago)) {
            orden.estadoPago = "Por confirmar";
            orden.datosTransferencia = configuracion.findById(1L).map(fila -> fila.cuenta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "La tienda aún no ha configurado la transferencia"));
        }
        for (var item : cantidades.entrySet()) {
            var p = productos.bloquearPorId(item.getKey()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
            if (p.getStock() < item.getValue())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente de " + p.getNombre());
            long precio = p.getDescuento() == 0 ? p.getPrecio() : Math.round(p.getPrecio() * (100L - p.getDescuento()) / 1000.0) * 10;
            p.setStock(p.getStock() - item.getValue());
            orden.items.add(new Orden.Detalle(p.getId(), p.getNombre(), item.getValue(), precio));
            orden.total += precio * item.getValue();
        }
        ordenes.save(orden);
        if ("cuenta_corriente".equals(orden.formaPago)) {
            var u = orden.cliente;
            if (!u.cuentaCorriente || orden.total > u.cupoCuentaCorriente - u.saldoPendiente)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La compra supera el cupo disponible o la cuenta está deshabilitada");
            u.saldoPendiente += orden.total;
            u.movimientosCC.add(new com.ferreterialosmaestros.api_rest.model.Usuario.Movimiento(
                Instant.now(), "compra", orden.total, "Compra N° " + orden.id, null));
        }
        return resumen(orden);
    }

    public List<Resumen> listar(String correo, boolean cliente) {
        return (cliente ? ordenes.findByClienteCorreoOrderByIdDesc(correo) : ordenes.findAllByOrderByIdDesc())
            .stream().map(this::resumen).toList();
    }

    public Resumen detalle(Long id, String correo, boolean cliente) {
        var orden = buscar(id);
        if (cliente && !orden.cliente.getCorreo().equals(correo)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return resumen(orden);
    }

    @Transactional
    public Resumen actualizar(Long id, String estado) {
        if (!List.of("Pendiente", "En preparación", "Enviado", "Entregado").contains(estado))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido");
        var orden = buscar(id);
        orden.estado = estado;
        return resumen(orden);
    }

    @Transactional
    public Resumen resolverPago(Long id, boolean aprobar) {
        var orden = ordenes.bloquearPorId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"Por confirmar".equals(orden.estadoPago))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pago no está pendiente");
        if (!aprobar) {
            orden.items.stream().sorted(Comparator.comparing(Orden.Detalle::productoId)).forEach(item ->
                productos.bloquearPorId(item.productoId()).ifPresent(p -> p.setStock(Math.addExact(p.getStock(), item.cantidad()))));
        }
        orden.estadoPago = aprobar ? "Confirmado" : "Rechazado";
        return resumen(orden);
    }

    private Orden buscar(Long id) {
        return ordenes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Resumen resumen(Orden o) {
        return new Resumen(o.id, o.cliente.getCorreo(), o.fecha, o.tipoEntrega, o.direccion, o.estado, o.formaPago, o.estadoPago, o.datosTransferencia, o.total, List.copyOf(o.items));
    }
}
