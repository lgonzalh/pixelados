"""Prepara las capturas del README: las recorta a ancho de README y las guarda
en docs/img/ para que el repositorio no cargue imagenes de telefono completas."""
import os
from PIL import Image

ORIGEN = r"C:\GitHub\pixelados\build\screenshots"
DESTINO = r"C:\GitHub\pixelados\docs\img"

CAPTURAS = [
    ("01_splash.png", "01-splash.png"),
    ("214_home_restaurado.png", "02-inicio.png"),
    ("185_topbar_ok.png", "03-editor.png"),
    ("151_simbolos_categorias.png", "04-simbolos.png"),
    ("154_pokemon_original.png", "05-pokemon.png"),
    ("62_ajustes.png", "06-ajustes.png"),
    ("94_ayuda.png", "07-ayuda.png"),
    ("25_exportar.png", "08-exportar.png"),
]

ANCHO = 420  # px de ancho final (las capturas originales son 720x1600)

os.makedirs(DESTINO, exist_ok=True)
for origen, salida in CAPTURAS:
    ruta = os.path.join(ORIGEN, origen)
    if not os.path.exists(ruta):
        print("falta:", origen)
        continue
    im = Image.open(ruta).convert("RGB")
    alto = round(im.height * ANCHO / im.width)
    im = im.resize((ANCHO, alto), Image.LANCZOS)
    destino = os.path.join(DESTINO, salida)
    im.save(destino, "PNG", optimize=True)
    print(f"{salida:20} {im.size} {round(os.path.getsize(destino)/1024)} KB")

total = sum(os.path.getsize(os.path.join(DESTINO, f)) for f in os.listdir(DESTINO))
print("total docs/img:", round(total / 1024), "KB")
