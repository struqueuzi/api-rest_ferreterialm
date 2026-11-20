package com.ferreterialosmaestros.api_rest.controller;

import com.ferreterialosmaestros.api_rest.model.*;
import com.ferreterialosmaestros.api_rest.repository.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/abonos")
@SecurityRequirement(name="jwt")
public class AbonoController {
    private final AbonoRepository abonos;
    private final UsuarioRepository usuarios;
    public record Datos(@Positive long monto, @Size(max=250) String detalle,
        @NotNull @Pattern(regexp="efectivo|debito|credito|transferencia") String medio) {}
    public record Resolucion(@NotNull Boolean aprobar) {}
    public record Vista(Long id, Long idUsuario, long monto, String detalle, String medio, String estado, Instant fechaISO) {}

    public AbonoController(AbonoRepository abonos, UsuarioRepository usuarios) {
        this.abonos = abonos;
        this.usuarios = usuarios;
    }

    @GetMapping @Transactional(readOnly=true)
    public List<Vista> listar(JwtAuthenticationToken jwt) {
        var u = usuarios.findByCorreo(jwt.getName()).orElseThrow();
        return (u.rol == Usuario.Rol.ADMIN ? abonos.findAllByOrderById() : abonos.findByUsuarioIdOrderById(u.id))
            .stream().map(this::vista).toList();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public Vista solicitar(@Valid @RequestBody Datos d, JwtAuthenticationToken jwt) {
        var u = usuarios.bloquearPorCorreo(jwt.getName()).orElseThrow();
        long pendientes = abonos.findByUsuarioIdOrderById(u.id).stream()
            .filter(a -> "Pendiente".equals(a.estado)).mapToLong(a -> a.monto).sum();
        if (!u.cuentaCorriente || d.monto() > u.saldoPendiente - pendientes)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El abono excede el saldo pendiente que puedes abonar");
        var a = new Abono();
        a.usuario = u;
        a.monto = d.monto();
        a.medio = d.medio();
        a.detalle = d.detalle();
        return vista(abonos.save(a));
    }

    @PatchMapping("/{id}") @Transactional
    public Vista resolver(@PathVariable Long id, @Valid @RequestBody Resolucion resolucion) {
        var abono = abonos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Long usuarioId = abono.getUsuario().getId();
        var u = org.hibernate.Hibernate.unproxy(usuarios.bloquearPorId(usuarioId).orElseThrow(), Usuario.class);
        entityManager.refresh(abono, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!"Pendiente".equals(abono.estado))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El abono ya fue revisado");
        if (resolucion.aprobar()) {
            if (abono.monto > u.saldoPendiente)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El abono supera el saldo actual");
            u.saldoPendiente -= abono.monto;
            u.movimientosCC.add(new Usuario.Movimiento(Instant.now(), "abono", -abono.monto, abono.detalle, abono.medio));
        }
        abono.estado = resolucion.aprobar() ? "Aprobado" : "Rechazado";
        return vista(abono);
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private Vista vista(Abono a) {
        return new Vista(a.id, a.getUsuario().getId(), a.monto, a.detalle, a.medio, a.estado, a.fecha);
    }
}
