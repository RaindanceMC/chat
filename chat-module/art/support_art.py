"""Paints the support window's art into the chat module's resource-pack folder.

    python3 -I chat-module/art/support_art.py

The header is a font glyph at twice its pixel size, so one art pixel is two GUI pixels and stays crisp at every
GUI scale. The mug icon's steam is an animated texture (frames stacked, .mcmeta).
"""
import json
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
PACK = os.path.join(HERE, '..', 'src', 'main', 'resources', 'de', 'raindancer', 'modules', 'chat', 'pack')
NS = 'rainschat'

ROAST = (42, 24, 14)        # outline, darkest
ESPRESSO = (59, 36, 22)
MOCHA = (92, 58, 35)
COFFEE = (111, 74, 46)
CARAMEL = (192, 138, 90)
LATTE = (217, 191, 160)
CREAM = (243, 227, 204)
FOAM = (250, 242, 230)
BEAN = (78, 45, 25)
BEAN_LIGHT = (122, 76, 44)
HEART = (229, 115, 115)
HEART_DARK = (170, 70, 75)
GLOW = (245, 217, 168)


def put(img, x, y, colour):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), colour + (255,) if len(colour) == 3 else colour)


def rect(img, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, colour)


def blend(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def bean(img, x, y, flip=False):
    """A 4x5 coffee bean with its crease."""
    shape = ['.##.', '#hh#', '#c##', '##c#', '.##.'] if not flip else ['.##.', '#hh#', '##c#', '#c##', '.##.']
    for dy, row in enumerate(shape):
        for dx, ch in enumerate(row):
            if ch == '#':
                put(img, x + dx, y + dy, BEAN)
            elif ch == 'h':
                put(img, x + dx, y + dy, BEAN_LIGHT)
            elif ch == 'c':
                put(img, x + dx, y + dy, ROAST)


def heart(img, x, y):
    for dy, row in enumerate(['.#.#.', '#####', '#####', '.###.', '..#..']):
        for dx, ch in enumerate(row):
            if ch == '#':
                put(img, x + dx, y + dy, HEART if dy < 3 or dx == 2 else HEART_DARK)
    put(img, x + 1, y + 1, (255, 190, 190))


def steam(img, x, y, height, phase, colour=CREAM):
    """A wisp rising from (x, y) upwards, swaying; fainter towards the top."""
    sway = [0, 0, 1, 1, 1, 0, 0, -1, -1, -1]
    for i in range(height):
        sx = x + sway[(i + phase) % len(sway)]
        fade = 1 - i / height
        if fade < 0.35 and (i + sx) % 2:
            continue   # dithered thinning at the top
        put(img, sx, y - i, blend(MOCHA, colour, 0.35 + 0.65 * fade))


def mug(img, x, y, w, h, handle=True):
    """A cream mug, its top-left outline corner at (x, y): coffee with crema, shading, a handle on the right."""
    # body
    rect(img, x, y, x + w - 1, y + h - 1, ROAST)
    rect(img, x + 1, y + 1, x + w - 2, y + h - 2, CREAM)
    for yy in range(y + 1, y + h - 1):
        put(img, x + w - 2, yy, LATTE)
        put(img, x + w - 3, yy, blend(CREAM, LATTE, 0.5))
        put(img, x + 1, yy, FOAM)
    # coffee and crema at the rim
    rect(img, x + 1, y + 1, x + w - 2, y + 2, MOCHA)
    for xx in range(x + 2, x + w - 2):
        put(img, xx, y + 1, CARAMEL if (xx - x) % 3 else LATTE)
    # a heart on the mug
    hx, hy = x + w // 2 - 2, y + h // 2 - 1
    for dy, row in enumerate(['#.#', '###', '.#.']):
        for dx, ch in enumerate(row):
            if ch == '#':
                put(img, hx + dx, hy + dy, HEART)
    if handle:
        hx = x + w
        rect(img, hx, y + 3, hx + 2, y + 3, ROAST)
        rect(img, hx, y + h - 4, hx + 2, y + h - 4, ROAST)
        rect(img, hx + 2, y + 3, hx + 3, y + h - 4, ROAST)
        rect(img, hx, y + 4, hx + 1, y + h - 5, CREAM)
        put(img, hx + 2, y + 4, LATTE)
        put(img, hx + 2, y + h - 5, LATTE)
        for yy in range(y + 4, y + h - 4):
            put(img, hx + 2, yy, LATTE)
        rect(img, hx, y + 5, hx + 1, y + h - 6, ESPRESSO)


PALETTE = {'o': ROAST, 'e': ESPRESSO, 'm': MOCHA, 'k': CARAMEL, 'l': LATTE, 'c': CREAM, 'f': FOAM,
           'h': HEART, 'd': HEART_DARK, 'w': (255, 200, 200)}

BIG_MUG = [
    '.ooooooooooooooooo.....',
    '.okkkllkkkllkkkllko....',
    '.ommmmmmmmmmmmmmmmoooo.',
    '.ofcccccccccccccllo..oo',
    '.ofccccccccccccclloc.lo',
    '.ofcccchhcchhcccllocclo',
    '.ofccchwhhhhhhccllo.clo',
    '.ofccchhhhhhhdccllo.clo',
    '.ofcccchhhhhdcccllocclo',
    '.ofccccchhhdccccllo.loo',
    '.ofcccccchdcccccllooo..',
    '.ofcccccccccccccllo....',
    '..olllllllllllllllo....',
    '...ooooooooooooooo.....',
]
BIG_SAUCER = [
    'oooooooooooooooooooooooooo',
    '.llllllllllllllllllllllll.',
    '..oooooooooooooooooooooo..',
]
SMALL_MUG = [
    '..ooooooooo.....',
    '..okklkklko.....',
    '..ommmmmmmoooo..',
    '..ofcccccco..o..',
    '..ofchcchco..o..',
    '..ofhhhhhlo..o..',
    '..ofchhhclooo...',
    '..ofcchcclo.....',
    '...olllllo......',
    '.oooooooooooo...',
    '..llllllllll....',
]


def stamp(img, x, y, rows):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch in PALETTE:
                put(img, x + dx, y + dy, PALETTE[ch])


def big_bean(img, x, y, flip=False):
    shape = ['.###.', '#kh##', '#h#c#', '##c##', '#c###', '.###.'] if not flip else \
            ['.###.', '##hk#', '#c#h#', '##c##', '###c#', '.###.']
    for dy, row in enumerate(shape):
        for dx, ch in enumerate(row):
            colour = {'#': BEAN, 'h': BEAN_LIGHT, 'k': (150, 98, 60), 'c': ROAST}.get(ch)
            if colour:
                put(img, x + dx, y + dy, colour)


def header():
    w, h = 120, 30
    img = Image.new('RGBA', (w, h))
    tones = [(46, 27, 16), (54, 32, 19), (63, 38, 23), (73, 45, 27), (84, 53, 32), (97, 62, 38)]
    bayer = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
    for y in range(h):
        for x in range(w):
            centre = max(0.0, 1 - ((x - 60) / 48) ** 2 - ((y - 15) / 26) ** 2)
            t = 0.25 + 0.75 * centre
            level = t * (len(tones) - 1) + (bayer[y % 4][x % 4] / 16 - 0.5) * 0.9
            img.putpixel((x, y), tones[max(0, min(len(tones) - 1, round(level)))] + (255,))
    for (cx, cy) in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
        img.putpixel((cx, cy), (0, 0, 0, 0))
    # the counter
    rect(img, 0, 26, w - 1, h - 1, ROAST)
    rect(img, 0, 26, w - 1, 26, MOCHA)
    rect(img, 0, 27, w - 1, 27, ESPRESSO)
    stamp(img, 47, 24, BIG_SAUCER)
    stamp(img, 49, 11, BIG_MUG)
    for x, height, phase in [(54, 10, 0), (59, 11, 4), (64, 9, 7)]:
        steam(img, x, 9, height, phase)
        steam(img, x + 1, 9, height - 3, phase)
    heart(img, 71, 1)
    for (x, y, flip) in [(28, 20, False), (35, 21, True), (85, 20, True), (92, 21, False), (21, 21, True),
                         (99, 20, False), (10, 5, False), (105, 7, True), (18, 12, True), (110, 15, False)]:
        big_bean(img, x, y, flip)
    rnd = random.Random(7)
    for _ in range(16):
        x, y = rnd.randrange(4, w - 4), rnd.randrange(2, 19)
        if not 42 < x < 80:
            put(img, x, y, GLOW if rnd.random() < 0.5 else CARAMEL)
    return img


def icon_frames():
    """16x16 mug, four frames of rising steam."""
    frames = []
    for phase in range(4):
        img = Image.new('RGBA', (16, 16))
        stamp(img, 0, 5, SMALL_MUG)
        for i, x in enumerate([4, 7]):
            steam(img, x, 4, 4, phase * 2 + i * 5, FOAM)
        frames.append(img)
    sheet = Image.new('RGBA', (16, 16 * len(frames)))
    for i, frame in enumerate(frames):
        sheet.paste(frame, (0, 16 * i))
    return sheet


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as out:
        json.dump(data, out, indent=1, ensure_ascii=True)


def main():
    files = []

    def out(rel):
        files.append(rel)
        path = os.path.join(PACK, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        return path

    header().save(out(f'assets/{NS}/textures/font/support/header.png'))
    write_json(out(f'assets/{NS}/font/support.json'), {'providers': [
        {'type': 'bitmap', 'file': f'{NS}:font/support/header.png', 'ascent': 52, 'height': 60, 'chars': ['']}]})

    icon_frames().save(out(f'assets/{NS}/textures/item/coffee.png'))
    write_json(out(f'assets/{NS}/textures/item/coffee.png.mcmeta'), {'animation': {'frametime': 4}})
    write_json(out(f'assets/{NS}/models/item/coffee.json'),
               {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/coffee'}})
    write_json(out(f'assets/{NS}/items/coffee.json'),
               {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/coffee'}})

    write_json(out('pack.mcmeta'), {'pack': {'description': "Rain's Chat — the support window's art",
                                             'min_format': 88, 'max_format': 97}})
    with open(os.path.join(PACK, 'files.txt'), 'w') as index:
        index.write('\n'.join(sorted(files)) + '\n')


if __name__ == '__main__':
    main()
