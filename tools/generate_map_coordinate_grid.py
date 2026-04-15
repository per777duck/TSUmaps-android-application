"""
Генерирует PNG карты ТГУ с сеткой и подписями координат (пиксели = Offset в Compose).
Запуск: python tools/generate_map_coordinate_grid.py
"""
from __future__ import annotations

import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    print("Нужен Pillow: pip install Pillow", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
MAP_PATH = ROOT / "app" / "src" / "main" / "res" / "drawable" / "map_original.png"
OUT_PATH = ROOT / "tools" / "map_coordinate_reference.png"

# Совпадает с CAMPUS_MAP_WIDTH_PX / CAMPUS_MAP_HEIGHT_PX в FoodRoutingData.kt
MAP_W, MAP_H = 784, 757

MINOR_STEP = 50
MAJOR_STEP = 100


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        Path(r"C:\Windows\Fonts\segoeui.ttf"),
        Path(r"C:\Windows\Fonts\arial.ttf"),
        Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
    ]
    for p in candidates:
        if p.is_file():
            try:
                return ImageFont.truetype(str(p), size=size)
            except OSError:
                continue
    return ImageFont.load_default()


def main() -> None:
    if not MAP_PATH.is_file():
        print(f"Не найден файл карты: {MAP_PATH}", file=sys.stderr)
        sys.exit(1)

    base = Image.open(MAP_PATH).convert("RGBA")
    if base.size != (MAP_W, MAP_H):
        print(f"Предупреждение: размер карты {base.size}, ожидалось {(MAP_W, MAP_H)}", file=sys.stderr)

    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    font_small = load_font(11)
    font_axis = load_font(12)
    font_title = load_font(14)

    # Сетка
    for x in range(0, MAP_W + 1, MINOR_STEP):
        w = 2 if x % MAJOR_STEP == 0 else 1
        alpha = 200 if x % MAJOR_STEP == 0 else 90
        color = (255, 80, 80, alpha) if x % MAJOR_STEP == 0 else (255, 255, 255, 70)
        draw.line([(x, 0), (x, MAP_H)], fill=color, width=w)
    for y in range(0, MAP_H + 1, MINOR_STEP):
        w = 2 if y % MAJOR_STEP == 0 else 1
        alpha = 200 if y % MAJOR_STEP == 0 else 90
        color = (80, 120, 255, alpha) if y % MAJOR_STEP == 0 else (255, 255, 255, 70)
        draw.line([(0, y), (MAP_W, y)], fill=color, width=w)

    # Подписи по верхнему краю (ось X) и левому (ось Y)
    pad = 2
    for x in range(0, MAP_W + 1, MAJOR_STEP):
        label = str(x)
        tw, th = draw.textbbox((0, 0), label, font=font_axis)[2:]
        draw.rectangle(
            [x - tw // 2 - 1, pad, x + tw // 2 + 1, pad + th + 2],
            fill=(0, 0, 0, 160),
        )
        draw.text((x - tw // 2, pad + 1), label, fill=(255, 255, 255, 255), font=font_axis)

    for y in range(MAJOR_STEP, MAP_H + 1, MAJOR_STEP):
        label = str(y)
        tw, th = draw.textbbox((0, 0), label, font=font_axis)[2:]
        draw.rectangle(
            [pad, y - th // 2 - 1, pad + tw + 4, y + th // 2 + 1],
            fill=(0, 0, 0, 160),
        )
        draw.text((pad + 2, y - th // 2), label, fill=(255, 255, 255, 255), font=font_axis)

    # Заголовок-легенда
    title = "Пиксели = Offset(x, y) в Kotlin | сетка 50, жирные линии 100 | 0..783 по X, 0..756 по Y"
    tw, th = draw.textbbox((0, 0), title, font=font_title)[2:]
    draw.rectangle([4, MAP_H - th - 14, 12 + tw, MAP_H - 4], fill=(0, 0, 0, 180))
    draw.text((8, MAP_H - th - 10), title, fill=(255, 255, 220, 255), font=font_title)

    out = Image.alpha_composite(base, overlay)
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT_PATH, "PNG", optimize=True)
    print(f"Сохранено: {OUT_PATH}")


if __name__ == "__main__":
    main()
