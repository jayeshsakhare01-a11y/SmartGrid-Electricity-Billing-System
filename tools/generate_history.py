"""
Generates 10 years (Nov 2016 - Oct 2026) of monthly bills for every customer
in customers.csv and writes bills.csv in the same format oop.java uses.

Run from the project folder:   python3 tools/generate_history.py
Back up bills.csv first - it is overwritten.

Rules mirror oop.java calculateTariff(): 3 slabs (100 / 200 / rest), fixed charge,
5% duty. Meter readings are cumulative (prev = last month's curr).
The two bills you entered by hand (October 2026) are kept exactly:
units and totals unchanged, only the invoice number and meter chain are re-based.
"""
import csv, random

random.seed(2026)

MONTHS = ["January","February","March","April","May","June","July","August",
          "September","October","November","December"]
SEASON = [0.90,0.90,1.10,1.40,1.60,1.20,0.90,0.85,0.90,1.00,0.85,0.85]  # Pune-like: summer peak
TARIFF = {"Residential": (4.5, 7.0, 10.0, 50.0),
          "Commercial":  (8.0, 10.0, 12.0, 150.0),
          "Industrial":  (9.0, 11.0, 14.0, 300.0)}
BASE_UNITS = {"Residential": 85, "Commercial": 150, "Industrial": 600}  # typical Oct-2026 usage
GROWTH = 0.03          # ~3% more usage each year
FIRST = (2016, 11)     # Nov 2016
LAST = (2026, 10)      # Oct 2026
KEPT = {("C001", 2026, 10): 46, ("C002", 2026, 10): 67}   # bills you already entered

def tariff(cat, units):
    r1, r2, r3, fixed = TARIFF[cat]
    energy, rem = 0.0, units
    s1 = min(rem, 100); energy += s1 * r1; rem -= s1
    s2 = min(rem, 200); energy += s2 * r2; rem -= s2
    energy += rem * r3
    tax = (energy + fixed) * 0.05
    return energy, fixed, tax, energy + fixed + tax

customers = []
with open("customers.csv", newline="", encoding="utf-8") as f:
    for row in csv.reader(f):
        if row: customers.append(row)

periods, (y, m) = [], FIRST
while (y, m) <= LAST:
    periods.append((y, m))
    m += 1
    if m == 13: y, m = y + 1, 1

# Every consumer is tracked for the full period (Nov 2016 - Oct 2026);
# consumers differ in how much power they use (separate random generator, so C001/C002 are unchanged).
rng = random.Random(7)
PROFILE = {}
for idx, c in enumerate(customers):
    PROFILE[c[0]] = (0, 1.0) if idx < 2 else (0, rng.uniform(0.7, 1.35))

rows = []
for cid, name, addr, phone, cat in customers:
    reading = 0
    start, scale = PROFILE[cid]
    for (y, m) in periods[start:]:
        if (cid, y, m) in KEPT:
            units = KEPT[(cid, y, m)]
        else:
            years_back = (LAST[0] - y) + (LAST[1] - m) / 12
            level = BASE_UNITS[cat] / ((1 + GROWTH) ** years_back)
            units = max(10, round(level * scale * SEASON[m-1] * random.uniform(0.88, 1.12)))
        prev, curr = reading, reading + units
        reading = curr
        e, fx, t, tot = tariff(cat, units)
        rows.append((y, m, cid, [cid, MONTHS[m-1], y, prev, curr, units, e, fx, t, tot,
                                 f"22-{m:02d}-{y}"]))

rows.sort(key=lambda r: (r[0], r[1], r[2]))          # oldest first, so INV0001 = Nov 2016
with open("bills.csv", "w", newline="", encoding="utf-8") as f:
    for i, (_, _, _, r) in enumerate(rows, 1):
        cid, month, year, prev, curr, units, e, fx, t, tot, date = r
        f.write(",".join([f"INV{i:04d}", cid, month, str(year), str(prev), str(curr),
                          str(units), repr(e), repr(fx), repr(t), repr(tot), date]) + "\r\n")
print(f"Wrote {len(rows)} bills for {len(customers)} customers")
