# Chat, fonts, and global scrolling

## One chat pipeline

`ChatChannelTabs` is the registered Forge runtime. Its Default Chat, Chat 1, and Chat 2 render/input paths share
`ChatPipeline` for measured wrapped rows, full-row bounds, scrolling, hover selection, clipping, and scrollbar
geometry:

```text
ClientChatReceivedEvent
  -> ChatChannelTabs.onChat
  -> one classified PipelineEntry feed
     -> Chat 1: player / party / reply
     -> Chat 2: system
     -> Default Chat: component-preserving native rendered-line cache
RenderGameOverlayEvent.Chat
  -> ChatChannelTabs.renderCustomDefault / renderSeparate
  -> ChatPipeline.layout -> scissored rows -> same bounds for hover and hit
GuiScreenEvent.MouseInputEvent.Pre
  -> exactly one hovered ChatPipeline scroll owner per wheel event
  -> native component forwarding on left press
  -> right-release menu, then matching left press/release action
```

Default Chat uses vanilla's component-preserving wrapped cache as its source, so it does not lose Minecraft click
or hover styles. Chat 1/Chat 2 use the same layout call with their classified feeds. If the required cache mapping
cannot be resolved, the default path fails closed to unmodified vanilla chat.

Right-click actions remain exactly:

1. `Copy to clipboard`
2. `Copy to chat box`

The choice is armed on press and executes only on a matching mouse-up. Clipboard text is unformatted. A success
message is only displayed after the fallback chain `GuiScreen.setClipboardString` → AWT toolkit → optional LWJGL
bridge passes a read-back equality check; failures show an on-screen toast instead of claiming success.

## Mod Settings

**Mod Settings** is the top-level owner of one global scroll speed (50%–300%) and one global Smooth Scrolling
choice. Shift + wheel uses fine scrolling. The active viewport consumes each real wheel event once: no editor,
chat window, or GUI shell applies a second wheel delta. Chat Editor links to Mod Settings instead of duplicating
these settings.

## Fonts

**Fonts** is a top-level hub entry, independent of Chat QoL. `Minecraft Default` leaves the native
`FontRenderer` untouched. Smooth, Doodle, Rounded, and Mono use Sky VeLor’s lazy private glyph atlas on its
`UIRender` paths; bad resources, headless AWT, texture allocation failure, and invalid external names all fall
back to Minecraft’s renderer and show a single notice.

External fonts belong in `config/skyvelor/fonts/`; only a plain basename ending in `.ttf` or `.otf` is accepted.
The optional **Apply to everything** request is deliberately fail-closed for Minecraft-owned text on Forge 1.8.9:
Sky VeLor-owned renderer paths apply the selected face, while vanilla text remains intact with an explicit notice.

Bundled OFL licenses and attribution are packaged under `assets/skyvelor/fonts/` and documented in
`THIRD_PARTY_LICENSES.md` and `THIRD_PARTY_NOTICES.md`.
