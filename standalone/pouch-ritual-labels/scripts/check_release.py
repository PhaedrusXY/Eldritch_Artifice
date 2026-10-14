#!/usr/bin/env python3
"""Verify the standalone Forge jar contains the correct mods.toml, mixins, and pouch code."""
from pathlib import Path
from zipfile import ZipFile
import json

build = Path(__file__).resolve().parents[1] / 'build' / 'libs'
jars = list(build.glob('pouch-ritual-labels-0.1.0-alpha.jar'))
assert len(jars) == 1, f'Expected standalone release JAR in {build}; found {jars}'
with ZipFile(jars[0]) as archive:
    names = set(archive.namelist())
    required = {
        'com/pouchritual/labels/PouchRitualLabels.class',
        'com/pouchritual/labels/client/PouchRenameScreen.class',
        'com/pouchritual/labels/client/RitualPouchClient.class',
        'com/pouchritual/labels/pouch/PouchLabels.class',
        'com/pouchritual/labels/pouch/RitualPouchNetwork.class',
        'com/pouchritual/labels/mixin/PouchSelectedNameMixin.class',
        'com/pouchritual/labels/mixin/RitualPouchScreenAccessor.class',
        'META-INF/mods.toml',
        'pouchlabels.mixins.json',
    }
    assert required <= names, sorted(required - names)
    assert not any(n.startswith('com/eldritchartifice/') for n in names)
    mods = archive.read('META-INF/mods.toml').decode()
    assert 'modId="pouchlabels"' in mods and 'modId="mna"' in mods
    assert 'modId="dimdoors"' not in mods
    mixins = json.loads(archive.read('pouchlabels.mixins.json'))
    assert mixins['mixins'] == ['PouchSelectedNameMixin']
    assert mixins['client'] == ['RitualPouchScreenAccessor']
    manifest = archive.read('META-INF/MANIFEST.MF').decode()
    assert 'MixinConfigs: pouchlabels.mixins.json' in manifest
print('Standalone mod packaged correctly:', jars[0])
