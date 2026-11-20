package com.ferreterialosmaestros.api_rest.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Abono {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Usuario usuario;
    public long monto;
    public String detalle;
    public String medio;
    public String estado = "Pendiente";
    public Instant fecha = Instant.now();

    public Usuario getUsuario() { return usuario; }
}
