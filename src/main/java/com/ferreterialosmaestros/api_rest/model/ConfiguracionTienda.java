package com.ferreterialosmaestros.api_rest.model;

import jakarta.persistence.*;

@Entity
public class ConfiguracionTienda {
    @Id public Long id;
    @Embedded public CuentaBancaria cuenta;

    public ConfiguracionTienda() {}
    public ConfiguracionTienda(CuentaBancaria cuenta) {
        id = 1L;
        this.cuenta = cuenta;
    }
}
