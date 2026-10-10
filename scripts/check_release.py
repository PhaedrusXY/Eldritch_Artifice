#!/usr/bin/env python3
"""Check the shipped jar's upgrade contract and resource/metadata consistency."""
import json
from pathlib import Path
import struct
import tomllib
import zipfile

root = Path(__file__).resolve().parents[1]
metadata = tomllib.loads((root / 'src/main/resources/META-INF/mods.toml').read_text())
version = metadata['mods'][0]['version']
jar = root / 'build/libs' / f'eldritchartifice-{version}.jar'
with zipfile.ZipFile(jar) as archive:
    assert archive.testzip() is None
    names = archive.namelist()
    shipped = tomllib.loads(archive.read('META-INF/mods.toml').decode())
    assert [m['modId'] for m in shipped['mods']] == ['eldritchartifice']
    assert shipped['mods'][0]['version'] == version
    assert not any('EyeInteraction' in n or 'UnseenEyeMod.class' in n for n in names)
    assert 'com/eldritchartifice/eye/UnseenEyeModule.class' in names
    for name in names:
        if name.endswith('.json'):
            json.loads(archive.read(name))
        if name.endswith('.class'):
            assert name.startswith('com/eldritchartifice/'), name
            assert struct.unpack('>H', archive.read(name)[6:8])[0] == 61, name
    guide = json.loads(archive.read('assets/eldritchartifice/guide/en_us.json'))
    legacy = 'eldritcheye:unseen_eye'
    for key in ['eldritchartifice:displacement', 'eldritchartifice:time_warp',
                'eldritchartifice:teleport', 'eldritchartifice:caged_singularity', legacy]:
        assert guide[key]['category'] == 'sorcery' and guide[key]['index'] == 2, key
    assert guide[legacy]['tier'] == 3
    assert guide[legacy]['related_recipes'][0]['location'] == legacy
    recipe = json.loads(archive.read('data/eldritcheye/recipes/unseen_eye.json'))
    assert legacy in json.dumps(recipe)
    assert 'assets/eldritcheye/guide/en_us.json' not in names, 'Duplicate guide path'
    mixins = json.loads(archive.read('eldritchartifice.mixins.json'))
    assert mixins['client'] == ['EyeHudMixin', 'EyeCodexSortMixin', 'ReserveManaBarMixin']
    assert not mixins.get('mixins')
    mixin_prefix = mixins['package'].replace('.', '/') + '/'
    declared_mixins = {mixin_prefix + cls + '.class' for cls in mixins['client']}
    # Mixin reserves its entire package; ordinary mod classes must never live there.
    assert {n for n in names if n.startswith(mixin_prefix) and n.endswith('.class')} == declared_mixins
    assert 'com/eldritchartifice/EldritchArtifice.class' not in declared_mixins
    assert 'MixinConfigs: eldritchartifice.mixins.json' in archive.read('META-INF/MANIFEST.MF').decode()
    assert archive.read('com/eldritchartifice/eye/EyeWheel.class').find(b'setVisible') >= 0
print(f'PASS: {jar.name}: single mod, legacy spell resources, guide entries, client mixins, Java 17')
