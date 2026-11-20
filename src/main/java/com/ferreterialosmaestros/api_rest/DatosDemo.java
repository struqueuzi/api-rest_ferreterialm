package com.ferreterialosmaestros.api_rest;

import tools.jackson.databind.ObjectMapper;
import com.ferreterialosmaestros.api_rest.model.*;
import com.ferreterialosmaestros.api_rest.repository.*;
import com.ferreterialosmaestros.api_rest.service.ProductoService;
import java.util.Locale;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"demo", "demo-mysql"})
public class DatosDemo {
    @Bean ApplicationRunner cargarDemo(UsuarioRepository usuarios, ProductoRepository productos, ProductoService servicio, ConfiguracionRepository configuracion, PasswordEncoder passwords) {
        return args -> {
            for (var rol : Usuario.Rol.values()) {
                String nombre = rol.name().toLowerCase(Locale.ROOT);
                String correo = nombre + "@maestros.cl";
                if (usuarios.findByCorreo(correo).isPresent()) continue;
                var u = new Usuario();
                u.nombre = nombre;
                u.correo = correo;
                u.rol = rol;
                if (rol == Usuario.Rol.CLIENTE) {
                    u.cuentaCorriente = true;
                    u.cupoCuentaCorriente = 40000;
                    u.saldoPendiente = 15000;
                    u.movimientosCC.add(new Usuario.Movimiento(java.time.Instant.now(), "ajuste", 15000, "Saldo inicial demo", null));
                }
                u.hash = passwords.encode(nombre + "123");
                usuarios.save(u);
            }
            if (!configuracion.existsById(1L)) {
                configuracion.save(new ConfiguracionTienda(new CuentaBancaria("Banco de demostración", "Cuenta Corriente",
                    "0000000000", "Ferretería Los Maestros Demo", "76.418.293-6", "pagos@example.invalid")));
            }
            if (productos.count() == 0) {
                try (var entrada = new ClassPathResource("catalogo-demo.json").getInputStream()) {
                    for (var producto : new ObjectMapper().readValue(entrada, Producto[].class)) servicio.guardar(producto);
                }
            }
        };
    }
}
