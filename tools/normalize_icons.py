"""Normaliza los iconos propios para que pesen visualmente igual que los de
Material: recorta al contenido y lo centra ocupando el 84% del lienzo.

Los originales en assets/ no se tocan; se reescriben solo las copias que usa la
app (res/drawable-nodpi).
"""
import os
from PIL import Image

BASE = r"C:\GitHub\pixelados\app\src\main\res\drawable-nodpi"
SIZE = 512
# Proporción del lienzo que ocupa la figura. Los iconos de Material ocupan
# entre el 67% (flecha atrás) y el 75% (deshacer) de su caja de 24 dp, así que
# se iguala a ~76% para que pesen visualmente lo mismo.
FILL = 0.76

for name in sorted(os.listdir(BASE)):
    if not (name.startswith("ic_") and name.endswith(".png")):
        continue
    path = os.path.join(BASE, name)
    im = Image.open(path).convert("RGBA")
    bbox = im.split()[3].getbbox()
    if not bbox:
        continue
    glyph = im.crop(bbox)

    # escala para que el lado mayor de la figura mida FILL * SIZE
    target = int(SIZE * FILL)
    scale = target / max(glyph.size)
    new_size = (max(1, round(glyph.width * scale)), max(1, round(glyph.height * scale)))
    glyph = glyph.resize(new_size, Image.LANCZOS)

    canvas = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    canvas.paste(glyph, ((SIZE - new_size[0]) // 2, (SIZE - new_size[1]) // 2), glyph)
    canvas.save(path)
    print(f"{name:32} {im.size} -> figura {new_size}")

print("listo")
