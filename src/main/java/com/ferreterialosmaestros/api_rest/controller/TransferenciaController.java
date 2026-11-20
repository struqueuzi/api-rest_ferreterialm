package com.ferreterialosmaestros.api_rest.controller;

import com.ferreterialosmaestros.api_rest.model.*;
import com.ferreterialosmaestros.api_rest.repository.ConfiguracionRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transferencia")
public class TransferenciaController {
    private final ConfiguracionRepository configuracion;
    public TransferenciaController(ConfiguracionRepository configuracion) { this.configuracion = configuracion; }

    @GetMapping
    public ResponseEntity<CuentaBancaria> obtener() {
        return configuracion.findById(1L).map(fila -> ResponseEntity.ok(fila.cuenta))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    @SecurityRequirement(name="jwt")
    public CuentaBancaria guardar(@Valid @RequestBody CuentaBancaria cuenta) {
        return configuracion.save(new ConfiguracionTienda(cuenta)).cuenta;
    }
}
