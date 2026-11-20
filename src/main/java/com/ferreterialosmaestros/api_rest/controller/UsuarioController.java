package com.ferreterialosmaestros.api_rest.controller;

import com.ferreterialosmaestros.api_rest.model.Usuario;
import com.ferreterialosmaestros.api_rest.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/usuarios")
@SecurityRequirement(name="jwt")
public class UsuarioController {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    public record UsuarioVista(Long id, String nombre, String correo, String rut, String telefono, String rol, boolean activo, boolean cuentaCorriente, long cupoCuentaCorriente, long saldoPendiente, List<Usuario.Movimiento> movimientosCC) {}
    public record Datos(@NotBlank @Size(max=120) String nombre, @NotBlank @Email @Size(max=180) String correo,
        @Size(max=20) String rut, @Size(max=20) String telefono, @Size(max=72) String clave,
        @NotNull @Pattern(regexp="admin|vendedor|cliente") String rol, boolean activo, boolean cuentaCorriente, @PositiveOrZero long cupoCuentaCorriente, @PositiveOrZero long saldoPendiente) {}
    public record Estado(@NotNull Boolean activo) {}

    public UsuarioController(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @GetMapping @Transactional(readOnly=true)
    public List<UsuarioVista> listar() { return usuarios.findAll().stream().map(this::vista).toList(); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public UsuarioVista crear(@Valid @RequestBody Datos d) { return guardar(new Usuario(), d); }

    @PutMapping("/{id}") @Transactional
    public UsuarioVista actualizar(@PathVariable Long id, @Valid @RequestBody Datos d, JwtAuthenticationToken jwt) {
        var u = buscar(id);
        if (u.correo.equals(jwt.getName()) && (!d.activo() || !"admin".equals(d.rol()) ||
                !u.correo.equals(d.correo().trim().toLowerCase(Locale.ROOT))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conserva el correo, rol y estado de tu cuenta activa");
        return guardar(u, d);
    }

    @PatchMapping("/{id}") @Transactional
    public UsuarioVista estado(@PathVariable Long id, @Valid @RequestBody Estado d, JwtAuthenticationToken jwt) {
        var u = buscar(id);
        if (!d.activo() && u.correo.equals(jwt.getName()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tu cuenta debe mantenerse activa");
        u.activo = d.activo();
        return vista(usuarios.save(u));
    }

    private UsuarioVista guardar(Usuario u, Datos d) {
        var clave = d.clave() == null ? "" : d.clave();
        if ((u.id == null || !clave.isEmpty()) && clave.length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave requiere al menos seis caracteres");
        u.nombre = d.nombre().trim();
        u.correo = d.correo().trim().toLowerCase(Locale.ROOT);
        u.rut = d.rut() == null || d.rut().isBlank() ? null : d.rut().replace(".", "").toUpperCase(Locale.ROOT);
        u.telefono = d.telefono() == null || d.telefono().isBlank() ? null : d.telefono().replaceAll("\\s", "").replaceFirst("^\\+56", "");
        u.rol = Usuario.Rol.valueOf(d.rol().toUpperCase(Locale.ROOT));
        u.activo = d.activo();
        if ((!d.cuentaCorriente() || u.rol != Usuario.Rol.CLIENTE) && u.saldoPendiente > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Liquida el saldo antes de deshabilitar la cuenta corriente");
        long saldoAnterior = u.saldoPendiente;
        u.cuentaCorriente = d.cuentaCorriente() && u.rol == Usuario.Rol.CLIENTE;
        u.cupoCuentaCorriente = u.cuentaCorriente ? d.cupoCuentaCorriente() : 0;
        u.saldoPendiente = u.cuentaCorriente ? d.saldoPendiente() : 0;
        if (u.saldoPendiente > u.cupoCuentaCorriente)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El saldo supera el cupo asignado");
        long ajuste = u.saldoPendiente - saldoAnterior;
        if (ajuste != 0) u.movimientosCC.add(new Usuario.Movimiento(java.time.Instant.now(), "ajuste", ajuste, "Ajuste de administración", null));
        if (!clave.isEmpty()) u.hash = encoder.encode(clave);
        try { return vista(usuarios.saveAndFlush(u)); }
        catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Correo, RUT o teléfono repetido");
        }
    }
    private Usuario buscar(Long id) {
        return usuarios.bloquearPorId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private UsuarioVista vista(Usuario u) {
        return new UsuarioVista(u.id, u.nombre, u.correo, u.rut, u.telefono, u.rol.name().toLowerCase(Locale.ROOT), u.activo, u.cuentaCorriente, u.cupoCuentaCorriente, u.saldoPendiente, List.copyOf(u.movimientosCC));
    }
}
