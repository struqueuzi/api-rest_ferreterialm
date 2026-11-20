package com.ferreterialosmaestros.api_rest.repository;

import com.ferreterialosmaestros.api_rest.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository
        extends JpaRepository<Producto, Long> {
}
