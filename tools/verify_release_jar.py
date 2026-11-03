#!/usr/bin/env python3
"""Verify a packaged The Sky's Lunaria Mod Forge 1.8.9 release JAR.

This verifier intentionally checks release identity, safety-critical feature surfaces, packaged assets, and archive
hygiene without treating internal Java package names as public branding. It is standard-library-only so CI can run it
immediately after ForgeGradle reobfuscation.
"""
from __future__ import print_function

import argparse
import os
import struct
import sys
import zipfile


class Results(object):
    def __init__(self):
        self.passed = 0
        self.failed = 0

    def check(self, condition, message):
        if condition:
            self.passed += 1
            print("  ok   " + message)
        else:
            self.failed += 1
            print("  FAIL " + message)


def manifest_text(raw):
    """Unfold JAR manifest continuation lines before simple attribute matching."""
    text = raw.decode("utf-8", "replace").replace("\r\n", "\n")
    lines = []
    for line in text.split("\n"):
        if line.startswith(" ") and lines:
            lines[-1] += line[1:]
        else:
            lines.append(line)
    return "\n".join(lines)


def jpeg_size(raw):
    """Return JPEG width/height from an SOF marker, or None for malformed input."""
    if len(raw) < 4 or raw[:2] != b"\xff\xd8":
        return None
    index = 2
    while index + 9 <= len(raw):
        if raw[index] != 0xff:
            index += 1
            continue
        while index < len(raw) and raw[index] == 0xff:
            index += 1
        if index >= len(raw):
            return None
        marker = raw[index]
        index += 1
        if marker in (0xd8, 0xd9) or 0xd0 <= marker <= 0xd7:
            continue
        if index + 2 > len(raw):
            return None
        length = struct.unpack(">H", raw[index:index + 2])[0]
        if length < 2 or index + length > len(raw):
            return None
        if marker in (0xc0, 0xc1, 0xc2, 0xc3, 0xc5, 0xc6, 0xc7, 0xc9, 0xca, 0xcb, 0xcd, 0xce, 0xcf):
            if length < 8:
                return None
            height, width = struct.unpack(">HH", raw[index + 3:index + 7])
            return width, height
        index += length
    return None


def png_size(raw):
    if len(raw) < 24 or raw[:8] != b"\x89PNG\r\n\x1a\n" or raw[12:16] != b"IHDR":
        return None
    return struct.unpack(">II", raw[16:24])


def expected_numeric(version):
    value = version
    if value.startswith("Beta-"):
        value = value[5:]
    if value.endswith("v"):
        value = value[:-1]
    return value


