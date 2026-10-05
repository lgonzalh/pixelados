"""Compara la zona del lienzo entre tres capturas para verificar que un trazo
se dibuja y que deshacer lo quita (cuenta de pixeles no blancos)."""
import sys
from PIL import Image, ImageChops

RUTAS = sys.argv[1:]
CAJA = (20, 340, 700, 1040)  # zona del lienzo en el moto g23


def no_blancos(ruta):
    im = Image.open(ruta).convert("RGB").crop(CAJA)
    px = im.load()
    n = 0
    for y in range(0, im.height, 2):
        for x in range(0, im.width, 2):
            r, g, b = px[x, y]
            if not (r > 235 and g > 235 and b > 235):
                n += 1
    return n, im


datos = [(r, *no_blancos(r)) for r in RUTAS]
for ruta, n, _ in datos:
    print(f"{ruta.split(chr(92))[-1]:35} pixeles con color: {n}")

if len(datos) >= 2:
    for i in range(len(datos) - 1):
        dif = ImageChops.difference(datos[i][2], datos[i + 1][2])
        print("diferencia entre", RUTAS[i].split(chr(92))[-1], "y", RUTAS[i + 1].split(chr(92))[-1], "=", dif.getbbox() is not None)
