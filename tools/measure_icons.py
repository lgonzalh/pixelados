"""Mide cuánto ocupa la figura dentro del lienzo de cada icono de assets."""
import os
from PIL import Image

BASE = r"C:\GitHub\pixelados\app\src\main\res\drawable-nodpi"

rows = []
for name in sorted(os.listdir(BASE)):
    if not (name.startswith("ic_") and name.endswith(".png")):
        continue
    im = Image.open(os.path.join(BASE, name)).convert("RGBA")
    bbox = im.split()[3].getbbox()
    if not bbox:
        continue
    w, h = im.size
    bw, bh = bbox[2] - bbox[0], bbox[3] - bbox[1]
    rows.append((name, w, h, bw, bh, max(bw, bh) / max(w, h)))

print("icono                            lienzo     figura   relleno")
for name, w, h, bw, bh, ratio in rows:
    print(f"{name:32} {w}x{h:<6} {bw}x{bh:<6} {ratio:6.2f}")

if rows:
    ratios = [r[5] for r in rows]
    print()
    print("relleno minimo %.2f  maximo %.2f  promedio %.2f" % (min(ratios), max(ratios), sum(ratios) / len(ratios)))
