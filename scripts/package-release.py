#!/usr/bin/env python3
"""Prepare reviewable release files locally. Performs no network calls or credential reads."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

root = Path(__file__).resolve().parents[1]
properties = dict(line.split("=", 1) for line in (root / "gradle.properties").read_text().splitlines() if "=" in line)
version = properties["version"]
name = f"building-palette-{version}"
artifact = root / "build" / "libs" / f"{name}.jar"
with zipfile.ZipFile(artifact) as jar:
    metadata = json.loads(jar.read("fabric.mod.json"))
    if metadata["version"] != version:
        raise SystemExit("Jar version does not match gradle.properties; rebuild first")
    if any("/test/" in path or "/harness/" in path or path.startswith("dev/tazer/") for path in jar.namelist()):
        raise SystemExit("Test code or dependency implementation found in release jar")
destination = root / "build" / "release"
destination.mkdir(parents=True, exist_ok=True)
hashes = []
for suffix in (".jar", "-sources.jar"):
    source = root / "build" / "libs" / (name + suffix)
    target = destination / source.name
    shutil.copy2(source, target)
    hashes.append(f"{hashlib.sha256(target.read_bytes()).hexdigest()}  {target.name}")
(destination / "SHA256SUMS.txt").write_text("\n".join(hashes) + "\n")
description = (root / "docs" / "MODRINTH.md").read_text()
(destination / "MODRINTH.md").write_text(description)
(destination / "metadata.json").write_text(json.dumps({
    "name": "Building Palette", "version": version, "version_type": "alpha",
    "game_versions": ["26.3"], "loaders": ["fabric"], "client_side": "required", "server_side": "required",
    "dependencies": [
        {"project_id": "P7dR8mSH", "dependency_type": "required"},
        {"project_id": "ewyjbmG4", "version_id": "AQkLCaPY", "dependency_type": "required"},
        {"project_id": "vvgTqF0n", "version_id": "H4rTCX81", "dependency_type": "required"},
    ]
}, indent=2) + "\n")
print(f"Prepared local release files: {destination}")
