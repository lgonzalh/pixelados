"""Convierte las referencias de assets/pokemon en sellos pixel-art.

- Separa la figura del fondo de rejilla (grises claros).
- Reduce a una cuadricula N x N por promedio de area.
- Guarda la mascara y el COLOR de cada celda (paleta reducida), para que el
  sello se pueda estampar relleno, con contorno o con sus colores originales.
Salida: Kotlin con la lista de sellos de la categoria "Pokémon".
"""
import glob
import os
import re
from PIL import Image

BASE = r"C:\GitHub\pixelados\assets\pokemon"
OUT = r"C:\GitHub\pixelados\app\src\main\java\com\pixelados\data\model\PokemonCatalog.kt"
PREVIEW = r"C:\GitHub\pixelados\build\pokemon_preview.png"
N = 21  # celdas por lado del sello

# nombre visible por palabra clave del archivo (se toman de las referencias)
NAMES = [
    ("pikachu", "Pikachu"),
    ("bulbizarre", "Bulbasaur"),
    ("charmander", "Charmander"),
    ("carapuce", "Squirtle"),
    ("abra", "Abra"),
    ("jigglypuff", "Jigglypuff"),
    ("ash", "Entrenador"),
    ("blastoise", "Blastoise"),
    ("dragonite", "Dragonite"),
    ("chikorita", "Chikorita"),
    ("evoli", "Eevee"),
    ("dracaufeu", "Charizard"),
    ("ponyta", "Ponyta"),
    ("gible", "Gible"),
    ("greninja", "Greninja"),
    ("lapras", "Lapras"),
    ("lucario", "Lucario"),
    ("meowth", "Meowth"),
    ("mewtwo", "Mewtwo"),
    ("mew", "Mew"),
    ("articuno", "Articuno"),
    ("gengar", "Gengar"),
    ("metamorph", "Ditto"),
    ("tiplouf", "Piplup"),
    ("sylveon", "Sylveon"),
    ("onix", "Onix"),
    ("magicarpe", "Magikarp"),
    ("psyduck", "Psyduck"),
    ("snorlax", "Snorlax"),
    ("umbreon", "Umbreon"),
    ("vulpix", "Vulpix"),
    ("rowlet", "Rowlet"),
    ("scyther", "Scyther"),
    ("serpent-legendaire", "Dragón legendario"),
    ("pokeball", "Pokébola"),
    ("hyper-ball", "Ultrabola"),
    ("pokedex", "Pokédex"),
]


def name_for(path):
    low = os.path.basename(path).lower()
    for key, label in NAMES:
        if key in low:
            return label
    return os.path.splitext(os.path.basename(path))[0][:18]


def quantize(color, palette):
    """Devuelve el color de la paleta mas cercano."""
    best, best_d = palette[0], 1e9
    for p in palette:
        d = sum((a - b) ** 2 for a, b in zip(color, p))
        if d < best_d:
            best, best_d = p, d
    return best


