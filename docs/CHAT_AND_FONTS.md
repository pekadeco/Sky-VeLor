# Chat, fonts, and global scrolling

## One chat pipeline

All three chat targets run through the same runtime class and data model:

```text
ClientChatReceivedEvent
  -> ChatRuntime.onChat
  -> ChatPipeline.Message queues
     Default: All / Party / Reply; Chat 1: Party; Chat 2: Reply
RenderGameOverlayEvent.Chat
  -> ChatRuntime.onRender
  -> ChatPipeline.layout / hit / fade / Scroll
  -> one renderer for Default Chat, Chat 1 and Chat 2
MouseEvent
  -> ChatRuntime.onMouse (one wheel consumer)
  -> shared row hit test -> component forwarding or mouse-up copy menu
```

`ChatRuntime` is the real Forge caller, not a test adapter. It draws an entire wrapped message as one
rounded hover block spanning the chat window's full right edge. The same bounds are used for movement,
scale, scrolling, hover and component forwarding.

Right-click actions are deliberately limited to exactly:

1. `Copy to clipboard`
2. `Copy to chat box`

Clipboard text is unformatted. A success notification is only shown after the chain
`GuiScreen.setClipboardString` → AWT toolkit → LWJGL bridge passes a read-back equality check.

## Mod Settings

**Mod Settings** owns chat and GUI wheel speed independently. Both start at `2.0x`, range from
`0.25x` to `6.0x`, and use Shift for a quarter-step fine scroll. Each mouse wheel event is consumed by
exactly one hovered custom viewport. Smooth scrolling is also global, so Chat Editor does not contain
a competing scroll-speed setting.

## Fonts

**Fonts** is a top-level hub entry. `Minecraft Default` restores the original `FontRenderer` reference
exactly. Smooth, Doodle, Rounded and Mono are rasterised to an HD 16×16 atlas in a private resource
pack domain and constructed through Minecraft's ordinary `FontRenderer`; formatting codes and shadow
drawing stay in the normal renderer path. The replacement fails closed: a resource/reflection/font
failure restores Minecraft Default and gives one in-game notice.

External fonts belong in `config/skyvelor/fonts/`; only a basename ending in `.ttf` or `.otf` is
accepted. **Apply to everything** is on by default. Turning it off routes Sky VeLor-owned screens,
HUD and chat to the selected renderer while returning `Minecraft.fontRendererObj` to vanilla.

License texts and attribution are packaged with the fonts; see `THIRD_PARTY_NOTICES.md`.
