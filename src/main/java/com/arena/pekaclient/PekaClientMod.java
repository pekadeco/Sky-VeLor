package com.arena.pekaclient;

import com.arena.pekaclient.command.ClientActions;
import com.arena.pekaclient.command.ItemDebugCommand;
import com.arena.pekaclient.command.PriceSetCommand;
import com.arena.pekaclient.command.QuickNoteCommand;
import com.arena.pekaclient.command.SvCommand;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.dungeon.DragonKillAlert;
import com.arena.pekaclient.dungeon.DungeonMapOverlay;
import com.arena.pekaclient.dungeon.PuzzleRoomDatabase;
import com.arena.pekaclient.dungeon.M7OverlayRenderer;
import com.arena.pekaclient.dungeon.TerminalSolver;
import com.arena.pekaclient.fishing.FishingStats;
import com.arena.pekaclient.slayer.SlayerTracker;
import com.arena.pekaclient.feature.ChatChannelTabs;
import com.arena.pekaclient.feature.ChattingAnimationBridge;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.feature.LeapUse;
import com.arena.pekaclient.feature.SpiritLeap;
import com.arena.pekaclient.gui.AppleGuiButton;
import com.arena.pekaclient.gui.AppleHubScreen;
import com.arena.pekaclient.gui.HudEditorScreen;
import com.arena.pekaclient.gui.LeapScreen;
import com.arena.pekaclient.gui.LoadingScreen;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.render.MobHighlight;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHookStatus;
import com.arena.pekaclient.startup.StartupTasks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Entry point for Sky VeLor. Client-only. Adds:
 *  - a keybind (default Right Shift) that opens the hub from gameplay,
 *  - a "Sky VeLor" button on the main menu and pause menu,
 *  - movable HUD elements (Mining BPS, balance, inventory, hotbar, player doll) with an editor,
 *  - the Spirit Leap teammate selector, which replaces only the server-opened Spirit Leap menu when enabled,
 *  - a loading screen that runs Sky VeLor's own startup tasks,
 *  - M7 dungeon overlay, boss and dragon hitboxes, and the dragon kill alert,
 *  - all-rarity fishing counters, Festival best, Sea Creature HP and the Lord Jawbus alert,
 *  - optional client-side chat tabs and an opt-in bridge for an already-installed compatible animation runtime.
 * HUD/world features use standard Forge 1.8.9 events. Item Animation has one isolated Forge coremod hook around
 * Minecraft's real first-person hand renderer and equipped-progress update; it does not replace renderers, mutate
 * final fields, or change entity/world/gameplay rendering.
 */
@Mod(modid = PekaClientMod.MODID,
        name = PekaClientMod.NAME,
        version = PekaClientMod.VERSION,
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true)
public class PekaClientMod {

    /** The modid stays as it was. It names the asset folder and the keybind ids, so changing it would break them. */
    public static final String MODID = "pekaclient";
    public static final String NAME = PekaVersion.DISPLAY_NAME;
    public static final String VERSION = PekaVersion.NAME;

    /** Unique-enough id for our injected menu button (vanilla ids are < 200). */
    private static final int MENU_BUTTON_ID = 0x41504C;

    /** The keybind id is kept so a binding the player already set still works. Only its category name changed. */
    public static final KeyBinding OPEN_KEY =
            new KeyBinding("key.pekaclient.open", Keyboard.KEY_RSHIFT, "key.categories.pekaclient");

    /** Opens the HUD editor directly on Item Animation. It is deliberately unbound on a fresh install. */
    public static final KeyBinding ITEM_ANIMATION_KEY =
            new KeyBinding("key.pekaclient.itemanimation", Keyboard.KEY_NONE, "key.categories.pekaclient");

    /**
     * Use Spirit Leap: default unbound. Pressing it while holding a Spirit Leap item sends the same right-click
     * the game sends. The server still opens its own leap menu first, and only then can the teammate picker show.
     */
    public static final KeyBinding LEAP_KEY =
            new KeyBinding("key.pekaclient.leap", Keyboard.KEY_NONE, "key.categories.pekaclient");

