# Chat QoL and detailed Chat Editor

`ChatChannelTabs` is the retained runtime owner for **Chat QoL**. It works from Minecraft's existing chat history and falls back to native rendering when the guarded bridge is unavailable.

- With Chat QoL disabled, vanilla chat rendering, history, input, components, and scrolling remain unchanged.
- With it enabled, the local client can draw the same cached messages with a configurable message background, fade/slide arrival, compact-message counter, smooth scrolling, tab strip, hover state, and captured-message menu.
- All remains additive. Party, Reply, and custom tabs are local views; they do not reroute received traffic away from All.
- The strip is attached above the ordinary `GuiChat` field only while chat is open. It supports built-in tabs, bounded custom tabs, unread indicators, key cycling, and optional send prefixes.

## Detailed editor

Use **Misc → Fonts → Open Chat Editor** or `/lunaria chateditor` for the retained detailed editor. It exposes:

1. Background color, opacity, shape, and width behavior.
2. Message animation, fade, compact messages, and extended chat-length behavior.
3. Tab visibility, custom tab creation/editing/reordering/removal, filters, colors, sound, and prefixes.
4. Menu/highlight color, opacity, and mouse policy.
5. Per-section resets.

The inline Chat QoL route remains available in **QoL → Inventory And Chat**. Both surfaces use the same persisted Chat QoL owner.

## Copy and interaction safety

The context menu captures the selected message state once. Copy-to-chat uses the actual focused chat input and respects Minecraft's length bound. Native click components are forwarded through guarded vanilla handling rather than being replaced by a server-command path.

## Diagnostics

`/lunaria chatdebug` and `/lunaria layoutdebug` expose local visual diagnostics. They do not alter saved chat configuration.
