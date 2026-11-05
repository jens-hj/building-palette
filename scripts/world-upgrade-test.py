#!/usr/bin/env python3
"""Boot released jars against real saved worlds. Touches only build/migration/."""
from pathlib import Path
import datetime
import hashlib
import json
import os
import shutil
import subprocess
import sys
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'build/migration'
RUN = BASE / ('run-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
VERSIONS = ('0.1.0-alpha.10', '0.1.0-alpha.11', '0.1.0-alpha.12')
NEW = dict(line.split('=', 1) for line in (ROOT / 'gradle.properties').read_text().splitlines() if '=' in line)['version']

def digest_tree(path):
    return {str(p.relative_to(path)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted(path.rglob('*')) if p.is_file()}

def launch(directory, version, phase):
    directory.mkdir(parents=True, exist_ok=True)
    (directory / 'eula.txt').write_text('eula=true\n')
    (directory / 'server.properties').write_text(
        'online-mode=false\nserver-ip=127.0.0.1\nserver-port=0\n'
        'level-type=minecraft:flat\ngenerate-structures=false\nlevel-seed=0\n'
        'view-distance=2\nsimulation-distance=2\nspawn-protection=0\n'
        'pause-when-empty-seconds=-1\nsync-chunk-writes=true\nmax-tick-time=120000\n')
    jar = BASE / 'artifacts' / f'building-palette-{version}.jar'
    assert jar.is_file(), f'Missing released artifact: {jar}'
    command = ['java', '-Xmx2G', '-Dfabric.development=true',
        '-Dfabric.addMods=' + str(jar) + os.pathsep + str(BASE / 'world-upgrade-test.jar'),
        '-Dpalette.upgrade.version=' + version, '-Dpalette.upgrade.phase=' + phase,
        '-cp', (BASE / 'classpath.txt').read_text(),
        'net.fabricmc.loader.impl.launch.knot.KnotServer', '--nogui', '--world', 'world']
    marker = directory / 'upgrade-result.json'
    marker.unlink(missing_ok=True)
    log = directory / (phase + '.log')
    print(f'Running {phase} with {version}: {directory}', flush=True)
    with log.open('w') as output:
        result = subprocess.run(command, cwd=directory, stdout=output, stderr=subprocess.STDOUT, timeout=240)
    if result.returncode or not marker.is_file() or not json.loads(marker.read_text())['passed']:
        print('\n'.join(log.read_text().splitlines()[-70:]), file=sys.stderr)
        raise SystemExit(f'Upgrade test failed; inspect {log}')
    report = json.loads(marker.read_text())
    assert report['phase'] == phase and report['version'] == version
    print('Passed: ' + json.dumps(report), flush=True)
    return report

BASE.mkdir(parents=True, exist_ok=True)
manifest = json.loads((ROOT / 'scripts/world-upgrade-artifacts.json').read_text())
for old in VERSIONS:
    artifact = manifest[old]
    path = BASE / 'artifacts' / artifact['filename']
    if not path.is_file():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(urllib.request.urlopen(artifact['url'], timeout=60).read())
    assert hashlib.sha512(path.read_bytes()).hexdigest() == artifact['hashes']['sha512'], f'Wrong published artifact: {old}'

subprocess.run(['./gradlew', '--no-daemon', '--offline', '--no-configuration-cache',
                '-I', 'scripts/world-upgrade.gradle', 'migrationClasspath'], cwd=ROOT, check=True)
shutil.copy2(ROOT / 'build/libs' / f'building-palette-{NEW}.jar', BASE / 'artifacts' / f'building-palette-{NEW}.jar')
RUN.mkdir(parents=True)
reports = []
for version in VERSIONS:
    old = RUN / ('seed-' + version)
    reports.append(launch(old, version, 'seed'))
    baseline = digest_tree(old / 'world')
    upgraded = RUN / (version + '-to-' + NEW)
    shutil.copytree(old / 'world', upgraded / 'world')
    reports.append(launch(upgraded, NEW, 'upgrade'))
    # The next process opens the same upgraded save, including the edited old block.
    reports.append(launch(upgraded, NEW, 'reopen'))
    assert digest_tree(old / 'world') == baseline, 'Original old-version fixture changed'
report = {'target': NEW, 'runs': reports, 'original_saves_unchanged': True, 'directory': str(RUN)}
(RUN / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
(BASE / 'latest-report.json').write_text(json.dumps(report, indent=2) + '\n')
print('All saved-world upgrade/reopen checks passed: ' + str(RUN), flush=True)