def contains_all(raw, values):
    return all(value in raw for value in values)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", help="reobfuscated release JAR")
    parser.add_argument("--version", required=True, help="display version, e.g. Beta-0.10.9.4v")
    parser.add_argument("--mcmod-version", default=None, help="numeric Forge metadata version")
    args = parser.parse_args()

    result = Results()
    expected_name = "Lunaria-Mod-1.8.9-" + args.version + ".jar"
    result.check(os.path.basename(args.jar) == expected_name,
                 "artifact name is " + expected_name)
    result.check(os.path.isfile(args.jar), "artifact exists")
    if not os.path.isfile(args.jar):
        print("JAR verification: passed {0}, failed {1}".format(result.passed, result.failed))
        return 1

    try:
        archive = zipfile.ZipFile(args.jar, "r")
    except (IOError, OSError, zipfile.BadZipfile) as error:
        result.check(False, "JAR is readable: " + str(error))
        print("JAR verification: passed {0}, failed {1}".format(result.passed, result.failed))
        return 1

    try:
        names = archive.namelist()
        name_set = set(names)
        result.check(bool(names), "archive has entries")
        result.check(len(names) == len(name_set), "archive has no duplicate paths")
        result.check(not any(name.startswith("/") or ".." in name.split("/") for name in names),
                     "archive paths are relative and traversal-free")
        result.check(not any(name.endswith(".java") or name.startswith("build/") or name.startswith("dist/")
                             for name in names),
                     "release JAR contains no source/build/delivery files")
        mod_classes = [name for name in names if name.startswith("com/arena/pekaclient/") and name.endswith(".class")]
        result.check(len(mod_classes) >= 120,
                     "contains a substantial compiled Lunaria implementation ({0} classes)".format(len(mod_classes)))

        required = (
            "com/arena/pekaclient/PekaClientMod.class",
            "com/arena/pekaclient/PekaVersion.class",
            "com/arena/pekaclient/command/SvCommand.class",
            "com/arena/pekaclient/command/SvDungeonCommand.class",
            "com/arena/pekaclient/command/ItemDebugCommand.class",
            "com/arena/pekaclient/itemlockswap/ItemLockSwapController.class",
            "com/arena/pekaclient/dungeon/DungeonMapOverlay.class",
            "com/arena/pekaclient/gui/DungeonHudEditorScreen.class",
            "com/arena/pekaclient/assist/dungeon/DungeonScoreTracker.class",
            "com/arena/pekaclient/assist/dungeon/RoomClearAlertController.class",
            "com/arena/pekaclient/assist/dungeon/DungeonChestCloser.class",
            "com/arena/pekaclient/assist/visual/DeadMobRenderFilter.class",
            "com/arena/pekaclient/assist/visual/AspectTeleportTransitionRenderer.class",
            "com/arena/pekaclient/dungeon/DungeonProgressTracker.class",
            "com/arena/pekaclient/dungeon/solver/terminals/TerminalGuiLayer.class",
            "com/arena/pekaclient/render/GlobalFontManager.class",
            "com/arena/pekaclient/gui/LunariaArtwork.class",
            "com/arena/pekaclient/gui/LunariaDownloadingTerrainScreen.class",
            "mixins.lunaria.json",
            "mcmod.info",
            "assets/lunaria/lang/en_US.lang",
            "assets/lunaria/dungeons/rooms.json",
            "assets/lunaria/textures/gui/lunaria_loading_background.jpg",
            "assets/lunaria/textures/gui/lunaria_logo.png",
            "assets/lunaria/textures/gui/lunaria_moon.png",
        )
        missing = [name for name in required if name not in name_set]
        result.check(not missing, "required Lunaria runtime classes and resources are packaged"
                     + ("" if not missing else ": " + ", ".join(missing)))
        result.check(not any(name.startswith("assets/pekaclient/") or name.startswith("assets/skyvelor/")
                             for name in names),
                     "only the Lunaria packaged asset namespace is present")

        manifest = manifest_text(archive.read("META-INF/MANIFEST.MF")) if "META-INF/MANIFEST.MF" in name_set else ""
        result.check("Implementation-Title: The Sky's Lunaria Mod" in manifest,
                     "manifest identifies The Sky's Lunaria Mod")
        result.check("Implementation-Version: " + args.version in manifest,
                     "manifest version matches " + args.version)
        result.check("FMLCorePlugin: com.arena.pekaclient.render.itemanimation.ItemAnimationLoadingPlugin" in manifest,
                     "manifest wires the narrow visual/item-lock core hook")
        result.check("MixinConfigs: mixins.lunaria.json" in manifest,
                     "manifest wires the Lunaria Mixin configuration")

        numeric = args.mcmod_version or expected_numeric(args.version)
        metadata = archive.read("mcmod.info").decode("utf-8", "replace") if "mcmod.info" in name_set else ""
        result.check('"modid": "lunaria"' in metadata and '"name": "The Sky\'s Lunaria Mod"' in metadata,
                     "mcmod metadata uses Lunaria public identity")
        result.check('"version": "' + numeric + '"' in metadata,
                     "processed mcmod metadata carries numeric version " + numeric)
        # mcmod.info declares the Minecraft compatibility version; build.gradle pins the Forge coordinate.
        result.check('"mcversion": "1.8.9"' in metadata,
                     "mcmod metadata targets Minecraft 1.8.9")

        version_class = archive.read("com/arena/pekaclient/PekaVersion.class") if "com/arena/pekaclient/PekaVersion.class" in name_set else b""
        entry_class = archive.read("com/arena/pekaclient/PekaClientMod.class") if "com/arena/pekaclient/PekaClientMod.class" in name_set else b""
        command_class = archive.read("com/arena/pekaclient/command/SvCommand.class") if "com/arena/pekaclient/command/SvCommand.class" in name_set else b""
        dungeon_command = archive.read("com/arena/pekaclient/command/SvDungeonCommand.class") if "com/arena/pekaclient/command/SvDungeonCommand.class" in name_set else b""
        result.check(contains_all(version_class, (args.version.encode("utf-8"), b"The Sky's Lunaria Mod")),
                     "compiled version constants retain the public title and version")
        result.check(contains_all(entry_class, (b"lunaria", b"LunariaDownloadingTerrainScreen", b"DungeonChestCloser")),
                     "entry point wires Lunaria metadata, terrain overlay, and retained chest feature")
        result.check(contains_all(command_class, (b"lunaria", b"sv", b"velor", b"dungeon")),
                     "main command packages /lunaria with /sv and /velor aliases")
        result.check(contains_all(dungeon_command, (b"debug", b"svdungeon", b"Dungeon debug begin", b"raw sidebar")),
                     "dungeon command retains live diagnostics and legacy direct alias support")

        mixins = archive.read("mixins.lunaria.json") if "mixins.lunaria.json" in name_set else b""
        result.check(contains_all(mixins, (b"com.arena.pekaclient.mixin", b"MixinGuiContainer", b"MixinEntityRenderer")),
                     "Mixin declaration retains container and visual compatibility hooks")

        loading = archive.read("assets/lunaria/textures/gui/lunaria_loading_background.jpg") \
            if "assets/lunaria/textures/gui/lunaria_loading_background.jpg" in name_set else b""
        logo = archive.read("assets/lunaria/textures/gui/lunaria_logo.png") \
            if "assets/lunaria/textures/gui/lunaria_logo.png" in name_set else b""
        moon = archive.read("assets/lunaria/textures/gui/lunaria_moon.png") \
            if "assets/lunaria/textures/gui/lunaria_moon.png" in name_set else b""
        result.check(jpeg_size(loading) == (1408, 768), "supplied loading illustration is packaged at 1408 by 768")
        result.check(png_size(logo) == (744, 744), "supplied transparent logo is packaged at 744 by 744")
        result.check(png_size(moon) == (360, 289), "logo-derived moon header asset is packaged at 360 by 289")

        artwork = archive.read("com/arena/pekaclient/gui/LunariaArtwork.class") \
            if "com/arena/pekaclient/gui/LunariaArtwork.class" in name_set else b""
        terrain = archive.read("com/arena/pekaclient/gui/LunariaDownloadingTerrainScreen.class") \
            if "com/arena/pekaclient/gui/LunariaDownloadingTerrainScreen.class" in name_set else b""
        # ForgeGradle remaps Gui draw-method names in reobfuscated bytecode. The source archive verifier checks the
        # cover-fit call itself; this packaged check relies on stable resource identifiers instead.
        result.check(contains_all(artwork, (b"lunaria_loading_background", b"lunaria_logo", b"lunaria_moon")),
                     "Lunaria artwork renderer links the supplied loading, logo, and moon assets")
        result.check(contains_all(terrain, (b"Downloading terrain", b"Terrain progress is not exposed", b"--%")),
                     "terrain overlay truthfully reports indeterminate Forge terrain progress")
    except (KeyError, IOError, OSError, zipfile.BadZipfile) as error:
        result.check(False, "required archive data is readable: " + str(error))
    finally:
        archive.close()

    print("JAR verification: passed {0}, failed {1}".format(result.passed, result.failed))
    return 1 if result.failed else 0


if __name__ == "__main__":
    sys.exit(main())
