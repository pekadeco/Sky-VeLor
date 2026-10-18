# Chat, fonts, and global scrolling

## One Default Chat implementation

`ChatChannelTabs` is the single Forge runtime for **Chat QoL**. It never creates Chat 1/Chat 2 windows, routes
received traffic, classifies messages, or writes Minecraft's chat cache.

- With **Chat QoL** off, or if a guarded reflection bridge cannot resolve, Minecraft keeps its normal Default Chat
  renderer, history, input, components, and scrolling unchanged.
- With Chat QoL on and the bridge ready, Sky VeLor cancels only the Default Chat overlay draw and redraws that same
  vanilla cache once. The opt-in surface has a rounded configurable **message-only** background, fade/short-slide
  arrivals, eased scrolling, full-group hover, and an instant captured-message context menu.
- Received messages remain in vanilla history. **All** always includes every message; Party, Reply, and custom tabs
  are view filters only, so a custom match is additive rather than routed away from All.

The attached strip is immediately above the real `GuiChat` field and is shown **only while chat is open**. It supports
All, Party, Reply, and up to five custom tabs (eight total), unread dots, Ctrl+Tab/keybind cycling, and optional
send prefixes. Tabs use a translucent blue base with white text; custom tab colours are thin accent underlines. Its
reserved message boundary keeps the strip out of message rows.

## Chat Editor

The active-font-measured **Chat Editor** intentionally contains only:

1. **Background** — colour, open/closed opacity, 0–12 px corner radius, **Fit to messages / Full area** for open
   chat, and optional **Fit width to text**.
2. **Messages** — smooth arrival toggle, animation duration, and fade duration.
3. **Tabs** — blue-base colour plus inactive/active opacity, visibility, custom add/rename/delete/reorder/recolour,
   Contains/Starts with/Regex filtering, filter test sample, send prefix, optional sound, and eight-tab cap. Invalid
   regex is shown in red and leaves the last safe filter in force.
4. **Menu and Highlight** — translucent white-capable group highlight and right/left/both instant context-menu policy.
5. **Reset** — per-section safe defaults.

Built-in filters stay valid even when hidden. Existing retired split-window, pattern, target, and editor keys load
without reviving their behavior; the next save omits them.

## Grouping, clicks, and copying

A wrapped message group retains its complete original component where Minecraft exposes it. If only wrapped rows
are available, Sky VeLor rebuilds one full clean fallback payload before showing a menu. The payload is captured at
menu-open time, so moving the pointer or relaying out chat cannot change what either action copies.

The menu opens and closes instantly. It always contains:

1. `Copy to clipboard`
2. `Copy to chat box`

A third `Message player` action appears only when the stored line has a safe 3–16 character player name. Choosing it
prepares `/msg <Name> ` in the focused chat box, retaining ordinary text or replacing the prior `/msg`, `/w`, or
`/tell` recipient segment. Clipboard text is clean/unformatted. **Copy to chat box** uses the real focused chat input,
preserves open chat, inserts at the cursor/selection, and enforces Minecraft's 256-character bound. Left-click
components with a native `ClickEvent` are forwarded through guarded vanilla handling; the menu never steals them.

## Diagnostics

`/velor layoutdebug` toggles the in-memory bounds overlay for Sky VeLor controls. It uses the same measured editor
layout and marks overlap evidence without changing saved settings.

`/velor chatdebug` toggles Default Chat group outlines and reports Chat QoL state, active implementation,
last tab-strip geometry/drawn state, tab count, group under the mouse with its detected player name, and the
immutable stored menu message.

## Mod Settings and Fonts

**Mod Settings** owns the one global scroll speed (50%–300%) and one global Smooth Scrolling choice. Shift + wheel
is fine scrolling; one hovered viewport consumes an event once.

**Fonts** remains independent of Chat QoL. `Minecraft Default` leaves the native `FontRenderer` untouched. Smooth,
Doodle, Rounded, and Mono use Sky VeLor’s lazy private glyph atlas on Sky VeLor UI paths; resource or texture
failure falls back safely to Minecraft’s renderer. Optional external fonts are limited to plain `.ttf`/`.otf`
basenames in `config/skyvelor/fonts/`.

Bundled OFL licenses and attribution are packaged under `assets/skyvelor/fonts/` and documented in
`THIRD_PARTY_LICENSES.md` and `THIRD_PARTY_NOTICES.md`.
