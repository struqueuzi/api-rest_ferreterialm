"""Pruebas HTTP del perfil de demostración. Requiere Python 3 y el backend activo."""
import http.cookiejar
import json
import os
import urllib.error
import urllib.request

BASE = os.environ.get("API_URL", "http://127.0.0.1:8080")
PRODUCTO = {"nombre": "Taladro de verificación", "precio": 29990, "stock": 9, "categoria": "General"}


def cliente():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


def verificar(opener, metodo, ruta, esperado, datos=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + ruta, method=metodo, headers=headers,
                                 data=json.dumps(datos).encode() if datos is not None else None)
    try:
        respuesta = opener.open(req, timeout=15)
    except urllib.error.HTTPError as error:
        respuesta = error
    cuerpo = respuesta.read()
    assert respuesta.status == esperado, (metodo, ruta, esperado, respuesta.status, cuerpo)
    print(f"{metodo:6} {ruta:35} {respuesta.status}")
    return json.loads(cuerpo) if cuerpo and "json" in respuesta.headers.get("Content-Type", "") else None


def login(opener, rol):
    return verificar(opener, "POST", "/api/v1/auth/login", 200,
                     {"correo": rol + "@maestros.cl", "clave": rol + "123"})["token"]


def main():
    anon, admin, vendedor, comprador = (cliente() for _ in range(4))
    productos, ordenes = "/api/v1/productos", "/api/v1/ordenes"
    assert productos in verificar(anon, "GET", "/v3/api-docs", 200)["paths"]
    verificar(anon, "GET", "/swagger-ui/index.html", 200)
    verificar(anon, "GET", productos, 200)
    verificar(anon, "POST", productos, 401, PRODUCTO)
    verificar(anon, "POST", "/api/v1/auth/login", 401, {"correo": "admin@maestros.cl", "clave": "error"})
    a, v, c = login(admin, "admin"), login(vendedor, "vendedor"), login(comprador, "cliente")
    verificar(admin, "POST", productos, 400, {**PRODUCTO, "precio": -1}, a)
    p = verificar(admin, "POST", productos, 201, PRODUCTO, a)
    ruta = f"{productos}/{p['id']}"
    verificar(vendedor, "PUT", ruta, 403, PRODUCTO, v)
    assert verificar(admin, "PUT", ruta, 200, {**PRODUCTO, "stock": 8, "descuento": 10}, a)["stock"] == 8
    orden = {"items": [{"productoId": p["id"], "cantidad": 2}], "tipoEntrega": "retiro", "formaPago": "normal"}
    verificar(comprador, "POST", ordenes, 409, {**orden, "items": [{"productoId": p["id"], "cantidad": 9}]}, c)
    assert verificar(anon, "GET", ruta, 200)["stock"] == 8
    o = verificar(comprador, "POST", ordenes, 201, orden, c)
    assert o["total"] == 53980
    assert verificar(anon, "GET", ruta, 200)["stock"] == 6
    assert any(fila["id"] == o["id"] for fila in verificar(vendedor, "GET", ordenes, 200, token=v))
    detalle = f"{ordenes}/{o['id']}"
    assert verificar(vendedor, "GET", detalle, 200, token=v)["total"] == o["total"]
    verificar(vendedor, "PATCH", detalle, 403, {"estado": "Enviado"}, v)
    verificar(admin, "PATCH", detalle, 200, {"estado": "Enviado"}, a)
    assert verificar(comprador, "GET", detalle, 200, token=c)["estado"] == "Enviado"
    perfil = "/api/v1/auth/perfil"
    abonos = "/api/v1/abonos"
    saldo = verificar(comprador, "GET", perfil, 200, token=c)["saldoPendiente"]
    abono = verificar(comprador, "POST", abonos, 201, {"monto": 1000, "medio": "efectivo", "detalle": "Prueba HTTP"}, c)
    verificar(vendedor, "GET", abonos, 403, token=v)
    verificar(admin, "PATCH", f"{abonos}/{abono['id']}", 200, {"aprobar": True}, a)
    assert verificar(comprador, "GET", perfil, 200, token=c)["saldoPendiente"] == saldo - 1000
    assert verificar(comprador, "GET", abonos, 200, token=c)[-1]["estado"] == "Aprobado"
    verificar(vendedor, "GET", "/api/v1/usuarios", 403, token=v)
    assert any(fila["rol"] == "cliente" for fila in verificar(admin, "GET", "/api/v1/usuarios", 200, token=a))
    cuenta = verificar(anon, "GET", "/api/v1/transferencia", 200)
    assert verificar(admin, "PUT", "/api/v1/transferencia", 200, cuenta, a) == cuenta
    transferencia = verificar(comprador, "POST", ordenes, 201, {**orden, "formaPago": "transferencia"}, c)
    assert transferencia["datosTransferencia"] == cuenta
    assert verificar(admin, "PATCH", f"{ordenes}/{transferencia['id']}/pago", 200, {"aprobar": False}, a)["estadoPago"] == "Rechazado"
    assert verificar(anon, "GET", ruta, 200)["stock"] == 6
    verificar(admin, "PATCH", f"{ordenes}/{transferencia['id']}/pago", 409, {"aprobar": False}, a)
    verificar(comprador, "POST", "/api/v1/auth/refresh", 200)
    verificar(comprador, "POST", "/api/v1/auth/logout", 204)
    verificar(comprador, "POST", "/api/v1/auth/refresh", 401)
    verificar(admin, "DELETE", ruta, 204, token=a)
    verificar(anon, "GET", ruta, 404)
    assert verificar(vendedor, "GET", detalle, 200, token=v)["items"][0]["productoNombre"] == PRODUCTO["nombre"]
    print("Pruebas HTTP completadas")


if __name__ == "__main__":
    main()
