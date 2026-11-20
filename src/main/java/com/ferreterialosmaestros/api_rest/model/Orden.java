package com.ferreterialosmaestros.api_rest.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name="boleta")
public class Orden {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Usuario cliente;
    public Instant fecha = Instant.now();
    public String tipoEntrega;
    public String direccion;
    public String formaPago;
    public String estadoPago;
    @Embedded public CuentaBancaria datosTransferencia;
    public String estado = "Pendiente";
    public long total;
    @ElementCollection
    @CollectionTable(name="detalle_boleta", joinColumns=@JoinColumn(name="orden_id"))
    @OrderColumn(name="posicion")
    public List<Detalle> items = new ArrayList<>();

    @Embeddable
    public record Detalle(Long productoId, String productoNombre, int cantidad, long precioUnitario) {}
}
