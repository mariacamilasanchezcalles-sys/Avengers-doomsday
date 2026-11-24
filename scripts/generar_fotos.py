"""Genera docs/fotos/fotos.json y las miniaturas en docs/fotos/_mini/.

Se ejecuta solo en GitHub (workflow "Fotos") cada vez que se suben fotos.
También se puede correr a mano:  python3 scripts/generar_fotos.py
Necesita Pillow (pip install pillow); sin Pillow genera la lista sin miniaturas.
"""
import json
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent / "docs" / "fotos"
MINI = RAIZ / "_mini"
EXT = {".jpg", ".jpeg", ".png", ".webp", ".gif"}
LADO_MINI = 480

try:
    from PIL import Image, ImageOps
except ImportError:  # pragma: no cover
    Image = None


def orden_natural(texto):
    return [int(t) if t.isdigit() else t.lower() for t in re.split(r"(\d+)", texto)]


def nombre_seccion(carpeta):
    # "01 X-Men" -> "X-Men"; "02-Loki" -> "Loki"
    return re.sub(r"^\d+[\s._-]*", "", carpeta).strip() or carpeta


def miniatura(origen, destino):
    if Image is None:
        return False
    if destino.exists() and destino.stat().st_mtime >= origen.stat().st_mtime:
        return True
    try:
        with Image.open(origen) as img:
            img = ImageOps.exif_transpose(img)
            img.thumbnail((LADO_MINI, LADO_MINI))
            if img.mode not in ("RGB", "L"):
                img = img.convert("RGB")
            destino.parent.mkdir(parents=True, exist_ok=True)
            img.save(destino, "JPEG", quality=78, optimize=True, progressive=True)
        return True
    except Exception as e:  # foto dañada o formato raro: se muestra sin miniatura
        print(f"  ⚠️  sin miniatura para {origen.name}: {e}", file=sys.stderr)
        return False


def main():
    fotos = []
    usadas = set()
    archivos = [
        p for p in RAIZ.rglob("*")
        if p.is_file() and p.suffix.lower() in EXT and MINI not in p.parents
    ]
    archivos.sort(key=lambda p: [orden_natural(x) for x in p.relative_to(RAIZ).parts])
    for p in archivos:
        rel = p.relative_to(RAIZ)
        seccion = nombre_seccion(rel.parts[0]) if len(rel.parts) > 1 else "General"
        entrada = {"archivo": rel.as_posix(), "seccion": seccion}
        txt = p.with_suffix(".txt")
        if txt.exists():
            entrada["texto"] = txt.read_text(encoding="utf-8").strip()
        mini_rel = Path("_mini") / rel.with_suffix(".jpg")
        if miniatura(p, RAIZ / mini_rel):
            entrada["mini"] = mini_rel.as_posix()
            usadas.add((RAIZ / mini_rel).resolve())
        fotos.append(entrada)

    # Borrar miniaturas de fotos que ya no existen
    if MINI.exists():
        for m in MINI.rglob("*"):
            if m.is_file() and m.resolve() not in usadas:
                m.unlink()

    (RAIZ / "fotos.json").write_text(
        json.dumps({"fotos": fotos}, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
    )
    print(f"{len(fotos)} fotos en docs/fotos/fotos.json")


if __name__ == "__main__":
    main()
