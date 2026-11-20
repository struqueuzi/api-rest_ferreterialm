package com.ferreterialosmaestros.api_rest.controller;

import com.ferreterialosmaestros.api_rest.model.Usuario;
import com.ferreterialosmaestros.api_rest.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwt;
    private final boolean secure;
    private final Map<String, Renovacion> sesiones = new ConcurrentHashMap<>();
    private record Renovacion(String correo, Instant vence) {}
    public record Credenciales(@NotBlank @Email String correo, @NotBlank @Size(max=72) String clave) {}
    public record Registro(@NotBlank @Size(max=120) String nombre, @NotBlank @Email @Size(max=180) String correo,
        @NotBlank @Size(min=6,max=72) String clave, @NotBlank @Size(max=20) String rut, @NotBlank @Size(max=20) String telefono) {}
    public record Perfil(Long id, String correo, String nombre, String rol, boolean cuentaCorriente,
        long cupoCuentaCorriente, long saldoPendiente, List<Usuario.Movimiento> movimientosCC) {}

    @GetMapping("/perfil")
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="jwt")
    public Perfil perfil(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken token) {
        return perfil(usuarios.findByCorreo(token.getName()).orElseThrow());
    }

    private Perfil perfil(Usuario u) {
        return new Perfil(u.id, u.correo, u.nombre, u.rol.name().toLowerCase(Locale.ROOT),
            u.cuentaCorriente, u.cupoCuentaCorriente, u.saldoPendiente, List.copyOf(u.movimientosCC));
    }
    public record Sesion(String token, Perfil usuario) {}

    public AuthController(UsuarioRepository usuarios, PasswordEncoder passwords, JwtEncoder jwt,
            @Value("${app.cookie-secure:true}") boolean secure) {
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.jwt = jwt;
        this.secure = secure;
    }

    @PostMapping("/registro") @ResponseStatus(HttpStatus.CREATED)
    public void registrar(@Valid @RequestBody Registro datos) {
        var u = new Usuario();
        u.nombre = datos.nombre().trim();
        u.correo = normalizar(datos.correo());
        u.rut = datos.rut().replace(".", "").toUpperCase(Locale.ROOT);
        u.telefono = datos.telefono().replaceAll("\\s", "").replaceFirst("^\\+56", "");
        u.hash = passwords.encode(datos.clave());
        u.rol = Usuario.Rol.CLIENTE;
        try { usuarios.saveAndFlush(u); }
        catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Correo, RUT o teléfono ya registrado");
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    @PostMapping("/login")
    public ResponseEntity<Sesion> login(@Valid @RequestBody Credenciales datos,
            @CookieValue(name="ferreteria_refresh", required=false) String anterior) {
        var u = usuarios.findByCorreo(normalizar(datos.correo()))
            .filter(usuario -> usuario.activo && passwords.matches(datos.clave(), usuario.hash))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (anterior != null) sesiones.remove(anterior);
        return emitir(u, Instant.now().plus(Duration.ofDays(1)));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    @PostMapping("/refresh")
    public ResponseEntity<Sesion> refresh(@CookieValue(name="ferreteria_refresh", required=false) String token) {
        var sesion = token == null ? null : sesiones.remove(token);
        if (sesion == null || !sesion.vence().isAfter(Instant.now())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return emitir(usuarios.findByCorreo(sesion.correo()).filter(u -> u.activo).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)), sesion.vence());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name="ferreteria_refresh", required=false) String token) {
        if (token != null) sesiones.remove(token);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
    }

    private ResponseEntity<Sesion> emitir(Usuario u, Instant limite) {
        Instant ahora = Instant.now();
        sesiones.entrySet().removeIf(entry -> !entry.getValue().vence().isAfter(ahora));
        var claims = JwtClaimsSet.builder().issuer("ferreteria").subject(u.correo).issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(300)).claim("rol", u.rol.name()).id(UUID.randomUUID().toString()).build();
        var acceso = jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
        String refresh = UUID.randomUUID() + "." + UUID.randomUUID();
        sesiones.put(refresh, new Renovacion(u.correo, limite));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header(HttpHeaders.SET_COOKIE, cookie(refresh, Duration.between(ahora, limite)))
            .body(new Sesion(acceso.getTokenValue(), perfil(u)));
    }

    private String cookie(String value, Duration edad) {
        return ResponseCookie.from("ferreteria_refresh", value).httpOnly(true).sameSite("Strict")
            .secure(secure).path("/api/v1/auth").maxAge(edad).build().toString();
    }
    private String normalizar(String correo) { return correo.trim().toLowerCase(Locale.ROOT); }
}
