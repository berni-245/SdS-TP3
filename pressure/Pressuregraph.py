#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Pressuregraph.py

Combined pressure plot (smooth wave) with selectable smoothing level.

Usage:
  python3 Pressuregraph.py [--smooth LEVEL] [file1.csv file2.csv ...]
LEVEL: none | low | med | high   (default: low)

Outputs: combined_wave.png (and shows the plot)
"""
import sys
import os
import csv

# require numpy + matplotlib
try:
    import numpy as np
except Exception:
    print("This script requires numpy. Install with:\n  python3 -m pip install --user numpy matplotlib")
    sys.exit(1)

import matplotlib.pyplot as plt

# optional pandas convenience
try:
    import pandas as pd
except Exception:
    pd = None

DEFAULT_FILES = ["pressure_01.csv", "pressure_23.csv"]
OUTFILE = "combined_wave.png"

UPSAMPLE_BASE = 500  # base number of upsample points (scaled per level)

def read_pairs_with_csv(fname):
    times = []
    pressures = []
    with open(fname, newline='') as fh:
        reader = csv.reader(fh)
        for row in reader:
            if not row:
                continue
            if len(row) == 1:
                parts = row[0].split(',')
            else:
                parts = row
            try:
                t = float(parts[0].strip())
                p = float(parts[1].strip())
            except (ValueError, IndexError):
                continue
            times.append(t)
            pressures.append(p)
    return np.array(times, dtype=float), np.array(pressures, dtype=float)

def read_file(fname):
    if not os.path.exists(fname):
        return np.array([]), np.array([])
    try:
        if pd is not None:
            df = pd.read_csv(fname, header=None, names=["time","pressure"])
            times = df["time"].astype(float).to_numpy()
            pressures = df["pressure"].astype(float).to_numpy()
        else:
            times, pressures = read_pairs_with_csv(fname)
    except Exception as e:
        print(f"[ERROR] reading {fname}: {e}")
        return np.array([]), np.array([])
    return times, pressures

def gaussian_kernel(width, sigma):
    if width % 2 == 0:
        width += 1
    half = width // 2
    x = np.arange(-half, half+1)
    k = np.exp(-0.5 * (x / sigma)**2)
    k /= k.sum()
    return k

def smooth_series(x, y, upsample_points=UPSAMPLE_BASE, kernel_width=5, sigma=1.0):
    if len(x) < 2:
        return np.array([]), np.array([])
    x_fine = np.linspace(x.min(), x.max(), upsample_points)
    y_interp = np.interp(x_fine, x, y)
    kernel = gaussian_kernel(kernel_width, sigma)
    y_smooth = np.convolve(y_interp, kernel, mode='same')
    return x_fine, y_smooth

def plot_combined(files, smooth_level="low"):
    # smoothing parameter presets
    presets = {
        "none": {"upsample": None, "kw": None},        # plot raw lines
        "low":  {"upsample": 300, "kw": (5, 0.8)},    # gentle
        "med":  {"upsample": 500, "kw": (7, 1.6)},    # moderate
        "high": {"upsample": 800, "kw": (11, 3.0)},   # strong
    }
    if smooth_level not in presets:
        print(f"[WARN] unknown smooth level '{smooth_level}', using 'low'.")
        smooth_level = "low"
    params = presets[smooth_level]

    plt.figure(figsize=(10, 6))
    plotted = 0
    names = ["Cuadrado", "Rectangulo"]

    for i, fname in enumerate(files):
        times, pressures = read_file(fname)
        if times.size == 0:
            print(f"[WARN] no data in {fname}; skipping.")
            continue

        if params["upsample"] is None:
            plt.plot(times, pressures, linestyle='-', linewidth=1.5, label=names[i])
        else:
            up, kw = params["upsample"], params["kw"]
            kernel_width, sigma = kw
            x_s, y_s = smooth_series(
                times, pressures,
                upsample_points=up,
                kernel_width=kernel_width,
                sigma=sigma
            )
            if x_s.size == 0:
                continue
            plt.plot(x_s, y_s, linewidth=2, label=names[i])
            plt.scatter(times, pressures, s=12, alpha=0.35)

    # Use actual delta symbol (Δ) and keep "Pressure" and "Time" in labels.
    plt.ylim(bottom=0, top=5)
    plt.xlabel("Tiempo (t)")
    plt.ylabel("Presión (N/m²)")
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.savefig(OUTFILE, dpi=300)
    print(f"[OK] Saved combined plot to: {OUTFILE}")
    plt.show()
    plt.close()
    return True


def parse_args(argv):
    smooth = "low"
    files = []
    i = 1
    while i < len(argv):
        a = argv[i]
        if a in ("--smooth", "-s") and i+1 < len(argv):
            smooth = argv[i+1].lower()
            i += 2
        else:
            files.append(a)
            i += 1
    if not files:
        files = DEFAULT_FILES[:]
    return smooth, files

def main(argv):
    smooth, files = parse_args(argv)
    plot_combined(files, smooth_level=smooth)

if __name__ == "__main__":
    main(sys.argv)

