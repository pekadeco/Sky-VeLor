# Loading-screen behavior

## Lunaria-owned startup screen

Lunaria draws its own startup screen while its queued client startup tasks run. The supplied wide illustration is drawn with an aspect-fill source crop: the image fills the available viewport without letterboxing, and excess source area is cropped from the center. This keeps the artwork usable across desktop GUI scales and narrow/tall Android/Pojav layouts.

The centered logo uses nearest-neighbour texture filtering. The panel uses a readable blue-to-purple rounded/glowing progress bar, current-step text, and a percentage derived from Lunaria's own completed startup tasks.

## Downloading-terrain screen

The vanilla Forge/Minecraft downloading-terrain lifecycle is preserved. Lunaria uses a **post-draw overlay** over the native terrain screen rather than replacing that screen class. The overlay can show the supplied artwork and an indeterminate `--%` state, because Forge/Minecraft 1.8.9 does not expose trustworthy terrain-download byte/step progress to an ordinary client mod.

It must not invent a percentage from render frames or replace the native screen in a way that risks world/join lifecycle behavior.

## Pre-mod Forge splash: precise boundary

An ordinary Forge 1.8.9 `@Mod` cannot replace the initial Forge splash screen: `SplashProgress` runs during Forge/FML bootstrap before normal mod discovery, construction, and `FMLInitializationEvent`. By the time Lunaria code is available through the normal mod lifecycle, that splash phase has already begun or finished.

A replacement is **technically possible only outside normal-mod support** by patching Forge, changing the launcher/bootstrap, or injecting a specially ordered coremod/tweaker before FML splash initialization. That approach is version-fragile, can conflict with other coremods/launchers, is not a supported Forge 1.8.9 mod API, and is not implemented in this release.

Therefore this release supports the Lunaria-owned startup screen and the native terrain post-draw overlay, but does not claim to replace the pre-mod Forge splash.
