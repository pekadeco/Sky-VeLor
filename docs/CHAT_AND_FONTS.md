# Chat, fonts, and global scrolling

## Chat ownership and routing

`ChatChannelTabs` is the Forge runtime for Chat QoL. It has one deliberately strict ownership rule:

- With **Chat QoL** or **Separate Chats** off, Minecraft's Default Chat remains the sole renderer, input owner,
  and history owner. Sky VeLor does not replace, reroute, backfill, or duplicate its messages.
- With both options on and the required safety bridge available, Sky VeLor cancels the Default Chat draw and owns
  only **new** messages in native **Chat 1** and **Chat 2** windows. Pre-toggle Default Chat history stays in the
  vanilla history and is never copied into a separate window.
- Turning Separate Chats off immediately restores vanilla Default Chat and clears the opt-in mirror. A failed
  reflection bridge also fails closed to vanilla instead of partially replacing a chat surface.

Each opt-in received message is classified once by the editable, compiled `ChatFilterRules`, then passed to
`ChatQolMode.destination`. Player, party, reply, and private traffic goes only to Chat 1; system, command,
server-result, and unknown traffic goes only to Chat 2. The destination decision is exclusive, so a message is
not duplicated between windows, and unknown text safely defaults to Chat 2.

```text
ClientChatReceivedEvent (both opt-ins on)
  -> ChatFilterRules.classify
  -> ChatQolMode.destination
     -> exactly one: Chat 1 or Chat 2
  -> bounded ChatQolHistory + matching ChatPipeline feed
RenderGameOverlayEvent.Chat
  -> native Chat 1 / Chat 2 windows
  -> one shared layout call for wrapping, hit bounds, hover, clipping, and scrolling
```

The one persisted `ChatQolSettings.isSeparateChats()` value is read by the GUI, routing point, and renderer.
`/velor chatdebug` prints the stored setting and those three live reader snapshots, then the last 20 routed
messages with their window, detected kind, and matching rule evidence.

## Controls, grouping, and copy

`ChatPipeline.Block` retains one message id across all of its wrapped rows. Hover highlighting and context-menu
selection operate on that block, not an individual visible row, so multi-line messages—including long unbroken
words, formatting boundaries, and adjacent messages from the same sender—remain distinct. Layout is rebuilt when
width, scroll, wrap, or retained history changes.

The menu-opening preference is in **Chat Editor → Hover / menu**:

- **Both** is the default; either button opens the menu on ordinary message text.
- **Right click** allows only right click.
- **Left click** opens on non-clickable message text while a `ClickEvent` component keeps its normal left-click
  behavior.

Right click always opens the menu. Outside click and Escape safely dismiss it. Its labels remain exactly:

1. `Copy to clipboard`
2. `Copy to chat box`

Clipboard text is clean/unformatted. `Copy to chat box` uses the production `ChatInputInsertion` path to preserve
an open focused chat field, insert at the cursor (or append), and clamp its contents to 256 characters. A copied
confirmation is shown only after the clipboard fallback chain succeeds.

The shared menu/hover transition uses an elapsed-time ease-out with a configurable duration; the default target is
100 ms and the editor allows it to be set to **Off**.

## Debug diagnostics

`/velor layoutdebug` toggles an in-memory diagnostic overlay. It draws thin red bounds for Sky VeLor labels, rows,
and controls plus visible vanilla buttons when a mapping can be found through guarded reflection. Intersecting
bounds are highlighted bright red and overlap pairs are sent to chat. The reflection lookup is optional and
fail-closed, so a mapping mismatch cannot affect normal GUI operation.

`/velor chatdebug` toggles message-group outlines and prints routing evidence. In Separate Chats mode it outlines
Chat 1 and Chat 2 grouped blocks. With Separate Chats off it may outline the native Default Chat's contiguous
wrapped groups only while the chat screen is open; this is diagnostic-only and does not select a replacement
Default Chat renderer.

## Mod Settings

**Mod Settings** owns one global scroll speed (50%–300%) and one global Smooth Scrolling choice. Shift + wheel
uses fine scrolling. Exactly one hovered viewport consumes each wheel event: no editor, chat window, or GUI shell
applies a second wheel delta. Chat Editor links to Mod Settings instead of duplicating these settings.

## Fonts

**Fonts** is a top-level hub entry, independent of Chat QoL. `Minecraft Default` leaves the native `FontRenderer`
untouched. Smooth, Doodle, Rounded, and Mono use Sky VeLor’s lazy private glyph atlas on its `UIRender` paths;
bad resources, headless AWT, texture allocation failure, and invalid external names all fall back to Minecraft’s
renderer and show a single notice.

External fonts belong in `config/skyvelor/fonts/`; only a plain basename ending in `.ttf` or `.otf` is accepted.
The optional **Apply to everything** request is deliberately fail-closed for Minecraft-owned text on Forge 1.8.9:
Sky VeLor-owned renderer paths apply the selected face, while vanilla text remains intact with an explicit notice.

Bundled OFL licenses and attribution are packaged under `assets/skyvelor/fonts/` and documented in
`THIRD_PARTY_LICENSES.md` and `THIRD_PARTY_NOTICES.md`.
