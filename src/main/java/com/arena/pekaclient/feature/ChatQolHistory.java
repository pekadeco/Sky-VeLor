package com.arena.pekaclient.feature;

/** Fixed-size allocation-light history shared by tabs, separate chat windows, unread dots and /velor chatdebug. */
public final class ChatQolHistory {

    public static final int CAPACITY = 200;

    public static final class Message {
        public String formatted = "";
        /** Single-line draw text prepared on receipt so the render loop does not repeatedly replace line breaks. */
        public String display = "";
        public String clean = "";
        public int kind = ChatFilterRules.SYSTEM;
        /** One-pass classifier explanation and unique destination retained for /velor chatdebug. */
        public String routingReason = "";
        public int destination = ChatQolMode.CHAT_2;
        public long receivedAt;
        /** Original component is held for shared copy, hover and left-click forwarding. */
        public Object component;
        /** Cached wrapped component rows for Chat 1/Chat 2; invalidated on receipt and width/style changes. */
        public final Object[] wrappedLines = new Object[2];
        public final int[] wrappedWidth = {-1, -1};
        public final int[] wrappedScale = {-1, -1};
    }

    private final Message[] messages = new Message[CAPACITY];
    private int next;
    private int size;
    private boolean unreadParty;
    private boolean unreadReply;

    public ChatQolHistory() {
        for (int i = 0; i < messages.length; i++) messages[i] = new Message();
    }

    /** Legacy/test entry point; production uses the overload retaining the one-pass routing evidence. */
    public void add(String formatted, int kind, Object component, long receivedAt, int activeTab) {
        add(formatted, kind, component, receivedAt, activeTab, "", ChatFilterRules.belongsToChatOne(kind)
                ? ChatQolMode.CHAT_1 : ChatQolMode.CHAT_2);
    }

    /** Records one received message into a preallocated slot. */
    public void add(String formatted, int kind, Object component, long receivedAt, int activeTab,
                    String reason, int destination) {
        Message message = messages[next];
        message.formatted = formatted == null ? "" : formatted;
        message.display = message.formatted.indexOf('\n') >= 0 ? message.formatted.replace('\n', ' ') : message.formatted;
        message.clean = ChatFilterRules.stripFormatting(message.formatted);
        message.kind = kind;
        message.routingReason = reason == null ? "" : reason;
        message.destination = destination == ChatQolMode.CHAT_1 ? ChatQolMode.CHAT_1 : ChatQolMode.CHAT_2;
        message.component = component;
        message.wrappedLines[0] = null;
        message.wrappedLines[1] = null;
        message.wrappedWidth[0] = message.wrappedWidth[1] = -1;
        message.wrappedScale[0] = message.wrappedScale[1] = -1;
        message.receivedAt = receivedAt;
        next = (next + 1) % messages.length;
        if (size < messages.length) size++;
        if (kind == ChatFilterRules.PARTY && activeTab != ChatFilterRules.PARTY) unreadParty = true;
        if (kind == ChatFilterRules.REPLY && activeTab != ChatFilterRules.REPLY) unreadReply = true;
    }

    /** newest(0) is the latest entry; null means the requested slot does not exist. */
    public Message newest(int offset) {
        if (offset < 0 || offset >= size) return null;
        int index = next - 1 - offset;
        if (index < 0) index += messages.length;
        return messages[index];
    }

    public int size() {
        return size;
    }

    public boolean unread(int tab) {
        return tab == ChatFilterRules.PARTY ? unreadParty : tab == ChatFilterRules.REPLY && unreadReply;
    }

    public void markRead(int tab) {
        if (tab == ChatFilterRules.PARTY) unreadParty = false;
        if (tab == ChatFilterRules.REPLY) unreadReply = false;
    }

    public void clear() {
        size = 0;
        next = 0;
        unreadParty = false;
        unreadReply = false;
    }
}
