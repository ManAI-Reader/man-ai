#!/usr/bin/env python3
"""Generate kanji_readings.csv from KANJIDIC2 (EDRDG).

Output: android/app/src/main/assets/kanji_readings.csv
"""

import csv
import gzip
import io
import subprocess
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

KANJIDIC2_URL = "http://www.edrdg.org/kanjidic/kanjidic2.xml.gz"


def main():
    """Download KANJIDIC2 and extract on/kun readings to CSV."""
    print("Downloading KANJIDIC2...")
    response = urllib.request.urlopen(KANJIDIC2_URL)
    xml_bytes = gzip.decompress(response.read())
    xml_str = xml_bytes.decode("utf-8")

    root = ET.fromstring(xml_str)

    output = io.StringIO()
    writer = csv.writer(output)

    count = 0
    for char in root.findall("character"):
        literal = char.find("literal").text
        rm = char.find("reading_meaning")
        if rm is None:
            continue
        rmgroup = rm.find("rmgroup")
        if rmgroup is None:
            continue

        readings = []
        seen = set()
        for r in rmgroup.findall("reading"):
            rtype = r.get("r_type")
            if rtype in ("ja_on", "ja_kun"):
                text = r.text.replace("-", "")
                if text not in seen:
                    seen.add(text)
                    readings.append(text)

        if readings:
            writer.writerow([literal] + readings)
            count += 1

    repo_root = Path(subprocess.check_output(
        ["git", "rev-parse", "--show-toplevel"], text=True
    ).strip())
    dest = repo_root / "android/app/src/main/assets/kanji_readings.csv"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(output.getvalue(), encoding="utf-8")

    print(f"Exported {count} kanji readings to {dest}")


if __name__ == "__main__":
    main()
