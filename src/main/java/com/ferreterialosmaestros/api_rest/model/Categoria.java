package com.ferreterialosmaestros.api_rest.model;

import jakarta.persistence.*;

@Entity
public class Categoria {
    @Id @Column(length = 120)
    private String nombre;

    protected Categoria() {}
    public Categoria(String nombre) { this.nombre = nombre; }
}
