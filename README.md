# API de Ferretería Los Maestros

Desarrollo Fullstack II · Evaluación Parcial 3. Backend Spring Boot, Java 17 o superior, Spring Data JPA, Spring Security y OpenAPI. El código se verificó con JDK 21.

## Demostración con MySQL 8.4

1. Instala un JDK, Docker y Compose. Desde esta carpeta define una contraseña local para la base.
2. Inicia la base y espera a que responda. El volumen conserva los datos al reiniciar.

```bash
export DB_PASSWORD='tu-clave-local'
docker compose up -d db
docker compose logs -f db
```

3. Cuando MySQL indique que está listo, inicia la API en otra terminal. Define la misma variable `DB_PASSWORD` en esa terminal.

```bash
export DB_PASSWORD='tu-clave-local'
export DB_URL='jdbc:mysql://localhost:3306/ferreteria'
export DB_USER='ferreteria'
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo-mysql
```

## Demostración inmediata con H2 persistente

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

El archivo de datos se crea en `data/`. Ambos perfiles de demostración cargan el catálogo inicial si está vacío y crean estas cuentas de prueba.

| Rol | Correo | Clave demo |
|---|---|---|
| Administrador | admin@maestros.cl | admin123 |
| Vendedor | vendedor@maestros.cl | vendedor123 |
| Cliente | cliente@maestros.cl | cliente123 |

## Uso y verificación

- Swagger UI: <http://127.0.0.1:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://127.0.0.1:8080/v3/api-docs>
- Catálogo: <http://127.0.0.1:8080/api/v1/productos>
- Para rutas protegidas, ejecuta el login en Swagger, copia el campo `token` y pégalo en **Authorize**.
- Con el backend activo, ejecuta `python3 scripts/verificar_api.py` para probar CRUD, autorización, stock, órdenes y renovación. La prueba crea una orden de ejemplo y elimina el producto temporal.
- Ejecuta `./mvnw test` para la prueba de arranque.

El perfil normal configura MySQL con `DB_URL`, `DB_USER` y `DB_PASSWORD`. Para HTTPS configura `JWT_SECRET` con un valor aleatorio de al menos 32 bytes y conserva la cookie segura. Los perfiles de demostración usan cookies HTTP locales. El token de acceso vence en cinco minutos y la renovación se guarda en memoria del servidor. Reiniciar la API requiere iniciar sesión otra vez.

## Alcance del modelo

`Usuario` tiene muchas `Orden` (tabla `boleta`). Cada orden guarda sus líneas en `detalle_boleta`, con referencia histórica por ID de producto, nombre, cantidad y precio unitario al comprar. `Producto` referencia a `Categoria`. La orden se registra y el stock se descuenta en la misma transacción. El listado de cliente se limita al usuario identificado por el JWT.
