package com.ferreterialosmaestros.api_rest;

import java.nio.charset.StandardCharsets;
import com.ferreterialosmaestros.api_rest.repository.UsuarioRepository;
import org.springframework.security.oauth2.core.*;
import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@SecurityScheme(name = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class SecurityConfig {
    @Bean SecretKey signingKey(@Value("${app.jwt-secret:}") String secret) throws Exception {
        if (secret.isBlank()) return KeyGenerator.getInstance("HmacSHA256").generateKey();
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT_SECRET requiere al menos 32 bytes");
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
    @Bean JwtEncoder encoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder decoder(SecretKey key, UsuarioRepository usuarios) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("ferreteria"),
            token -> usuarios.findByCorreo(token.getSubject())
                .filter(u -> u.activo && u.rol.name().equals(token.getClaimAsString("rol")))
                .map(u -> OAuth2TokenValidatorResult.success())
                .orElseGet(() -> OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token")))));
        return decoder;
    }
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/productos", "/api/v1/productos/*", "/api/v1/transferencia").permitAll()
                .requestMatchers("/api/v1/productos", "/api/v1/productos/*", "/api/v1/transferencia", "/api/v1/usuarios", "/api/v1/usuarios/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/perfil").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/abonos").hasAnyRole("ADMIN", "CLIENTE")
                .requestMatchers(HttpMethod.POST, "/api/v1/ordenes", "/api/v1/abonos").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/abonos/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/ordenes", "/api/v1/ordenes/*").hasAnyRole("ADMIN","VENDEDOR","CLIENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/ordenes/*", "/api/v1/ordenes/*/pago").hasRole("ADMIN")
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))).build();
    }
}
