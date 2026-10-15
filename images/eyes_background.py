#!/usr/bin/env python3
"""Render the Angelic Industries eyes as a looping GIF background, plus a still
of the same scene that Catalogue shows while the GIF loads.

    python3 images/eyes_background.py src/main/resources/assets/angellib/background.gif src/main/resources/assets/angellib/background.png

Needs Pillow and ImageMagick (`magick`). Pillow renders the frames, ImageMagick
assembles them, because Pillow's GIF writer merges identical frames into longer
ones and AngelLib plays every frame with the first frame's delay.
"""
import math
import random
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

W, H = 128, 64
SS = 8  # supersampling, for smooth edges after downscaling
FPS = 10
# Each eye blinks once per loop, so this also sets how often they blink.
SECONDS = 30
SEED = 1015

# Bigger eyes are closer and brighter, the smallest fade into the dark.
NEAR_BRIGHTNESS, FAR_BRIGHTNESS = 0.9, 0.15
BG = (0, 0, 0)
LINE = (242, 242, 242)
IRIS = (218, 9, 9)
STROKE = 1.0  # outline width in output px
STILL_SCALE = 4  # the still is upscaled with hard pixels, so it matches the GIF on screen

COUNT = 20
MIN_W, MAX_W = 8, 22  # eye widths from far to near, in output px
CANDIDATES = 12  # more candidates spread the eyes more evenly

# Blink timing in seconds. At 10 fps a blink shows as half shut, shut, half open, nearly open.
CLOSE, SHUT, OPEN = 0.15, 0.10, 0.20
DOUBLE_CHANCE = 0.15
DOUBLE_GAP = 0.5


def gap(a, b):
    """Space between two eyes. Eyes are wide and flat, so vertical distance counts extra."""
    return math.hypot(a["x"] - b["x"], (a["y"] - b["y"]) * 1.6) - (a["width"] + b["width"]) / 2


def layout(rng):
    """Random scatter: each eye takes the best of several random spots, the one
    furthest from the eyes already placed. Big eyes go first."""
    # Mostly small and far away, a few big and close.
    widths = sorted((MIN_W + rng.random() ** 1.7 * (MAX_W - MIN_W) for _ in range(COUNT)), reverse=True)
    eyes = []
    for width in widths:
        spots = [
            {"x": rng.uniform(width / 2 + 1, W - width / 2 - 1), "y": rng.uniform(width / 4 + 1, H - width / 4 - 1), "width": width}
            for _ in range(CANDIDATES)
        ]
        eye = max(spots, key=lambda s: min((gap(s, e) for e in eyes), default=0))
        eye["blinks"] = blinks(rng)
        near = (width - MIN_W) / (MAX_W - MIN_W)
        eye["brightness"] = FAR_BRIGHTNESS + near * (NEAR_BRIGHTNESS - FAR_BRIGHTNESS)
        eyes.append(eye)
    return eyes


def blinks(rng):
    """One blink per loop at a random frame, sometimes a double blink.
    Times wrap around the loop, so it repeats seamlessly."""
    t = rng.randrange(FPS * SECONDS) / FPS
    times = [t]
    if rng.random() < DOUBLE_CHANCE:
        times.append((t + DOUBLE_GAP) % SECONDS)
    return times


def openness(t, blink_times):
    """1 is fully open, 0 fully shut."""
    level = 1.0
    for b in blink_times:
        # Time since this blink started, wrapped around the loop.
        d = (t - b + SECONDS / 2) % SECONDS - SECONDS / 2
        if 0 <= d < CLOSE:
            v = 1 - (d / CLOSE) ** 2
        elif CLOSE <= d < CLOSE + SHUT:
            v = 0.0
        elif CLOSE + SHUT <= d < CLOSE + SHUT + OPEN:
            v = 1 - (1 - (d - CLOSE - SHUT) / OPEN) ** 2
        else:
            continue
        level = min(level, v)
    return level


def almond(cx, cy, unit, squash, steps=24):
    """The site's eye path, M-45 0 Q0-36 45 0 Q0 36-45 0, squashed vertically."""
    top, bottom = [], []
    for i in range(steps + 1):
        s = i / steps
        x = (-45 + 90 * s) * unit
        # Quadratic Bezier with its control point at (0, -36).
        y = 2 * s * (1 - s) * 36 * unit * squash
        top.append((cx + x, cy - y))
        bottom.append((cx - x, cy + y))
    return top + bottom[1:]


def draw_eye(frame, eye, squash):
    if squash <= 0:
        return  # shut eyes are invisible
    line, iris_color = (tuple(round(c * eye["brightness"]) for c in color) for color in (LINE, IRIS))
    unit = eye["width"] / 100 * SS
    cx, cy = eye["x"] * SS, eye["y"] * SS
    shape = almond(cx, cy, unit, squash)

    # Iris and pupil, clipped to the eye shape.
    iris = Image.new("RGB", frame.size, BG)
    d = ImageDraw.Draw(iris)
    for r, color in ((15, iris_color), (6, BG)):
        d.ellipse((cx - r * unit, cy - r * unit * squash, cx + r * unit, cy + r * unit * squash), fill=color)
    mask = Image.new("L", frame.size, 0)
    ImageDraw.Draw(mask).polygon(shape, fill=255)
    frame.paste(iris, mask=mask)

    ImageDraw.Draw(frame).line(shape + [shape[0]], fill=line, width=round(STROKE * SS), joint="curve")


def main(out, still=None):
    eyes = layout(random.Random(SEED))
    frames = []
    for f in range(FPS * SECONDS):
        big = Image.new("RGB", (W * SS, H * SS), BG)
        for eye in eyes:
            draw_eye(big, eye, openness(f / FPS, eye["blinks"]))
        frames.append(big.resize((W, H), Image.Resampling.BOX))

    # One palette built from every frame, no dithering, so nothing shimmers between frames.
    sheet = Image.new("RGB", (W, H * len(frames)))
    for i, frame in enumerate(frames):
        sheet.paste(frame, (0, i * H))
    palette = sheet.quantize(255, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    frames = [frame.quantize(palette=palette, dither=Image.Dither.NONE) for frame in frames]

    with tempfile.TemporaryDirectory() as tmp:
        paths = []
        for i, frame in enumerate(frames):
            path = Path(tmp) / f"{i:04d}.png"
            frame.save(path)
            paths.append(str(path))
        subprocess.run(["magick", "-delay", str(100 // FPS), "-loop", "0", "-dispose", "None", *paths, out], check=True)

    if still:
        # First frame where every eye is open.
        f = next(f for f in range(len(frames)) if all(openness(f / FPS, e["blinks"]) == 1 for e in eyes))
        frames[f].convert("RGB").resize((W * STILL_SCALE, H * STILL_SCALE), Image.Resampling.NEAREST).save(still, optimize=True)


if __name__ == "__main__":
    main(*sys.argv[1:3])
