"""Mide la tinta real de los iconos de la fila de herramientas (tamaño 26 dp
declarado en el código) para entender cómo escala el dibujable."""
from PIL import Image

SHOT = r"C:\GitHub\pixelados\build\screenshots\180_iconos_escala.png"
DENSITY = 306 / 160

im = Image.open(SHOT).convert("RGB")
px = im.load()
ancho = im.size[0]

# fila de herramientas: y aprox 1280..1360; 6 iconos repartidos
centros = [64, 184, 304, 424, 544, 664]
nombres = ["Pincel", "Borrador", "Rellenar", "Gotero", "Selección", "Símbolos"]

print(f"{'icono':10} {'ancho':>7} {'alto':>7}   (dp)")
for nombre, cx in zip(nombres, centros):
    minx, miny, maxx, maxy = 10 ** 6, 10 ** 6, -1, -1
    for y in range(1285, 1355):
        for x in range(max(0, cx - 45), min(ancho, cx + 45)):
            r, g, b = px[x, y]
            # pincel seleccionado = icono blanco; otros = trazo oscuro sobre blanco
            claro = r > 190 and g > 190 and b > 190
            oscuro = r < 90 and g < 90 and b < 90
            if claro or oscuro:
                minx, maxx = min(minx, x), max(maxx, x)
                miny, maxy = min(miny, y), max(maxy, y)
    if maxx < 0:
        print(f"{nombre:10}  (sin tinta)")
        continue
    w = (maxx - minx + 1) / DENSITY
    h = (maxy - miny + 1) / DENSITY
    print(f"{nombre:10} {w:6.1f} {h:6.1f}    {w:4.1f} x {h:4.1f} dp")
