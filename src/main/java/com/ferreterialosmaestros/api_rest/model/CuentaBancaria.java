package com.ferreterialosmaestros.api_rest.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;

@Embeddable
public record CuentaBancaria(
    @NotBlank @Size(max=100) String banco,
    @NotNull @Pattern(regexp="Cuenta Corriente|Cuenta Vista|Cuenta RUT|Cuenta de Ahorro") String tipoCuenta,
    @NotNull @Pattern(regexp="[0-9]{6,20}") String numeroCuenta,
    @NotBlank @Size(min=3,max=120) String titular,
    @NotBlank @Size(max=20) String rut,
    @NotBlank @Email @Size(max=150) String correo
) {}
