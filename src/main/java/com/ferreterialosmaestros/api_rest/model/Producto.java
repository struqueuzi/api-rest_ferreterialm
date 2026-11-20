package com.ferreterialosmaestros.api_rest.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;

@Entity
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(min = 3, max = 200)
    private String nombre;
    @NotNull @Min(1) @Max(100000000)
    private Integer precio;

    public Producto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getPrecio() {
        return precio;
    }

    public void setPrecio(Integer precio) {
        this.precio = precio;
    }

    @NotNull @Min(0)
    private Integer stock = 0;

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    @Size(max = 255)
    private String imagen = "";

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    @NotBlank @Size(max = 120)
    private String categoria = "General";

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "categoria", insertable = false, updatable = false)
    private Categoria categoriaRelacion;

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    @Size(max = 120)
    private String subcategoria = "";

    public String getSubcategoria() { return subcategoria; }
    public void setSubcategoria(String subcategoria) { this.subcategoria = subcategoria; }

    @NotNull @Min(0) @Max(90)
    private Integer descuento = 0;

    public Integer getDescuento() { return descuento; }
    public void setDescuento(Integer descuento) { this.descuento = descuento; }


    private boolean esNuevo = false;

    public boolean getEsNuevo() { return esNuevo; }
    public void setEsNuevo(boolean esNuevo) { this.esNuevo = esNuevo; }
}
