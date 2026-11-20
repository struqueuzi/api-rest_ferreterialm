package com.ferreterialosmaestros.api_rest.model;

import jakarta.persistence.*;

@Entity
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true)
    public String correo;
    public String nombre;
    @Column(unique = true)
    public String rut;
    @Column(unique = true)
    public String telefono;
    public String hash;
    public boolean activo = true;
    public boolean cuentaCorriente;
    public long saldoPendiente;
    public long cupoCuentaCorriente;
    @ElementCollection
    @CollectionTable(name="movimiento_cuenta", joinColumns=@JoinColumn(name="usuario_id"))
    @OrderColumn(name="posicion")
    public java.util.List<Movimiento> movimientosCC = new java.util.ArrayList<>();

    @Embeddable
    public record Movimiento(java.time.Instant fechaISO, String tipo, long monto, String detalle, String medio) {}

    @Enumerated(EnumType.STRING)
    public Rol rol;

    public String getCorreo() { return correo; }
    public Long getId() { return id; }

    public enum Rol { ADMIN, VENDEDOR, CLIENTE }
}
