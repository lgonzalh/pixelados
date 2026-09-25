"""Mide el tamaño real (en dp) de cada icono de la barra superior en la captura.

Uso: python measure_topbar.py [captura.png]
"""
import sys
from PIL import Image

SHOT = sys.argv[1] if len(sys.argv) > 1 else r"C:\GitHub\pixelados\build\screenshots\180_iconos_escala.png"
DENSITY = 306 / 160  # wm density del moto g23

im = Image.open(SHOT).convert("RGB")
px = im.load()

# ventanas (x0, x1) de cada icono en la barra superior, y rango vertical
ventanas = {
    "atrás": (30, 90),
    "ayuda ?": (275, 330),
    "deshacer": (360, 420),
    "rehacer": (440, 500),
    "guardar": (540, 615),
    "exportar": (625, 700),
}

print(f"{'icono':10} {'ancho':>8} {'alto':>8}   (en dp)")
for nombre, (x0, x1) in ventanas.items():
    minx, miny, maxx, maxy = 10 ** 6, 10 ** 6, -1, -1
    for y in range(105, 200):
        for x in range(x0, x1):
            r, g, b = px[x, y]
            if r > 190 and g > 190 and b > 190:  # trazo claro sobre barra oscura
                minx, maxx = min(minx, x), max(maxx, x)
                miny, maxy = min(miny, y), max(maxy, y)
    if maxx < 0:
        print(f"{nombre:10}  (sin tinta)")
        continue
    w = (maxx - minx + 1) / DENSITY
    h = (maxy - miny + 1) / DENSITY
    print(f"{nombre:10} {w:7.1f} {h:7.1f}    {w:4.1f} x {h:4.1f} dp")