def sample(path, n=N):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    px = im.load()

    # 1. mascara de figura: todo lo que no sea gris claro / blanco (rejilla)
    mask = Image.new("L", (w, h), 0)
    mpx = mask.load()
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            lo, hi = min(r, g, b), max(r, g, b)
            is_bg = (hi - lo) < 26 and hi > 170
            mpx[x, y] = 0 if is_bg else 255

    # 2. rejilla n x n por promedio de area
    small = mask.resize((n, n), Image.BOX)
    spx = small.load()

    # 3. color dominante de cada celda (solo pixeles de figura)
    cells = [[None] * n for _ in range(n)]
    buckets = [[[] for _ in range(n)] for _ in range(n)]
    for y in range(h):
        for x in range(w):
            if mpx[x, y]:
                cx, cy = min(n - 1, x * n // w), min(n - 1, y * n // h)
                buckets[cy][cx].append(px[x, y])

    colors = []
    for cy in range(n):
        for cx in range(n):
            if spx[cx, cy] >= 128 and buckets[cy][cx]:
                vals = buckets[cy][cx]
                avg = tuple(sum(v[i] for v in vals) // len(vals) for i in range(3))
                colors.append(avg)

    return small, colors


def build_palette(all_colors, k=14):
    """Paleta reducida simple (k-means ligero) para que los sellos no traigan ruido."""
    if not all_colors:
        return [(0, 0, 0)]
    # inicializacion uniforme sobre el rango de color
    all_colors = sorted(all_colors, key=lambda c: (sum(c), c))
    step = max(1, len(all_colors) // k)
    centers = [all_colors[min(len(all_colors) - 1, i * step)] for i in range(k)]
    for _ in range(6):
        groups = [[] for _ in centers]
        for c in all_colors:
            idx = min(range(len(centers)), key=lambda i: sum((a - b) ** 2 for a, b in zip(c, centers[i])))
            groups[idx].append(c)
        new = []
        for i, g in enumerate(groups):
            if g:
                new.append(tuple(sum(v[j] for v in g) // len(g) for j in range(3)))
            else:
                new.append(centers[i])
        if new == centers:
            break
        centers = new
    return centers


def main():
    files = sorted(glob.glob(os.path.join(BASE, "*.webp")))
    files = [f for f in files if "(1)" not in f]  # duplicados
    masks = {}
    all_colors = []
    for f in files:
        mask, colors = sample(f)
        masks[f] = (mask, colors)
        all_colors.extend(colors)

    palette = build_palette(all_colors, 14)

    entries = []
    usados = {}
    for f in files:
        mask, _colors = masks[f]
        im = Image.open(f).convert("RGB")
        w, h = im.size
        px = im.load()
        # color por celda
        buckets = [[[] for _ in range(N)] for _ in range(N)]
        for y in range(h):
            for x in range(w):
                r, g, b = px[x, y]
                lo, hi = min(r, g, b), max(r, g, b)
                if (hi - lo) < 26 and hi > 170:
                    continue
                cx, cy = min(N - 1, x * N // w), min(N - 1, y * N // h)
                buckets[cy][cx].append((r, g, b))
        small_px = mask.load()
        rows = []
        for cy in range(N):
            row = []
            for cx in range(N):
                if small_px[cx, cy] >= 128 and buckets[cy][cx]:
                    vals = buckets[cy][cx]
                    avg = tuple(sum(v[i] for v in vals) // len(vals) for i in range(3))
                    row.append(quantize(avg, palette))
                else:
                    row.append(None)
            rows.append(row)
        nombre = name_for(f)
        # Los nombres no pueden repetirse (la galería los usa como clave):
        # si se repite, se distingue por lo que aporta su archivo.
        if nombre in usados:
            usados[nombre] += 1
            extra = "entrenador" if "dresseur" in f.lower() else str(usados[nombre])
            print("nombre repetido:", nombre, "-> se usa", f"{nombre} ({extra})")
            nombre = f"{nombre} ({extra})"
        usados.setdefault(nombre, 1)
        entries.append((nombre, rows))

    # deduplicar por mascara+colores
    seen, kept = set(), []
    for name, rows in entries:
        sig = tuple(tuple(c for c in row) for row in rows)
        if sig in seen:
            print("repetido, se omite:", name)
            continue
        seen.add(sig)
        kept.append((name, rows))

    # ---- paleta compartida: cada figura guarda indices, no colores literales
    palette = sorted(set(palette))
    chars = "0123456789abcdefghijklmnopqrstuvwxyz"
    palette_hex = ",".join("%02X%02X%02X" % c for c in palette)

    def encode(rows):
        out = []
        for row in rows:
            s = ""
            for c in row:
                s += "." if c is None else chars[palette.index(quantize(c, palette))]
            out.append(s)
        return "/".join(out)

    # ---- Kotlin
    lines = ["package com.pixelados.data.model", ""]
    lines.append("/**")
    lines.append(" * Símbolos de la categoría Pokémon, creados a partir de las")
    lines.append(" * referencias pixel-art de assets/pokemon.")
    lines.append(" *")
    lines.append(" * Cada celda guarda su COLOR original, así que se pueden estampar")
    lines.append(" * con el color activo (relleno o contorno) o con sus colores propios.")
    lines.append(" *")
    lines.append(" * Las figuras se guardan como texto (una letra por celda, '.' = hueco)")
    lines.append(" * para que el archivo y el APK se mantengan pequeños.")
    lines.append(" *")
    lines.append(" * Total: %d figuras." % len(kept))
    lines.append(" */")
    lines.append("object PokemonCatalog {")
    lines.append('    val category: String = "Pokémon"')
    lines.append("")
    lines.append('    private const val CHARS = "%s"' % chars)
    lines.append('    private const val PALETTE = "%s"' % palette_hex)
    lines.append("")
    lines.append("    private val COLORS: IntArray by lazy {")
    lines.append('        PALETTE.split(",").map { 0xFF000000.toInt() or it.toInt(16) }.toIntArray()')
    lines.append("    }")
    lines.append("")
    lines.append("    /** Convierte el texto (una letra por celda) en la matriz de colores. */")
    lines.append("    private fun decode(figura: String): List<List<Int>> =")
    lines.append('        figura.split("/").map { fila ->')
    lines.append('            fila.map { ch -> if (ch == \'.\') 0 else COLORS[CHARS.indexOf(ch)] }')
    lines.append("        }")
    lines.append("")
    lines.append("    val all: List<Stamp> = listOf(")
    for name, rows in kept:
        encoded = encode(rows)
        # se parte en trozos para que ninguna línea sea kilométrica
        chunks = [encoded[i:i + 90] for i in range(0, len(encoded), 90)]
        if len(chunks) == 1:
            lines.append('        Stamp(name = "%s", category = category, hasOwnColors = true, pattern = decode("%s")),' % (name, chunks[0]))
        else:
            lines.append('        Stamp(')
            lines.append('            name = "%s",' % name)
            lines.append('            category = category,')
            lines.append('            hasOwnColors = true,')
            lines.append('            pattern = decode(')
            for ch in chunks:
                lines.append('                "%s" +' % ch)
            lines.append("            \"\")")
            lines.append('        ),')
    lines.append("    )")
    lines.append("}")
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")
    print("pokemon:", len(kept))

    # ---- vista previa
    cols = 8
    cell = 96
    rows_n = (len(kept) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell, rows_n * cell), (250, 250, 252))
    for i, (name, rows) in enumerate(kept):
        tile = Image.new("RGB", (cell - 8, cell - 8), (255, 255, 255))
        p = (cell - 8) / N
        for y, row in enumerate(rows):
            for x, c in enumerate(row):
                if c is not None:
                    tile.paste(Image.new("RGB", (int(p) + 1, int(p) + 1), c), (int(x * p), int(y * p)))
        sheet.paste(tile, ((i % cols) * cell + 4, (i // cols) * cell + 4))
    sheet.save(PREVIEW)


if __name__ == "__main__":
    main()
