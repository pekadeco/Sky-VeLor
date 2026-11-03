# Apple settings layout renderer

`AppleSettingsLayoutRenderer.java` is a **headless Java2D review tool**, not Minecraft runtime code.

It imports the real pure layout contracts:

- `ReferenceSettingsWindowLayout`
- `SettingsShellLayout`
- `SettingsRailLayout`
- `SettingsControlLayout`
- `ReferenceSettingsCatalog`

Run it after the mod classes have been compiled:

```sh
sh tools/run_apple_layout_renderer.sh build/apple-layout-previews
```

It creates exactly three untracked PNGs:

- `home-scale1.png`
- `dungeon-scale1.png`
- `home-scale4.png`

The renderer fails when the real geometry produces overlapping rail targets, an invalid 24 px header action, a window outside its requested margin, or anything other than the current nine settings destinations. CI uploads the three PNGs as short-lived visual review evidence; neither them nor renderer build products belong in `dist/`.