    private boolean loadingShown;
    /** The world identity already given the explicit Item Animation launcher/hook diagnosis. */
    private Object itemAnimationReportedWorld;
    private final M7OverlayRenderer m7Overlay = new M7OverlayRenderer();
    private final DungeonMapOverlay dungeonMapOverlay = new DungeonMapOverlay();
    private final DragonKillAlert dragonKill = new DragonKillAlert(m7Overlay);
    private final TerminalSolver terminalSolver = new TerminalSolver();
    private final FishingStats fishingStats = new FishingStats();
    private final SlayerTracker slayerTracker = new SlayerTracker();

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Settings are read here so the first screen has them. The loading screen runs the rest.
        AppleSettings.load();
        // Safe default off: this is a no-op unless the player opted in and a compatible external Chatting runtime exists.
        ChattingAnimationBridge.apply();
        Theme.refresh();
        ClientRegistry.registerKeyBinding(OPEN_KEY);
        ClientRegistry.registerKeyBinding(ITEM_ANIMATION_KEY);
        ClientRegistry.registerKeyBinding(LEAP_KEY);
        StartupTasks.add("Reading the HUD layout", HudManager::ensureLoaded);
        StartupTasks.add("Applying the theme", Theme::refresh);
        StartupTasks.add("Reading fishing counters", FishingStats::load);
        StartupTasks.add("Loading dungeon map templates", PuzzleRoomDatabase::load);
        ClientCommandHandler.instance.registerCommand(new SvCommand());
        ClientCommandHandler.instance.registerCommand(new ItemDebugCommand());
        ClientCommandHandler.instance.registerCommand(new QuickNoteCommand());
        ClientCommandHandler.instance.registerCommand(new PriceSetCommand("setprice"));
        ClientCommandHandler.instance.registerCommand(new PriceSetCommand("priceset"));
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ChatChannelTabs());
        MinecraftForge.EVENT_BUS.register(m7Overlay);
        MinecraftForge.EVENT_BUS.register(dungeonMapOverlay);
        MinecraftForge.EVENT_BUS.register(terminalSolver);
        MinecraftForge.EVENT_BUS.register(fishingStats);
        MinecraftForge.EVENT_BUS.register(slayerTracker);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            reportItemAnimationWorldJoin(mc);
            if (!loadingShown && StartupTasks.hasWork()) {
                // Show the loading screen only while real startup tasks are pending. Shown once per launch.
                loadingShown = true;
                mc.displayGuiScreen(new LoadingScreen(mc.currentScreen));
            }
            // Before the world moves. Also resets mining counts on world change, respawn and disconnect.
            // Item Animation is woven into Minecraft's authoritative hand/equip methods at class-load time; it has
            // no per-tick installation retry and its live settings are read only from those renderer callbacks.
            HudManager.tick(mc);
            DungeonContext.tick(mc);
            dragonKill.tick(System.currentTimeMillis());
            return;
        }
        ClientActions.run(mc);
        if (OPEN_KEY.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new AppleHubScreen(null));
        }
        while (ITEM_ANIMATION_KEY.isPressed()) {
            if (mc.currentScreen == null) {
                ClientActions.requestItemAnimation();
            }
        }
        while (LEAP_KEY.isPressed()) {
            if (mc.currentScreen == null && AppleSettings.leapKey) {
                useSpiritLeap(mc);
            }
        }
    }

    /**
     * Emits the real launcher/coremod state once per joined world. Persisted sliders alone are not evidence that
     * Forge accepted the transformer, so both the client log and in-game chat show ACTIVE or FAILED plus why.
     */
    private void reportItemAnimationWorldJoin(Minecraft mc) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            if (mc.theWorld == null) {
                itemAnimationReportedWorld = null;
            }
            return;
        }
        if (itemAnimationReportedWorld == mc.theWorld) {
            return;
        }
        itemAnimationReportedWorld = mc.theWorld;
        // Resolve lazy transformer diagnostics before taking the state/reason used in the single visible join line.
        ItemAnimationHookStatus.worldJoinReport();
        String version = PekaVersion.NAME;
        if (version.startsWith("Beta-")) {
            version = version.substring("Beta-".length());
        }
        if (version.endsWith("v")) {
            version = version.substring(0, version.length() - 1);
        }
        String state = ItemAnimationHookStatus.isActive() ? "ACTIVE" : "FAILED";
        String line = "Sky VeLor " + version + " | Item Animation: /velor itemanimation | Hook: " + state
                + " " + ItemAnimationHookStatus.getReason();
        System.out.println("[Sky VeLor] " + line);
        mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7b" + line));
    }

    /**
     * Sends the vanilla use-item action for the held Spirit Leap. Only inside a dungeon, and only when the item in
     * the selected hotbar slot is a Spirit Leap item. The server validates the use and opens its menu.
     */
    private static void useSpiritLeap(Minecraft mc) {
        EntityPlayerSP p = mc.thePlayer;
        if (p == null || mc.playerController == null || mc.theWorld == null) {
            return;
        }
        if (!DungeonContext.inDungeon()) {
            chat(mc, "Spirit Leap can only be used inside a dungeon (Catacombs sidebar).");
            return;
        }
        ItemStack held = p.getHeldItem();
        if (held == null || !LeapUse.isLeapItemName(held.getDisplayName())) {
            chat(mc, "Hold a Spirit Leap item in your selected hotbar slot first.");
            return;
        }
        LeapUse.markUsed();
        mc.playerController.sendUseItem(p, mc.theWorld, held);
    }

    private static void chat(Minecraft mc, String text) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7b[Sky VeLor] \u00a7f" + text));
        }
    }

    /** A left-click on a held Spirit Leap item triggers the leap, so the server opens the menu the GUI replaces. */
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.button != 0 || !event.buttonstate) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.thePlayer == null || !DungeonContext.inDungeon()) {
            return;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held != null && LeapUse.isLeapItemName(held.getDisplayName())) {
            // A left-click on a held Leap item triggers the leap: same use action as a right-click.
            // The server then opens its menu, and the Leap GUI takes it over.
            LeapUse.markUsed();
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, held);
        }
    }

    /** Overlay only: runs after the world is drawn and does not change any entity or block renderer. */
    @SubscribeEvent
    public void onWorldLast(RenderWorldLastEvent event) {
        MobHighlight.draw(event.partialTicks);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        // The editor draws the elements itself, so skip the in-game copy while it is open.
        if (!(mc.currentScreen instanceof HudEditorScreen) && HudManager.visibleInGame()) {
            HudManager.render(event.resolution);
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiMainMenu || event.gui instanceof GuiIngameMenu) {
            // Top-right corner: does not collide with vanilla's centred column.
            event.buttonList.add(new AppleGuiButton(MENU_BUTTON_ID, event.gui.width - 92, 8, 84, 20, NAME));
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button != null && event.button.id == MENU_BUTTON_ID) {
            event.setCanceled(true);
            Minecraft.getMinecraft().displayGuiScreen(new AppleHubScreen(event.gui));
        }
    }

    /**
     * Dungeon tab, Spirit Leap. When the server opens its leap menu and the feature is on, the teammate picker
     * replaces that screen. The server's container is used unchanged, so every click is the server's own window
     * click, and the server still validates it. The menu is recognised by {@link SpiritLeap#isLeapMenu}: its title,
     * or a recent Spirit Leap use inside a dungeon with teammate heads in the chest.
     */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Earliest screen Sky VeLor controls: the first main menu, shown only while startup work is pending.
        // The loading screen returns to that same main menu when its work is finished.
        if (event.gui instanceof GuiMainMenu && !loadingShown && StartupTasks.hasWork()) {
            loadingShown = true;
            event.gui = new LoadingScreen(event.gui);
            return;
        }
        if (!AppleSettings.leapMenu || !(event.gui instanceof GuiChest)) {
            return;
        }
        GuiChest chest = (GuiChest) event.gui;
        if (SpiritLeap.isLeapMenu(chest.inventorySlots)) {
            event.gui = new LeapScreen(chest.inventorySlots);
        }
    }
}
