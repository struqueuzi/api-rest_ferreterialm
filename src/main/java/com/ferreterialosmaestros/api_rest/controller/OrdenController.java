package com.ferreterialosmaestros.api_rest.controller;

import com.ferreterialosmaestros.api_rest.service.OrdenService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ordenes")
@SecurityRequirement(name="jwt")
public class OrdenController {
    private final OrdenService servicio;
    public OrdenController(OrdenService servicio) { this.servicio = servicio; }
    public record Pago(@jakarta.validation.constraints.NotNull Boolean aprobar) {}
    public record Estado(@NotBlank String estado) {}

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public OrdenService.Resumen crear(@Valid @RequestBody OrdenService.Nueva datos, JwtAuthenticationToken jwt) {
        return servicio.crear(jwt.getName(), datos);
    }
    @GetMapping
    public List<OrdenService.Resumen> listar(JwtAuthenticationToken jwt) {
        return servicio.listar(jwt.getName(), cliente(jwt));
    }
    @GetMapping("/{id}")
    public OrdenService.Resumen detalle(@PathVariable Long id, JwtAuthenticationToken jwt) {
        return servicio.detalle(id, jwt.getName(), cliente(jwt));
    }
    @PatchMapping("/{id}")
    public OrdenService.Resumen actualizar(@PathVariable Long id, @Valid @RequestBody Estado datos) {
        return servicio.actualizar(id, datos.estado());
    }
    @PatchMapping("/{id}/pago")
    public OrdenService.Resumen resolverPago(@PathVariable Long id, @Valid @RequestBody Pago datos) {
        return servicio.resolverPago(id, datos.aprobar());
    }
    private boolean cliente(JwtAuthenticationToken jwt) {
        return "CLIENTE".equals(jwt.getToken().getClaimAsString("rol"));
    }
}
