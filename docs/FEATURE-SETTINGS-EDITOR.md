# Lunaria settings and editor guide

## Settings shell

The native Minecraft 1.8.9 settings shell is the main player-facing configuration surface. Its fixed order is:

**Home → QoL → Slayer → Dungeons → Farming → Mining → Fishing → HUD & Visuals → Misc**

Home shows a code-baked transparent pixel moon above live FPS, memory, and ping cards. The header uses the same moon. The settings window has only Fullscreen and Close controls; it does not use browser code or an external GUI framework.

Each feature keeps one established configuration owner. The catalog supplies labels, search, accordion state, and controls; it does not replace a feature's persistence schema.

## Important routes

### QoL

- **Comfort:** Quiet Block Particles, Lock Hotbar Scroll, Hurt Camera Shake, Smooth Aspect Teleport, Hide Dead Mobs, and Auto Sprint.
- **Inventory And Chat:** Item Lock And Swap, Inventory Assist, and Chat QoL.
- **Shortcut:** Wardrobe, Pets, and slash-command keybinds. Key capture uses Escape to cancel and Backspace to clear.

Item Lock & Swap protects configured slots and validates pair swaps locally. It writes its current data to the Lunaria target and only reads the prior target when current data is absent.

### Dungeons

- **Run Tools:** Dungeon Map, player heads, terminal assistance, Spirit Leap, and boss/readout support.
- **Dungeon HUD:** direct-canvas editor, Room Clear Alert, and Auto Chest Close.
- **Floor VII / ESP:** retained dungeon overlays and local puzzle support.

The direct-canvas Dungeon HUD Editor lets a player select, drag, resize, reset, and preview its dungeon elements. It is intentionally separate from the ordinary HUD editor.

`/luna dungeon debug` reports local dungeon detection and raw sidebar/tab evidence for the retained parser.

Auto Chest Close is opt-in and narrowly accepts only exact normal chest titles in a confirmed dungeon. It does not click reward slots, accept rewards, or send movement commands.

### HUD & Visuals

- **HUD Editor:** ordinary movable HUD elements.
- **Item Animation:** visual first-person size, offsets, hit/slash shape, speed, smooth swing, and preview controls.

Item Animation is client-side visual behavior. Its effective settings preserve compatible old inputs but the current Item Animation keys are canonical when saving.

### Misc → Fonts and Chat Editor

The **Fonts** card deliberately retains optional custom fonts:

- Toggle the Lunaria font system on or off.
- Cycle bundled choices and discovered external fonts.
- Set a bounded 70–140% scale.
- Reload the selected font without moving files.
- Use **Lunaria Text Only** to preserve vanilla Minecraft text.

Put new `.ttf` or `.otf` files in `config/lunaria/fonts`. Files in the prior fallback folder remain readable; the current folder wins when an exact file name exists in both places. No file is copied, renamed, or deleted by font discovery.

The named **Chat Editor** opens the retained detailed Chat QoL editor with background, message, tab, menu/highlight, reset, custom-tab, filter, and send-prefix controls. `/luna chateditor` opens the same detailed editor after the chat screen closes.

## Command routes

`/luna` opens the menu and provides the feature commands.

## Safety boundary

Lunaria visual and quality-of-life controls are intended to operate on the local client. They do not guarantee server-rule compliance.
