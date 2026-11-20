package com.ferreterialosmaestros.api_rest.repository;

import com.ferreterialosmaestros.api_rest.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {}
