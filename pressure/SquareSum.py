#!/usr/bin/env python3
import csv
from collections import defaultdict
import os

def read_pressure_file(path):
    """
    Read a pressure file of the form:
      second,pressure
    where second may be an integer or float (we floor to int).
    Returns a dict[int -> float].
    """
    data = defaultdict(float)
    if not os.path.exists(path):
        print(f"Warning: {path} not found. Treating as empty.")
        return data

    with open(path, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        for row in reader:
            if not row or len(row) < 2:
                continue
            try:
                t = float(row[0])
                sec = int(t)  # bucket by integer second
                val = float(row[1])
                data[sec] += val
            except ValueError:
                # skip malformed rows
                continue
    return data

def write_pressure_file(path, data):
    """
    Write dict[int -> float] to CSV sorted by time ascending.
    """
    with open(path, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        for sec in sorted(data.keys()):
            writer.writerow([sec, repr(data[sec])])

def combine_pair(in_a, in_b, out_path):
    a = read_pressure_file(in_a)
    b = read_pressure_file(in_b)

    combined = defaultdict(float)
    for sec, val in a.items():
        combined[sec] += val
    for sec, val in b.items():
        combined[sec] += val

    if not combined:
        print(f"No data found in {in_a} or {in_b}. Writing empty {out_path}.")
    write_pressure_file(out_path, combined)
    print(f"Wrote {out_path} ({len(combined)} seconds).")

def main():
    pairs = [
        ("pressure_0.csv", "pressure_1.csv", "pressure_01.csv"),
        ("pressure_2.csv", "pressure_3.csv", "pressure_23.csv"),
    ]

    for a, b, out in pairs:
        combine_pair(a, b, out)

if __name__ == "__main__":
    main()

