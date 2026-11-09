#!/usr/bin/env python3
"""Native NeoForge save/edit/reopen checks, confined to build/migration/."""
from pathlib import Path
import datetime
import hashlib
import json
import shutil
import subprocess

root = Path(__file__).resolve().parents[1]
base = root / 'build/migration' / ('neoforge-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
seed = base / 'seed'
copy = base / 'reopen'

def digest(path):
    return {str(p.relative_to(path)): hashlib.sha256(p.read_bytes()).hexdigest() for p in path.rglob('*') if p.is_file()}

def launch(directory, phase):
    directory.mkdir(parents=True,exist_ok=True)
    (directory/'eula.txt').write_text('eula=true\n')
    (directory/'server.properties').write_text('online-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\npause-when-empty-seconds=-1\n')
    marker=directory/'upgrade-result.json'
    marker.unlink(missing_ok=True)
    log=directory/(phase+'.log')
    with log.open('w') as output:
        subprocess.run(['./gradlew','--no-daemon',':neoforge:runUpgradeServer',f'-PupgradeDirectory={directory}',f'-PupgradePhase={phase}'],cwd=root,stdout=output,stderr=subprocess.STDOUT,check=True,timeout=240)
    result=json.loads(marker.read_text())
    assert result['passed'] and result['phase']==phase, result
    print(json.dumps(result),flush=True)
    return result

results=[launch(seed,'seed')]
baseline=digest(seed/'world')
shutil.copytree(seed/'world',copy/'world')
results.append(launch(copy,'upgrade'))
results.append(launch(copy,'reopen'))
assert baseline==digest(seed/'world'), 'Original fixture modified'
report={'loader':'neoforge','runs':results,'original_saves_unchanged':True,'directory':str(base)}
(base/'report.json').write_text(json.dumps(report,indent=2)+'\n')
(root/'build/migration/neoforge-report.json').write_text(json.dumps(report,indent=2)+'\n')
print('Native NeoForge save/edit/reopen passed',flush=True)
