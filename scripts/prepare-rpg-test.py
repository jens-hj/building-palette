#!/usr/bin/env python3
"""Copy only mods/config into a disposable test fixture. Never copy live saves or credentials."""
import argparse
from pathlib import Path
import shutil

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("source", type=Path, help="Existing RPG instance directory")
args = parser.parse_args()
source = args.source.resolve()
destination = root / "build" / "rpg-instance"
if source == destination or destination in source.parents or source in destination.parents:
    parser.error("Source and test fixture must be separate directories")
if not (source / "mods").is_dir():
    parser.error("Source does not contain mods/")
destination.mkdir(parents=True, exist_ok=True)
for name in ("mods", "config", "frozenlib/downloaded_resourcepacks"):
    target = destination / name
    if target.exists():
        shutil.rmtree(target)
    if (source / name).exists():
        shutil.copytree(source / name, target)
# Include public cached resource packs so FrozenLib doesn't re-download them on every test.
# Fixture never includes saves, server lists, accounts, or launcher databases.
print(f"Prepared isolated RPG fixture: {destination}")
print(f"nix develop --command ./gradlew runClientGameTest -PrpgMods={destination / 'mods'}")
