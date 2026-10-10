"""Fail CI when an official 26.x Minecraft release is missing from the test matrix."""
import json
import re
from pathlib import Path
from urllib.request import Request, urlopen

manifest = Path(__file__).resolve().parents[1] / "gradle/paper-versions.properties"
known = {line.split("=", 1)[0].strip() for line in manifest.read_text().splitlines() if "=" in line and not line.startswith("#")}
request = Request("https://fill.papermc.io/v3/projects/paper", headers={"User-Agent": "MeowTools-compatibility-check/1.1"})
with urlopen(request, timeout=30) as response:
    project = json.load(response)
releases = {version for versions in project["versions"].values() for version in versions if re.fullmatch(r"26\.\d+(?:\.\d+)?", version)}
if not releases:
    raise SystemExit("Paper API returned no 26.x releases; check response format")
missing = releases - known
if missing:
    raise SystemExit("Add and verify new Minecraft releases in the compatibility matrix: " + ", ".join(sorted(missing)))
print("All published 26.x Minecraft releases are covered: " + ", ".join(sorted(releases)))
