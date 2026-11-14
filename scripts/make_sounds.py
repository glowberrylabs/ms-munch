#!/usr/bin/env python3
"""Generate the bundled sound effects using only the Python standard library."""

import math
import random
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 22050
OUT_DIR = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"


def envelope(t: float, duration: float, attack: float = 0.006, release: float = 0.02) -> float:
    if t < attack:
        return t / attack
    if t > duration - release:
        return max(0.0, (duration - t) / release)
    return 1.0


def tone(
    start_freq: float,
    end_freq: float,
    duration: float,
    volume: float = 0.5,
    waveform: str = "square",
    vibrato: float = 0.0,
) -> list[float]:
    samples: list[float] = []
    phase = 0.0
    count = int(SAMPLE_RATE * duration)
    for index in range(count):
        t = index / SAMPLE_RATE
        mix = t / duration
        frequency = start_freq + (end_freq - start_freq) * mix
        if vibrato:
            frequency += math.sin(2.0 * math.pi * 28.0 * t) * vibrato
        phase += 2.0 * math.pi * frequency / SAMPLE_RATE
        if waveform == "square":
            value = 1.0 if math.sin(phase) >= 0.0 else -1.0
        elif waveform == "triangle":
            value = (2.0 / math.pi) * math.asin(math.sin(phase))
        else:
            value = math.sin(phase)
        samples.append(value * envelope(t, duration) * volume)
    return samples


def noise(duration: float, volume: float, seed: int) -> list[float]:
    generator = random.Random(seed)
    count = int(SAMPLE_RATE * duration)
    return [
        generator.uniform(-1.0, 1.0) * envelope(index / SAMPLE_RATE, duration) * volume
        for index in range(count)
    ]


def mix(*tracks: list[float]) -> list[float]:
    length = max(len(track) for track in tracks)
    result = [0.0] * length
    for track in tracks:
        for index, value in enumerate(track):
            result[index] += value
    return result


def sequence(notes: list[tuple[float, float, str, float]]) -> list[float]:
    result: list[float] = []
    for frequency, duration, waveform, volume in notes:
        result.extend(tone(frequency, frequency, duration, volume, waveform))
    return result


def write(name: str, samples: list[float], gain: float = 0.72) -> None:
    peak = max(1e-9, max(abs(value) for value in samples))
    scale = gain / peak
    frames = b"".join(
        struct.pack("<h", int(max(-1.0, min(1.0, value * scale)) * 32767))
        for value in samples
    )
    with wave.open(str(OUT_DIR / name), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(SAMPLE_RATE)
        handle.writeframes(frames)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    write(
        "chomp_a.wav",
        mix(
            tone(470.0, 310.0, 0.075, 0.55, "square"),
            noise(0.045, 0.08, 3),
        ),
    )
    write(
        "chomp_b.wav",
        mix(
            tone(390.0, 250.0, 0.075, 0.55, "square"),
            noise(0.045, 0.08, 8),
        ),
    )
    write(
        "pellet.wav",
        mix(
            tone(640.0, 980.0, 0.12, 0.4, "triangle"),
            tone(1280.0, 1960.0, 0.09, 0.15, "sine"),
        ),
    )
    write(
        "fruit.wav",
        sequence(
            [
                (523.25, 0.06, "triangle", 0.4),
                (659.25, 0.06, "triangle", 0.4),
                (783.99, 0.12, "triangle", 0.45),
            ]
        ),
    )
    write(
        "ghost_eaten.wav",
        tone(1050.0, 180.0, 0.28, 0.5, "square", vibrato=40.0),
    )
    write(
        "extra_life.wav",
        sequence(
            [
                (523.25, 0.09, "triangle", 0.4),
                (659.25, 0.09, "triangle", 0.4),
                (783.99, 0.09, "triangle", 0.4),
                (1046.5, 0.2, "triangle", 0.45),
            ]
        ),
    )
    write(
        "death.wav",
        mix(
            tone(420.0, 70.0, 0.62, 0.55, "square"),
            tone(210.0, 45.0, 0.62, 0.25, "triangle"),
        ),
    )
    write(
        "level_start.wav",
        sequence(
            [
                (392.0, 0.08, "triangle", 0.4),
                (523.25, 0.08, "triangle", 0.4),
                (659.25, 0.08, "triangle", 0.4),
                (783.99, 0.18, "triangle", 0.45),
            ]
        ),
    )


if __name__ == "__main__":
    main()
