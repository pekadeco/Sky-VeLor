package com.arena.pekaclient.gui;

import com.arena.pekaclient.feature.LeapClass;
import com.arena.pekaclient.feature.LeapName;
import com.arena.pekaclient.feature.LeapText;
import com.arena.pekaclient.feature.SpiritLeap;
import com.arena.pekaclient.render.SkinHead;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Spirit Leap teammate picker. It replaces only the server-opened leap chest, and it is a plain flat screen:
 * a dark translucent panel, a grid of rectangular cards two columns wide, and a square player head on the left of
 * each card with the name beside it and the class label beneath. Four teammates therefore make two rows.
 *
 * Every click is the server's own window click on the teammate's slot, the same as a left click in the vanilla
 * menu. Sky VeLor sends no packet and no command of its own. After a pick the screen waits for the server, so a
 * double click or a held Enter cannot send a second leap.
 *
 * The same screen also serves the preview from the hub's Dungeon page
 * ({@link #preview(GuiScreen)}). In preview mode there is no container, the cards come from
 * {@link LeapPreviewData}, nothing can be clicked through to the server, and the panel is labelled as test data.
 * The drawing code is shared, so the preview shows the real screen rather than a mock-up.
 *
 * Classes: a teammate's class is read from the text the client already has, in this order - the leap item's own
 * display name, the scoreboard team prefix/suffix, the sidebar scoreboard lines, the tab-list entry - and the
 * item's "Class:" lore line last. See {@link LeapName}. A class that cannot be read stays unknown; it is never
 * guessed from a name and one player's class is never given to another.
 *
 * Heads: the skin is requested with {@code getLocationSkin()}, which starts the download. The real skin is drawn
 * only once {@code hasLocationSkin()} reports it has arrived. While it loads, a dark placeholder with a pulse is
 * drawn. The first letter is drawn only when the player is not in the tab list or the skin never arrived.
 */
public final class LeapScreen extends GuiScreen {

    private static final long PENDING_NS = 1500000000L;
    /** How long a skin may take to arrive before the head falls back to a letter. Loading keeps going. */
    private static final long SKIN_WAIT_NS = 12000000000L;

    private static final int CARD_H = 46;
    private static final int HEAD = 32;
    private static final int GAP = LeapLayout.GAP;
    private static final int PAD = LeapLayout.PAD;
    private static final int TITLE_H = 24;
    private static final int FOOT_H = 20;
    private static final int EMPTY_H = 40;

    private static final int PANEL = 0xD0000000;
    private static final int PANEL_EDGE = 0xFF3A3A3A;
    private static final int CARD_FILL = 0xB0101010;
    private static final int CARD_EDGE = 0xFF2E2E2E;
    private static final int CARD_LIT = 0x40FFFFFF;
    private static final int CARD_SELECT_EDGE = 0xFFE8E8E8;
    private static final int HEAD_PLACEHOLDER = 0xFF1B2230;
    private static final int HEAD_LETTER = 0xFF2C3E5C;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0A0;
    private static final int TEST_BANNER = 0xFFFFC46B;

    /** Head states. Only UNAVAILABLE shows a letter. LOADING shows a placeholder, never a generic face. */
    private static final int SKIN_UNAVAILABLE = 0;
    private static final int SKIN_LOADING = 1;
    private static final int SKIN_READY = 2;

    /** Skull textures already loaded, keyed by lower-case owner name. Filled by the skin loader's callback. */
    private static final Map<String, ResourceLocation> SKULL_SKINS = new ConcurrentHashMap<String, ResourceLocation>();
    /** Owners whose skull texture has been asked for, so each owner is requested once. */
    private static final Map<String, Boolean> SKULL_REQUESTED = new ConcurrentHashMap<String, Boolean>();

    /** One teammate, kept by slot number between frames. */
    private static final class Card {
        final int slot;
        ItemStack stack;
        /** The clean name shown on the card: no formatting codes, no class marker and no rank tag. */
        String name = "";
        /** The owner name from the skull's NBT, or the display name when the skull has no owner tag. */
        String owner = "";
        /** The class read from the leap item's own display name, or null. */
        LeapClass nameClass;
        /** The item's own "Class: X" lore line, or null. Used only when nothing else gives a class. */
        String loreClass;
        /** The class read from the scoreboard or the tab list, or null when not reliable. */
        LeapClass cls;
        /** Debug: the raw scoreboard and tab-list texts this card was matched from, and where the class came from. */
        String rawBoard;
        String rawTab;
        String source = "none";
        /** The scoreboard member the class was matched to, for the debug line. */
        String boardMember;
        /** The skull's own profile (name, id and textures), or null for a non-skull item. */
        GameProfile profile;
        String clsText = LeapClass.displayText(null);
        int clsColor = LeapClass.UNKNOWN_COLOR;
        ResourceLocation skin;
        int skinState = SKIN_UNAVAILABLE;
        long skinSinceNs;
        int x;
        int y;
        /** Fill used by the letter fallback in preview mode, where no skin can ever arrive. */
        int headFill = HEAD_LETTER;

        Card(int slot) {
            this.slot = slot;
        }
    }

    private final Container container;
    /** Where the preview returns to, or null on the real screen. */
    private final GuiScreen returnTo;
    /** True when this is the hub's test preview: no container, no server, no clicks. */
    private final boolean preview;
    private final List<Card> previous = new ArrayList<Card>();
    private final List<Card> cards = new ArrayList<Card>();
    private final List<Slot> slotBuffer = new ArrayList<Slot>();
    /** The sidebar lines for this frame, read once per refresh instead of once per card. */
    private final List<String> sidebar = new ArrayList<String>();

    private int highlight;
    private int cols = LeapLayout.COLS;
    /** Card width for the current screen. Set in {@link #layout()}. */
    private int cardW = LeapLayout.MIN_CARD_W;
    private long pendingUntilNs;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int closeX;
    private int closeY;
    private int closeW;

    public LeapScreen(Container container) {
        this.container = container;
        this.returnTo = null;
        this.preview = false;
    }

    private LeapScreen(GuiScreen returnTo) {
        this.container = null;
        this.returnTo = returnTo;
        this.preview = true;
    }

    /**
     * The test preview: the real screen, with sample teammates and no container behind it. Clicking a card does
     * nothing, and nothing is ever sent to a server.
     */
    public static LeapScreen preview(GuiScreen returnTo) {
        return new LeapScreen(returnTo);
    }

    /** True when this screen is the hub's preview. */
    public boolean isPreview() {
        return preview;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        if (preview) {
            buildPreviewCards();
            return;
        }
        // The chest the server opened was swapped before its own initGui ran, so make this container the
        // player's open container here. Otherwise the server's window updates would not reach this menu.
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.openContainer = container;
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        if (preview) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            container.onContainerClosed(mc.thePlayer);
        }
    }

    /** True while the server still has this menu open for the player. Always true in preview mode. */
    private boolean stillOpen() {
        if (preview) {
            return true;
        }
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        return p != null && p.openContainer == container;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!stillOpen()) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }

    /** Builds the four sample cards through the same parse path the real cards use. */
    private void buildPreviewCards() {
        cards.clear();
        previous.clear();
        LeapPreviewData.Sample[] samples = LeapPreviewData.samples();
        for (int i = 0; i < samples.length; i++) {
            Card c = new Card(i);
            LeapName.Parsed parsed = LeapName.parse(samples[i].text);
            c.name = parsed.name.isEmpty() ? "Unknown" : parsed.name;
            c.owner = c.name;
            c.nameClass = parsed.cls;
            c.headFill = samples[i].headFill;
            c.cls = parsed.cls;
            c.clsText = c.cls == null ? LeapClass.displayText(null) : c.cls.label();
            c.clsColor = c.cls == null ? LeapClass.UNKNOWN_COLOR : c.cls.color();
            c.source = c.cls == null ? "none (Unknown class)" : "preview";
            c.rawBoard = samples[i].text;
            c.rawTab = null;
            // No account behind a sample name, so there is no skin to fetch: the letter fallback is the real
            // fallback path the live screen uses when a head never arrives, drawn by the same code.
            c.skinState = SKIN_UNAVAILABLE;
            cards.add(c);
        }
        highlight = 0;
    }

    /**
     * Reads the teammates from the open menu. Cards are matched to the previous frame by slot number, so the
     * hover state survives. Item details are re-read only when the item in a slot changes. The scoreboard and
     * tab-list class is re-read every frame, so a change there shows up at once.
     */
    private void refresh() {
        if (preview) {
            // Sample data never changes, so the cards built in initGui are reused as they are.
            return;
        }
        SpiritLeap.teammateSlots(container, slotBuffer);
        previous.clear();
        previous.addAll(cards);
        cards.clear();
        long now = System.nanoTime();
        for (int i = 0, n = slotBuffer.size(); i < n; i++) {
            Slot s = slotBuffer.get(i);
            Card c = null;
            for (int k = 0, m = previous.size(); k < m; k++) {
                if (previous.get(k).slot == s.slotNumber) {
                    c = previous.get(k);
                    break;
                }
            }
            if (c == null) {
                c = new Card(s.slotNumber);
            }
            ItemStack stack = s.getStack();
            if (stack != c.stack) {
                c.stack = stack;
                readDetails(c, stack);
            }
            cards.add(c);
        }
        // Names are known only after every card has read its owner, so the class pass runs second.
        Collection<String> names = knownNames();
        readSidebar();
        for (Card c : cards) {
            resolve(c, now, names);
        }
        if (highlight >= cards.size()) {
            highlight = Math.max(0, cards.size() - 1);
        }
    }

    private void readDetails(Card c, ItemStack stack) {
        if (stack == null) {
            c.name = "";
            c.owner = "";
            c.loreClass = null;
            c.nameClass = null;
            return;
        }
        // The item's own name is client-visible and belongs to exactly this teammate, so it is read first:
        // it carries the class marker ("[B]") and the rank tags ("[VIP]") that used to end up in the drawn name.
        LeapName.Parsed parsed = LeapName.parse(stack.getDisplayName());
        c.nameClass = parsed.cls;
        c.name = parsed.name;
        c.owner = ownerName(stack, c.name);
        c.profile = skullProfile(stack);
        List<String> lore = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
        c.loreClass = LeapText.classFromLore(lore);
    }

    /** The skull owner's profile from the item NBT (compound form only), or null. Never throws. */
    static GameProfile skullProfile(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("SkullOwner", 10)) {
            return null;
        }
        try {
            GameProfile profile = NBTUtil.readGameProfileFromNBT(tag.getCompoundTag("SkullOwner"));
            return profile == null || profile.getName() == null || profile.getName().isEmpty() ? null : profile;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The sidebar lines, formatted the way the sidebar shows them, read once per refresh. */
    private void readSidebar() {
        sidebar.clear();
        Minecraft mc = Minecraft.getMinecraft();
        Scoreboard board = mc.theWorld == null ? null : mc.theWorld.getScoreboard();
        if (board == null) {
            return;
        }
        try {
            for (ScoreObjective objective : board.getObjectives()) {
                for (Score score : board.getSortedScores(objective)) {
                    String member = score.getPlayerName();
                    if (member == null) {
                        continue;
                    }
                    ScorePlayerTeam team = board.getPlayersTeam(member);
                    String line = team == null ? member : ScorePlayerTeam.formatPlayerName(team, member);
                    if (line != null) {
                        sidebar.add(line);
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // A scoreboard that changes while it is being read must never bring the screen down.
        }
    }

    /**
     * Works out a teammate's head state and class.
     *
     * The class comes from the leap item's own display name first, because that text belongs to this exact
     * teammate. If it says nothing, the scoreboard team prefix/suffix is used, then the sidebar lines when the
     * name is cut short, then the tab-list entry; when the scoreboard and the tab list both name a class and
     * disagree, the class stays unknown rather than picking one. The item's own "Class:" lore line is the last
     * resort. Nothing is guessed: every step needs the class marker in text that names this teammate.
     */
    private void resolve(Card c, long now, Collection<String> names) {
        Minecraft mc = Minecraft.getMinecraft();
        NetworkPlayerInfo info = null;
        if (!c.owner.isEmpty() && mc.getNetHandler() != null) {
            info = mc.getNetHandler().getPlayerInfo(c.owner);
        }
        resolveSkin(c, info, now);

        Scoreboard board = mc.theWorld == null ? null : mc.theWorld.getScoreboard();
        String[] boardHit = board == null ? null : findBoardEntry(board, c);
        String boardRaw = boardHit == null ? null : boardHit[1];
        String boardMember = boardHit == null ? null : boardHit[0];
        LeapClass fromBoard = boardHit == null ? null : classFrom(boardRaw, boardMember, names);
        String tabRaw = info == null ? null : tabText(info);
        String tabName = info == null || info.getGameProfile() == null ? c.owner : info.getGameProfile().getName();
        LeapClass fromTab = info == null ? null : classFrom(tabRaw, tabName, names);

        LeapClass cls;
        String source;
        if (c.nameClass != null) {
            cls = c.nameClass;
            source = "item name";
        } else if (fromBoard != null && fromTab != null && fromBoard != fromTab) {
            cls = null;
            source = "none";
        } else {
            cls = fromBoard != null ? fromBoard : fromTab;
            source = fromBoard != null ? "scoreboard" : fromTab != null ? "tab" : "none";
        }
        if (cls == null) {
            // The sidebar carries the same "[B] name" text the leap menu does. It is used for this teammate's
            // own name only, so one player's marker can never be handed to another player's card.
            LeapClass fromSidebar = classFromSidebar(c.owner.isEmpty() ? c.name : c.owner, names);
            if (fromSidebar != null) {
                cls = fromSidebar;
                source = "sidebar";
            }
        }
        if (cls == null && c.loreClass != null) {
            cls = LeapClass.parse(c.loreClass);
            if (cls != null) {
                source = "lore";
            }
        }
        c.cls = cls;
        c.clsText = cls == null ? LeapClass.displayText(null) : cls.label();
        c.clsColor = cls == null ? LeapClass.UNKNOWN_COLOR : cls.color();
        c.rawBoard = boardRaw;
        c.boardMember = boardMember;
        c.rawTab = tabRaw;
        c.source = cls == null ? "none (Unknown class)" : source;
    }

    /** The class for teammates from the sidebar lines, for the owner's name only. */
    private LeapClass classFromSidebar(String owner, Collection<String> names) {
        if (owner.isEmpty()) {
            return null;
        }
        for (int i = 0; i < sidebar.size(); i++) {
            LeapClass cls = classFrom(sidebar.get(i), owner, names);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    /**
     * Finds the scoreboard entry for a teammate. Every team is searched, and each member is compared with the
     * skull owner and the item's display name, ignoring case. Returns {member, formatted text} or null.
     * The formatted text is the team prefix, the member name and the suffix, as the sidebar shows them.
     */
    static String[] findBoardEntry(Scoreboard board, Card c) {
        List<String> candidates = new ArrayList<String>(2);
        if (!c.owner.isEmpty()) {
            candidates.add(c.owner);
        }
        if (!c.name.isEmpty() && !candidates.contains(c.name)) {
            candidates.add(c.name);
        }
        for (String want : candidates) {
            for (ScorePlayerTeam team : board.getTeams()) {
                for (String member : team.getMembershipCollection()) {
                    if (LeapText.sameName(member, want)) {
                        return new String[]{member, ScorePlayerTeam.formatPlayerName(team, member)};
                    }
                }
            }
        }
        return null;
    }

    /** The class from one raw text: the exact owner match first, then the unique truncated-name match. */
    private static LeapClass classFrom(String raw, String owner, Collection<String> names) {
        if (raw == null) {
            return null;
        }
        LeapClass exact = LeapText.classFromPrefix(raw, owner);
        return exact != null ? exact : LeapText.classFromTruncated(raw, owner, names);
    }

    /** Every name the client can see: the tab list profiles and the owners of the cards in this menu. */
    private Collection<String> knownNames() {
        List<String> out = new ArrayList<String>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() != null) {
            for (Object o : mc.getNetHandler().getPlayerInfoMap()) {
                NetworkPlayerInfo info = (NetworkPlayerInfo) o;
                if (info.getGameProfile() != null && info.getGameProfile().getName() != null) {
                    out.add(info.getGameProfile().getName());
                }
            }
        }
        for (Card c : cards) {
            if (!c.owner.isEmpty()) {
                out.add(c.owner);
            }
        }
        return out;
    }

    /**
     * Works out the head. The tab-list skin is used when the player is in the tab list. Otherwise the skull's own
     * texture is used, loaded once per owner. Never blocks: it only reads state and starts the loads.
     */
    private static void resolveSkin(Card c, NetworkPlayerInfo info, long now) {
        ResourceLocation skin = null;
        if (info != null) {
            // getLocationSkin() is what starts the download. hasLocationSkin() reports that it has arrived.
            ResourceLocation loc = info.getLocationSkin();
            if (info.hasLocationSkin()) {
                skin = loc;
            }
        }
        if (skin == null && c.profile != null) {
            skin = skullSkin(c.profile);
        }
        if (skin != null) {
            c.skin = skin;
            c.skinState = SKIN_READY;
            c.skinSinceNs = 0L;
            return;
        }
        c.skin = null;
        if (info == null && c.profile == null) {
            // Neither the tab list nor the skull can give a skin.
            c.skinState = SKIN_UNAVAILABLE;
            c.skinSinceNs = 0L;
            return;
        }
        if (c.skinSinceNs == 0L) {
            c.skinSinceNs = now;
        }
        c.skinState = now - c.skinSinceNs > SKIN_WAIT_NS ? SKIN_UNAVAILABLE : SKIN_LOADING;
    }

    /** The skull's texture once it has loaded, or null. Starts the load the first time an owner is seen. */
    private static ResourceLocation skullSkin(final GameProfile profile) {
        final String key = profile.getName().toLowerCase(Locale.ROOT);
        ResourceLocation done = SKULL_SKINS.get(key);
        if (done != null) {
            return done;
        }
        if (SKULL_REQUESTED.put(key, Boolean.TRUE) == null) {
            Minecraft.getMinecraft().getSkinManager().loadProfileTextures(profile,
                    new SkinManager.SkinAvailableCallback() {
                        @Override
                        public void skinAvailable(MinecraftProfileTexture.Type type, ResourceLocation loc,
                                                  MinecraftProfileTexture texture) {
                            if (type == MinecraftProfileTexture.Type.SKIN && loc != null) {
                                SKULL_SKINS.put(key, loc);
                            }
                        }
                    }, false);
        }
        return null;
    }

    /** The text of a tab-list entry, or the profile name when the entry has no display text. */
    private static String tabText(NetworkPlayerInfo info) {
        IChatComponent dn = info.getDisplayName();
        if (dn != null) {
            return dn.getUnformattedText();
        }
        return info.getGameProfile() == null ? null : info.getGameProfile().getName();
    }

    /** The skull's owner name, from either NBT form, or the display name when the skull has no owner tag. */
    static String ownerName(ItemStack stack, String fallback) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            if (tag.hasKey("SkullOwner", 8)) {
                String n = tag.getString("SkullOwner").trim();
                if (!n.isEmpty()) {
                    return n;
                }
            } else if (tag.hasKey("SkullOwner", 10)) {
                String n = tag.getCompoundTag("SkullOwner").getString("Name").trim();
                if (!n.isEmpty()) {
                    return n;
                }
            }
        }
        return fallback == null ? "" : fallback;
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!stillOpen()) {
            mc.displayGuiScreen(null);
            return;
        }
        refresh();
        layout();
        drawDefaultBackground();

        // Panel.
        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        drawBorder(panelX, panelY, panelW, panelH, PANEL_EDGE);

        FontRenderer fr = fr();
        String title = preview ? "Spirit Leap (Preview)" : "Spirit Leap";
        fr.drawStringWithShadow(title, panelX + PAD, panelY + 8, TEXT);
        String sub = preview ? "Test data" : cards.size() == 1 ? "1 teammate" : cards.size() + " teammates";
        fr.drawString(sub, panelX + panelW - PAD - fr.getStringWidth(sub), panelY + 8, preview ? TEST_BANNER : TEXT_DIM);
        drawRect(panelX + 1, panelY + TITLE_H, panelX + panelW - 1, panelY + TITLE_H + 1, PANEL_EDGE);

        int hovered = -1;
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (inside(mx, my, c.x, c.y, cardW, CARD_H)) {
                hovered = i;
            }
        }
        if (hovered >= 0) {
            highlight = hovered;
        }

        drawCardBackgrounds();
        drawHeads();
        drawCardText(fr);

        if (cards.isEmpty()) {
            String none = "No teammates in this menu";
            fr.drawString(none, panelX + (panelW - fr.getStringWidth(none)) / 2, panelY + TITLE_H + 14, TEXT_DIM);
        }

        // Footer: hint on the left, close on the right.
        int footY = panelY + panelH - FOOT_H;
        drawRect(panelX + 1, footY, panelX + panelW - 1, footY + 1, PANEL_EDGE);
        String hint;
        if (preview) {
            hint = "Preview only: sample teammates, nothing is sent. Esc closes";
        } else if (highlight >= 0 && highlight < cards.size()) {
            Card h = cards.get(highlight);
            hint = "Class: board=[" + plain(h.rawBoard) + "] tab=[" + plain(h.rawTab) + "] lore=["
                    + plain(h.loreClass) + "] source=" + h.source;
        } else {
            hint = "Click a teammate, or use the arrows and Enter. Esc closes";
        }
        int hintW = Math.max(0, panelW - 2 * PAD - 70);
        fr.drawString(fr.trimStringToWidth(hint, hintW), panelX + PAD, footY + 6, TEXT_DIM);
        String close = "Close";
        closeW = fr.getStringWidth(close);
        closeX = panelX + panelW - PAD - closeW;
        closeY = footY + 6;
        boolean closeHover = inside(mx, my, closeX - 4, footY, closeW + 8, FOOT_H);
        fr.drawString(close, closeX, closeY, closeHover ? TEXT : TEXT_DIM);

        super.drawScreen(mx, my, partialTicks);
    }

    /**
     * Text safe to draw: formatting codes removed. The debug line shows raw server text, and drawing a section
     * sign made the rest of the line change colour and look corrupted, so it is stripped before it is drawn.
     */
    static String plain(String s) {
        if (s == null) {
            return "-";
        }
        String t = LeapText.strip(s).trim();
        return t.isEmpty() ? "-" : t;
    }

    private void layout() {
        int n = cards.size();
        int avail = width - 2 * LeapLayout.MARGIN;
        cardW = LeapLayout.cardWidth(width, HEAD);
        cols = LeapLayout.COLS;
        int rows = LeapLayout.rows(n);
        int bodyH = rows == 0 ? EMPTY_H : rows * CARD_H + (rows - 1) * GAP;
        panelW = Math.min(avail, LeapLayout.panelWidth(cardW));
        panelH = TITLE_H + PAD + bodyH + PAD + FOOT_H;
        panelX = (width - panelW) / 2;
        panelY = Math.max(4, (height - panelH) / 2);

        int gridW = cols * cardW + (cols - 1) * GAP;
        int startX = panelX + (panelW - gridW) / 2;
        int startY = panelY + TITLE_H + PAD;
        for (int i = 0; i < n; i++) {
            Card c = cards.get(i);
            c.x = startX + (i % cols) * (cardW + GAP);
            c.y = startY + (i / cols) * (CARD_H + GAP);
        }
    }

    private void drawCardBackgrounds() {
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            boolean selected = i == highlight;
            drawRect(c.x, c.y, c.x + cardW, c.y + CARD_H, CARD_FILL);
            if (selected) {
                drawRect(c.x + 1, c.y + 1, c.x + cardW - 1, c.y + CARD_H - 1, CARD_LIT);
                drawBorder(c.x, c.y, cardW, CARD_H, CARD_SELECT_EDGE);
            } else {
                drawBorder(c.x, c.y, cardW, CARD_H, CARD_EDGE);
            }
        }
    }

    private void drawHeads() {
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.nanoTime() / 1.0e9 * 4.0);
        for (Card c : cards) {
            int hx = c.x + LeapLayout.HEAD_LEFT;
            int hy = c.y + (CARD_H - HEAD) / 2;
            if (c.skinState == SKIN_READY && c.skin != null) {
                SkinHead.draw(c.skin, hx, hy, HEAD, 1f);
            } else if (c.skinState == SKIN_LOADING) {
                drawRect(hx, hy, hx + HEAD, hy + HEAD, HEAD_PLACEHOLDER);
                int edge = 0xFF000000 | (int) (0x55 + 0x55 * pulse) << 16 | (int) (0x80 + 0x40 * pulse) << 8
                        | (int) (0xC0 + 0x3F * pulse);
                drawBorder(hx, hy, HEAD, HEAD, edge);
            } else {
                drawRect(hx, hy, hx + HEAD, hy + HEAD, c.headFill);
                String letter = c.name.isEmpty() || "Unknown".equals(c.name) ? "?"
                        : c.name.substring(0, 1).toUpperCase(Locale.ROOT);
                FontRenderer fr = fr();
                fr.drawString(letter, hx + (HEAD - fr.getStringWidth(letter)) / 2,
                        hy + (HEAD - 8) / 2, TEXT);
            }
        }
    }

    private void drawCardText(FontRenderer fr) {
        int maxW = LeapLayout.textWidth(cardW, HEAD);
        for (Card c : cards) {
            if (maxW <= 0) {
                continue;
            }
            int textX = c.x + LeapLayout.HEAD_LEFT + HEAD + LeapLayout.HEAD_GAP;
            // The name on the card never carries the class marker or a rank tag: those were removed when the
            // item's name was read. The class gets its own line, so the two can never be confused.
            String name = c.name.isEmpty() ? "Unknown" : c.name;
            name = fr.trimStringToWidth(name, maxW);
            int nameColor = c.cls == null ? TEXT : c.clsColor;
            fr.drawStringWithShadow(name, textX, c.y + 12, nameColor);
            String cls = fr.trimStringToWidth(c.clsText, maxW);
            fr.drawString(cls, textX, c.y + 25, TEXT_DIM);
        }
    }

    private static FontRenderer fr() {
        return Minecraft.getMinecraft().fontRendererObj;
    }

    /** One-pixel border made of four thin rectangles. */
    private static void drawBorder(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y, x + 1, y + h, color);
        drawRect(x + w - 1, y, x + w, y + h, color);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) {
            return;
        }
        if (inside(mx, my, closeX - 4, panelY + panelH - FOOT_H, closeW + 8, FOOT_H)) {
            leave();
            return;
        }
        if (preview) {
            // The preview is a layout test: a click only moves the highlight, nothing is sent anywhere.
            for (int i = 0; i < cards.size(); i++) {
                Card c = cards.get(i);
                if (inside(mx, my, c.x, c.y, cardW, CARD_H)) {
                    highlight = i;
                    return;
                }
            }
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (inside(mx, my, c.x, c.y, cardW, CARD_H)) {
                pick(i);
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char c, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) {
            leave();
            return;
        }
        if (cards.isEmpty()) {
            return;
        }
        switch (code) {
            case Keyboard.KEY_LEFT:
                highlight = Math.max(0, highlight - 1);
                return;
            case Keyboard.KEY_RIGHT:
                highlight = Math.min(cards.size() - 1, highlight + 1);
                return;
            case Keyboard.KEY_UP:
                highlight = Math.max(0, highlight - cols);
                return;
            case Keyboard.KEY_DOWN:
                highlight = Math.min(cards.size() - 1, highlight + cols);
                return;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
            case Keyboard.KEY_SPACE:
                if (!preview) {
                    pick(highlight);
                }
                return;
            default:
                break;
        }
    }

    /**
     * Sends the same window click vanilla sends for a left click on the teammate's slot. After a pick the screen
     * waits for the server (pending), so a double click or a repeated Enter cannot send a second leap.
     */
    private void pick(int index) {
        if (preview || index < 0 || index >= cards.size()) {
            return;
        }
        long now = System.nanoTime();
        if (pendingUntilNs != 0L) {
            if (now < pendingUntilNs) {
                return;
            }
            pendingUntilNs = 0L;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.playerController == null || mc.thePlayer == null || !stillOpen()) {
            return;
        }
        pendingUntilNs = now + PENDING_NS;
        highlight = index;
        mc.playerController.windowClick(container.windowId, cards.get(index).slot, 0, 0, mc.thePlayer);
    }

    /** Leaves the screen: back to the hub in preview mode, or the vanilla close packet for a real menu. */
    private void leave() {
        if (preview) {
            Minecraft.getMinecraft().displayGuiScreen(returnTo);
            return;
        }
        closeMenu();
    }

    /** Closes the menu the way vanilla does: the close packet goes to the server, then the screen closes. */
    private void closeMenu() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (p != null && p.openContainer == container) {
            p.closeScreen();
        }
        if (mc.currentScreen == this) {
            mc.displayGuiScreen((GuiScreen) null);
        }
    }
}
