#!/usr/bin/env python3
"""Guard player-facing resource text and Java string literals against the former product name.

The explicit path/literal allowlist covers legacy on-disk paths, the deployed endpoint, the
sync protocol domain separator and an old debug property. It cannot hide a new visible label.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
OLD = re.compile(r'sky[ -]?velor', re.I)
LITERAL = re.compile(r'"((?:\\.|[^"\\])*)"')
ALLOW = {
    'config/AppleSettings.java': {'skyvelor.properties'},
    'config/AssistSettings.java': {'wss://skyvelor-dungeon-sync.bombadzalge.workers.dev/ws'},
    'dungeon/sync/DungeonPuzzleSyncConfig.java': {'wss://skyvelor-dungeon-sync.bombadzalge.workers.dev/ws', 'skyvelor-dungeon-sync.properties'},
    'dungeon/sync/DungeonSyncIdentity.java': {r'skyvelor-dungeon-sync-v3\n'},
    'feature/LeapDiagnostics.java': {'skyvelor.debug.leap'},
    'feature/inventory/SlotLockStore.java': {'skyvelor/slot-locks.properties'},
    'feature/inventory/SlotLocking.java': {'skyvelor/slot-locks.properties'},
    'feature/protection/ItemProtection.java': {'skyvelor/protected-items.txt'},
    'feature/protection/ProtectedItemStore.java': {'skyvelor/protected-items.txt'},
    'feature/qol/EquipmentOverlay.java': {'skyvelor-equipment.dat'},
    'fishing/FishingStats.java': {'skyvelor-fishing.properties'},
    'hud/HudManager.java': {'skyvelor-hud.properties'},
    'notes/QuickNotes.java': {'skyvelor-notes.txt'},
    'render/ExternalFontLocations.java': {'skyvelor/fonts'},
    'slayer/SlayerTracker.java': {'skyvelor-slayer-prices.properties'},
}

def violations(path, text):
    return [literal for literal in LITERAL.findall(text)
            if OLD.search(literal) and literal not in ALLOW.get(path, set())]

assert violations('gui/Example.java', 'String title = "Sky VeLor";') == ['Sky VeLor']
assert violations('gui/Example.java', 'String title = "SkyVeLor";') == ['SkyVeLor']
assert violations('gui/Example.java', 'String title = "Sky-VeLor";') == ['Sky-VeLor']
java_root = ROOT / 'src/main/java/com/arena/pekaclient'
errors = []
for path in java_root.rglob('*.java'):
    relative = path.relative_to(java_root).as_posix()
    text = path.read_text(encoding='utf-8')
    errors.extend('%s: %s' % (relative, match) for match in violations(relative, text))
    for literal in LITERAL.findall(text):
        if re.search(r'/sv(?:\b|dungeon)|/velor\b', literal, re.I):
            errors.append('%s: obsolete visible command %s' % (relative, literal))
for path in (ROOT / 'src/main/resources').rglob('*'):
    if path.is_file() and path.suffix in {'.lang', '.info', '.json', '.txt'}:
        if OLD.search(path.read_text(encoding='utf-8')):
            errors.append(str(path.relative_to(ROOT)))
if errors:
    raise SystemExit('Old branding outside explicit internal allowlist:\n' + '\n'.join(errors))
print('visible_branding_test passed')
