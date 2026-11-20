package com.ferreterialosmaestros.api_rest.service;

import com.ferreterialosmaestros.api_rest.model.Producto;
import com.ferreterialosmaestros.api_rest.repository.ProductoRepository;

import com.ferreterialosmaestros.api_rest.model.Categoria;
import com.ferreterialosmaestros.api_rest.repository.CategoriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository repository;

    private final CategoriaRepository categorias;

    public ProductoService(ProductoRepository repository, CategoriaRepository categorias) {
        this.repository = repository;
        this.categorias = categorias;
    }

    // Listar todos los productos
    public List<Producto> listar() {
        return repository.findAll();
    }

    // Buscar un producto por ID
    public Producto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado"
                    )
                );
    }

    // Guardar un producto
    @org.springframework.transaction.annotation.Transactional
    public Producto guardar(Producto producto) {
        producto.setId(null);
        asegurarCategoria(producto.getCategoria());
        return repository.save(producto);
    }

    // Actualizar un producto
    @org.springframework.transaction.annotation.Transactional
    public Producto actualizar(Long id, Producto datos) {
        Producto producto = repository.bloquearPorId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        producto.setNombre(datos.getNombre());
        producto.setPrecio(datos.getPrecio());
        producto.setStock(datos.getStock());
        producto.setImagen(datos.getImagen());
        asegurarCategoria(datos.getCategoria());
        producto.setCategoria(datos.getCategoria());
        producto.setSubcategoria(datos.getSubcategoria());
        producto.setDescuento(datos.getDescuento());
        producto.setEsNuevo(datos.getEsNuevo());

        return repository.save(producto);
    }

    // Eliminar un producto
    @org.springframework.transaction.annotation.Transactional
    public void eliminar(Long id) {
        Producto producto = repository.bloquearPorId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        repository.delete(producto);
    }
    private void asegurarCategoria(String nombre) {
        categorias.save(new Categoria(nombre));
    }
}
