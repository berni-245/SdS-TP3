#!/usr/bin/env python3
"""
Read impulse_*.csv files, divide each impulse by a wall-specific divisor that
depends on L (command-line arg), bucket by integer second, and write pressure_<id>.csv.

Usage:
    python3 process_impulses.py <L>
Example:
    python3 process_impulses.py 0.01
"""
import glob
import os
import csv
import argparse
from collections import defaultdict

def parse_args():
    p = argparse.ArgumentParser(description="Process impulse files into pressure files with per-wall divisors depending on L.")
    p.add_argument("L", type=float, help="Value L used to compute certain divisors (e.g. 0.01)")
    return p.parse_args()

def make_divisors(L):
    # Base numbers and where to subtract L (as you specified)
    return {
        0: 0.09 * 3 + ((0.09 - L) / 2),
        1: 0.09 * 3 + ((0.09 - L) / 2),
        2: 0.09*2 + L,
        3: 0.09*2 + L,
    }

def process_file(file_path, divisors):
    base = os.path.basename(file_path)
    id_str = base.split("_")[-1].replace(".csv", "")
    try:
        wall_id = int(id_str)
    except ValueError:
        print(f"Warning: couldn't parse wall id from '{base}'. Writing to pressure_unknown.csv")
        wall_id = None

    divisor = divisors.get(wall_id)
    if divisor == 0:
        print(f"Warning: divisor for wall {wall_id} is 0. Will skip division and keep original impulse values.")
        divisor = None  # signal: do not divide

    buckets = defaultdict(float)

    # Read impulse file
    with open(file_path, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        for row in reader:
            if not row or len(row) < 2:
                continue
            try:
                time = float(row[0])
                impulse = float(row[1])
            except ValueError:
                print(f"Skipping malformed row in {file_path}: {row}")
                continue

            if divisor is None:
            	raise ValueError("Divisor says no")
            impulse = impulse / divisor

            bucket = int(time)  # floor to integer seconds
            buckets[bucket] += impulse

    # Write pressure file
    out_name = f"pressure_{wall_id}.csv" if wall_id is not None else "pressure_unknown.csv"
    with open(out_name, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        for t in sorted(buckets.keys()):
            writer.writerow([t, buckets[t]])

    printed_div = "unchanged" if divisor is None else divisors.get(wall_id, 1.0)
    print(f"Created {out_name} (from {base}; divisor={printed_div})")

def main():
    args = parse_args()
    divisors = make_divisors(args.L)

    files = glob.glob("impulse_*.csv")
    if not files:
        print("No impulse_*.csv files found in the current directory.")
        return

    for fp in sorted(files):
        process_file(fp, divisors)

if __name__ == "__main__":
    main()

