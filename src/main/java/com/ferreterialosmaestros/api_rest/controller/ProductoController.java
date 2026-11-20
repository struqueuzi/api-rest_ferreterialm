package com.ferreterialosmaestros.api_rest.controller;

import com.ferreterialosmaestros.api_rest.model.Producto;
import com.ferreterialosmaestros.api_rest.service.ProductoService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Producto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Producto buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "jwt")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto guardar(@Valid @RequestBody Producto producto) {
        return service.guardar(producto);
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "jwt")
    @PutMapping("/{id}")
    public Producto actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Producto producto) {
        return service.actualizar(id, producto);
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "jwt")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}