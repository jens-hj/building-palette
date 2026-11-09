#!/usr/bin/env python3
"""Prepare both loader releases locally; no network or credential access."""
from pathlib import Path
import hashlib
import json
import shutil
import tomllib
import zipfile

root = Path(__file__).resolve().parents[1]
properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line)
version = properties['version']
destination = root / 'build/release'
destination.mkdir(parents=True, exist_ok=True)
hashes = []
for loader, directory, companions in [
    ('fabric', root / 'build/libs', ('Chc4Pi2b', 'IBTjtUTI')),
    ('neoforge', root / 'neoforge/build/libs', ('aP8yK8nm', 'Dh9cO38a')),
]:
    artifact = directory / f'building-palette-{version}-{loader}.jar'
    with zipfile.ZipFile(artifact) as jar:
        paths = jar.namelist()
        metadata = (json.loads(jar.read('fabric.mod.json')) if loader == 'fabric'
                    else tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode())['mods'][0])
        if metadata['version'] != version:
            raise SystemExit('Jar version mismatch; rebuild first')
        if any('/test/' in p or '/harness/' in p or p.startswith('dev/tazer/') for p in paths):
            raise SystemExit('Test code or dependency implementation in release jar')
        other = 'neoforge' if loader == 'fabric' else 'fabric'
        if any(p.startswith(f'dev/jensh/buildingpalette/{other}/') for p in paths):
            raise SystemExit('Wrong platform adapter in release jar')
    for suffix in ('', '-sources'):
        source = directory / f'building-palette-{version}-{loader}{suffix}.jar'
        target = destination / source.name
        shutil.copy2(source, target)
        hashes.append(f'{hashlib.sha256(target.read_bytes()).hexdigest()}  {target.name}')
    dependencies = [
        {'project_id': 'ewyjbmG4', 'version_id': companions[0], 'dependency_type': 'required'},
        {'project_id': 'vvgTqF0n', 'version_id': companions[1], 'dependency_type': 'required'},
        {'project_id': 'rlloIFEV', 'dependency_type': 'optional'},
    ]
    if loader == 'fabric':
        dependencies += [{'project_id': 'P7dR8mSH', 'dependency_type': 'required'},
                         {'project_id': 'mOgUt4GM', 'dependency_type': 'optional'}]
    (destination / f'metadata-{loader}.json').write_text(json.dumps({
        'name': f'Building Palette {version} ({loader.title()})',
        'version': f'{version}+{loader}', 'version_type': 'alpha',
        'game_versions': ['26.3'], 'loaders': [loader],
        'client_side': 'required', 'server_side': 'required', 'dependencies': dependencies,
    }, indent=2) + '\n')
(destination / 'SHA256SUMS.txt').write_text('\n'.join(hashes) + '\n')
shutil.copy2(root / 'docs/MODRINTH.md', destination / 'MODRINTH.md')
project = json.loads((root / 'docs/PROJECT.json').read_text())
project.update(body_file='MODRINTH.md', icon_file='icon.gif')
shutil.copy2(root / 'docs/assets/icon.gif', destination / 'icon.gif')
(destination / 'gallery').mkdir(exist_ok=True)
for entry in project['gallery']:
    source = root / entry['file']
    shutil.copy2(source, destination / 'gallery' / source.name)
    entry['file'] = 'gallery/' + source.name
(destination / 'project.json').write_text(json.dumps(project, indent=2) + '\n')
print(f'Prepared Fabric and NeoForge releases: {destination}')
