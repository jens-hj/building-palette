#!/usr/bin/env python3
"""Run graphical Minecraft tests on an isolated display, without desktop popups/input."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

root = Path(__file__).resolve().parents[1]
xvfb = shutil.which("Xvfb")
if xvfb is None:
    candidates = sorted(Path("/nix/store").glob("*-xorg-server-*/bin/Xvfb"))
    xvfb = str(candidates[-1]) if candidates else None
if not xvfb:
    raise SystemExit("Xvfb is required; run inside nix develop")
if len(sys.argv) < 2:
    raise SystemExit("Usage: headless-test.py ./gradlew runClientGameTest [options]")
started = time.time()
with (root / "build" / "xvfb.log").open("w") as log:
    display = subprocess.Popen([xvfb, "-displayfd", "1", "-screen", "0", "1280x720x24", "-nolisten", "tcp"],
                               stdout=subprocess.PIPE, stderr=log, text=True)
    try:
        number = display.stdout.readline().strip()
        if not number.isdigit():
            raise SystemExit("Could not start isolated X display; see build/xvfb.log")
        environment = os.environ.copy()
        environment["DISPLAY"] = ":" + number
        environment["SDL_VIDEO_DRIVER"] = "x11"
        environment.pop("WAYLAND_DISPLAY", None)
        # Runtime tests must inherit the isolated display, even when a Gradle daemon exists.
        result = subprocess.run(sys.argv[1:], env=environment, cwd=root)
        if result.returncode:
            raise SystemExit(result.returncode)
        for name in ("building_palette_shapes.png", "building_palette_inventory.png"):
            screenshot = root / "build" / "previews" / name
            if not screenshot.exists() or screenshot.stat().st_mtime < started:
                raise SystemExit("Minecraft exited without completing the graphical test: " + name)
    finally:
        display.terminate()
        display.wait(timeout=10)
