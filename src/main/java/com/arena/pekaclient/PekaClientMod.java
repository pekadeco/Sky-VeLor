package com.arena.pekaclient;

import com.arena.pekaclient.assist.ShortcutController;
import com.arena.pekaclient.assist.CommandKeybindController;
import com.arena.pekaclient.assist.WardrobeShortcutController;
import com.arena.pekaclient.assist.inventory.InventoryAssistController;
import com.arena.pekaclient.assist.dungeon.DungeonBossTracker;
import com.arena.pekaclient.assist.dungeon.DungeonRunTracker;
import com.arena.pekaclient.assist.dungeon.DungeonScoreTracker;
import com.arena.pekaclient.assist.dungeon.DungeonChestCloser;
import com.arena.pekaclient.assist.dungeon.RoomClearAlertController;
import com.arena.pekaclient.assist.dungeon.DungeonTerminalAssistController;
import com.arena.pekaclient.assist.dungeon.DungeonWaypointController;
import com.arena.pekaclient.assist.dungeon.SpiritBowLocator;
import com.arena.pekaclient.assist.sync.DungeonSyncManager;
import com.arena.pekaclient.assist.movement.AutoSprintController;
import com.arena.pekaclient.assist.visual.AspectTeleportTransitionRenderer;
import com.arena.pekaclient.assist.visual.DeadMobRenderFilter;
import com.arena.pekaclient.command.ClientActions;
import com.arena.pekaclient.command.ItemDebugCommand;
import com.arena.pekaclient.command.PriceSetCommand;
import com.arena.pekaclient.command.QuickNoteCommand;
import com.arena.pekaclient.command.SvCommand;
import com.arena.pekaclient.command.SvDungeonCommand;
import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.config.AssistSettings;
import com.arena.pekaclient.config.KeybindMigration;
import com.arena.pekaclient.dungeon.DragonKillAlert;
import com.arena.pekaclient.dungeon.DungeonMapOverlay;
import com.arena.pekaclient.dungeon.PuzzleRoomDatabase;
import com.arena.pekaclient.dungeon.M7OverlayRenderer;
import com.arena.pekaclient.dungeon.TerminalSolver;
import com.arena.pekaclient.dungeon.DungeonProgressTracker;
import com.arena.pekaclient.dungeon.SpiritBowTracer;
import com.arena.pekaclient.dungeon.WitherDoorHighlight;
import com.arena.pekaclient.dungeon.solver.devices.FirstDeviceSolver;
import com.arena.pekaclient.dungeon.solver.devices.ThirdDeviceSolver;
import com.arena.pekaclient.dungeon.solver.puzzles.BoulderSolver;
import com.arena.pekaclient.dungeon.solver.puzzles.CreeperSolver;
import com.arena.pekaclient.dungeon.solver.puzzles.SilverFishSolver;
import com.arena.pekaclient.dungeon.solver.puzzles.TeleportMazeSolver;
import com.arena.pekaclient.dungeon.solver.puzzles.ThreeWeirdos;
import com.arena.pekaclient.dungeon.solver.puzzles.WaterSolver;
import com.arena.pekaclient.dungeon.solver.terminals.ClickInOrderSolver;
import com.arena.pekaclient.dungeon.solver.terminals.ClickOnColorsSolver;
import com.arena.pekaclient.dungeon.solver.terminals.CorrectPanesSolver;
import com.arena.pekaclient.dungeon.solver.terminals.HideTooltips;
import com.arena.pekaclient.dungeon.solver.terminals.MazeSolver;
import com.arena.pekaclient.dungeon.solver.terminals.StartingWithSolver;
import com.arena.pekaclient.dungeon.solver.terminals.TerminalDebug;
import com.arena.pekaclient.dungeon.solver.terminals.TerminalDebugCommand;
import com.arena.pekaclient.dungeon.solver.terminals.TerminalGuiLayer;
import com.arena.pekaclient.dungeon.solver.terminals.TerminalSimulatorCommand;
import com.arena.pekaclient.dungeon.solver.terminals.TerminalTracker;
import com.arena.pekaclient.dungeon.solver.terminals.TerminalWaypoints;
import com.arena.pekaclient.dungeonesp.DungeonEspController;
import com.arena.pekaclient.fishing.FishingStats;
import com.arena.pekaclient.slayer.SlayerTracker;
import com.arena.pekaclient.feature.ChatChannelTabs;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.feature.LeapUse;
import com.arena.pekaclient.feature.HotbarScrollPolicy;
import com.arena.pekaclient.feature.SpiritLeap;
import com.arena.pekaclient.feature.protection.ItemProtection;
import com.arena.pekaclient.feature.qol.EntityVisualCleanup;
import com.arena.pekaclient.feature.qol.EquipmentOverlay;
import com.arena.pekaclient.feature.qol.RemoveIrritatingMessages;
import com.arena.pekaclient.events.PacketHandler;
import com.arena.pekaclient.gui.AppleGuiButton;
import com.arena.pekaclient.gui.AppleSettingsScreen;
import com.arena.pekaclient.gui.HudEditorScreen;
import com.arena.pekaclient.gui.DungeonHudEditorScreen;
import com.arena.pekaclient.gui.GuiDebugOverlay;
import com.arena.pekaclient.gui.LeapScreen;
import com.arena.pekaclient.gui.LoadingScreen;
import com.arena.pekaclient.gui.LunariaDownloadingTerrainScreen;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.itemlockswap.ItemLockSwapController;
import com.arena.pekaclient.itemlockswap.ItemLockSwapKeybind;
import com.arena.pekaclient.itemlockswap.ItemLockSwapOverlay;
import com.arena.pekaclient.itemlockswap.ItemLockSwapSettings;
import com.arena.pekaclient.render.Theme;
import com.arena.pekaclient.render.BloodMobHighlight;
import com.arena.pekaclient.render.MobHighlight;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHookStatus;
import com.arena.pekaclient.startup.StartupTasks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import io.netty.channel.ChannelPipeline;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * Entry point for Lunaria. Client-only. Adds:
 *  - a keybind (default Right Shift) that opens the hub from gameplay,
 *  - a "Lunaria" button on the main menu and pause menu,
 *  - movable HUD elements (Mining BPS, inventory, player doll) with an editor,
 *  - the Spirit Leap teammate selector, which replaces only the server-opened Spirit Leap menu when enabled,
 *  - a loading screen that runs Lunaria's own startup tasks,
 *  - M7 dungeon overlay, boss and dragon hitboxes, and the dragon kill alert,
 *  - all-rarity fishing counters, Festival best, Sea Creature HP and the Lord Jawbus alert,
 *  - an opt-in Default Chat QoL layer with attached All/Party/Reply/custom tabs and inline settings groups.
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

    /** Public metadata now uses Lunaria; KeybindMigration and AppleSettings retain established Controls/config values. */
    public static final String MODID = "lunaria";
    public static final String NAME = PekaVersion.DISPLAY_NAME;
    public static final String VERSION = PekaVersion.NAME;

    /** Unique-enough id for our injected menu button (vanilla ids are < 200). */
    private static final int MENU_BUTTON_ID = 0x41504C;

    /** The keybind id is kept so a binding the player already set still works. Only its category name changed. */
    public static final KeyBinding OPEN_KEY =
            new KeyBinding("key.lunaria.open", Keyboard.KEY_RSHIFT, "key.categories.lunaria");

    /** Opens the HUD editor directly on Item Animation. It is deliberately unbound on a fresh install. */
    public static final KeyBinding ITEM_ANIMATION_KEY =
            new KeyBinding("key.lunaria.itemanimation", Keyboard.KEY_NONE, "key.categories.lunaria");

    /**
     * Use Spirit Leap: default unbound. Pressing it while holding a Spirit Leap item sends the same right-click
     * the game sends. The server still opens its own leap menu first, and only then can the teammate picker show.
     */
    public static final KeyBinding LEAP_KEY =
            new KeyBinding("key.lunaria.leap", Keyboard.KEY_NONE, "key.categories.lunaria");

    /** Optional configurable alternative to Ctrl+Tab for cycling visible native chat tabs. */
    public static final KeyBinding CHAT_TAB_KEY =
            new KeyBinding("key.lunaria.chatTab", Keyboard.KEY_NONE, "key.categories.lunaria");

    /** Optional direct route to Chat Settings; unbound on fresh installs. */
    public static final KeyBinding CHAT_EDITOR_KEY =
            new KeyBinding("key.lunaria.chatEditor", Keyboard.KEY_NONE, "key.categories.lunaria");

    /** Default L: release over B to create/remove A↔B; release over an unpaired A to toggle its lock, or a paired A to remove its pair. */
    public static final KeyBinding ITEM_LOCK_SWAP_KEY =
            new KeyBinding("key.lunaria.itemLockSwap", Keyboard.KEY_L, "key.categories.lunaria");

    /** Fresh installs and Shortcut reset use W/P; an already-saved Controls or Shortcut value always wins instead. */
    public static final KeyBinding WARDROBE_SHORTCUT_KEY =
            new KeyBinding("key.lunaria.wardrobeShortcut", Keyboard.KEY_W, "key.categories.lunaria");
    public static final KeyBinding PETS_SHORTCUT_KEY =
            new KeyBinding("key.lunaria.petsShortcut", Keyboard.KEY_P, "key.categories.lunaria");
    private boolean loadingShown;
    private volatile NetworkManager pendingPacketBridgeManager;
    private long packetBridgeWaitStartedAt;
    private boolean packetBridgeWaitNoticeShown;
    /** The world identity already given the explicit Item Animation launcher/hook diagnosis. */
    private Object itemAnimationReportedWorld;
    private final M7OverlayRenderer m7Overlay = new M7OverlayRenderer();
    private final DungeonMapOverlay dungeonMapOverlay = new DungeonMapOverlay();
    private final DungeonSyncManager dungeonSyncManager = new DungeonSyncManager(dungeonMapOverlay);
    private final DungeonEspController dungeonEspController = new DungeonEspController();
    private final AspectTeleportTransitionRenderer aspectTeleportTransition = new AspectTeleportTransitionRenderer();
    private final DeadMobRenderFilter deadMobRenderFilter = new DeadMobRenderFilter();
    private final DragonKillAlert dragonKill = new DragonKillAlert(m7Overlay);
    private final TerminalSolver terminalSolver = new TerminalSolver();
    private final FishingStats fishingStats = new FishingStats();
    private final SlayerTracker slayerTracker = new SlayerTracker();
    private final ItemLockSwapOverlay itemLockSwapOverlay = new ItemLockSwapOverlay();
    private final ItemLockSwapController itemLockSwapController = new ItemLockSwapController();
    private final DungeonBossTracker dungeonBossTracker = new DungeonBossTracker();
    private final DungeonTerminalAssistController dungeonTerminalAssistController = new DungeonTerminalAssistController();
    private final RoomClearAlertController roomClearAlertController = new RoomClearAlertController();
    private final DungeonChestCloser dungeonChestCloser = new DungeonChestCloser();
    private final DungeonWaypointController dungeonWaypointController = new DungeonWaypointController();
    private final SpiritBowLocator spiritBowLocator = new SpiritBowLocator();
    private final AutoSprintController autoSprintController = new AutoSprintController();
    private final ShortcutController shortcutController = new ShortcutController();
    private final WardrobeShortcutController wardrobeShortcutController = new WardrobeShortcutController();
    private final CommandKeybindController commandKeybindController = new CommandKeybindController();
    private final InventoryAssistController inventoryAssistController = new InventoryAssistController();

    // Friend-base dungeon and visual owners which do not overlap the current retained controllers.
    private final BloodMobHighlight bloodMobHighlight = new BloodMobHighlight();
    private final MobHighlight mobHighlight = new MobHighlight();
    private final WitherDoorHighlight witherDoorHighlight = new WitherDoorHighlight();
    private final SpiritBowTracer spiritBowTracer = new SpiritBowTracer();
    private final EquipmentOverlay equipmentOverlay = new EquipmentOverlay();
    private final EntityVisualCleanup entityVisualCleanup = new EntityVisualCleanup();
    private final RemoveIrritatingMessages removeIrritatingMessages = new RemoveIrritatingMessages();
    private final ItemProtection itemProtection = new ItemProtection();
    private final BoulderSolver boulderSolver = new BoulderSolver();
    private final CreeperSolver creeperSolver = new CreeperSolver();
    private final SilverFishSolver silverFishSolver = new SilverFishSolver();
    private final TeleportMazeSolver teleportMazeSolver = new TeleportMazeSolver();
    private final ThreeWeirdos threeWeirdosSolver = new ThreeWeirdos();
    private final WaterSolver waterSolver = new WaterSolver();
    private final FirstDeviceSolver firstDeviceSolver = new FirstDeviceSolver();
    private final ThirdDeviceSolver thirdDeviceSolver = new ThirdDeviceSolver();
    private final ClickInOrderSolver clickInOrderSolver = new ClickInOrderSolver();
    private final ClickOnColorsSolver clickOnColorsSolver = new ClickOnColorsSolver();
    private final CorrectPanesSolver correctPanesSolver = new CorrectPanesSolver();
    private final HideTooltips hideTooltips = new HideTooltips();
    private final MazeSolver mazeSolver = new MazeSolver();
    private final StartingWithSolver startingWithSolver = new StartingWithSolver();
    private final TerminalTracker terminalTracker = new TerminalTracker();
    private final TerminalDebug terminalDebug = new TerminalDebug();
    private final TerminalGuiLayer terminalGuiLayer = new TerminalGuiLayer();
    private final TerminalWaypoints terminalWaypoints = new TerminalWaypoints();

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Settings are read here so the first screen has them. The loading screen runs the rest.
        AppleSettings.load();
        Theme.refresh();
        ClientRegistry.registerKeyBinding(OPEN_KEY);
        ClientRegistry.registerKeyBinding(ITEM_ANIMATION_KEY);
        ClientRegistry.registerKeyBinding(LEAP_KEY);
        ClientRegistry.registerKeyBinding(CHAT_TAB_KEY);
        ClientRegistry.registerKeyBinding(CHAT_EDITOR_KEY);
        ClientRegistry.registerKeyBinding(ITEM_LOCK_SWAP_KEY);
        ClientRegistry.registerKeyBinding(WARDROBE_SHORTCUT_KEY);
        ClientRegistry.registerKeyBinding(PETS_SHORTCUT_KEY);
        // Item Protection is deliberately unbound by default, avoiding a collision with Item Lock and Swap.
        ClientRegistry.registerKeyBinding(ItemProtection.PROTECT_KEY);
        // Translation-key renames would otherwise make Minecraft treat existing Controls values as unrelated defaults.
        KeybindMigration.migrate(new java.io.File(Minecraft.getMinecraft().mcDataDir, "options.txt"),
                KeybindMigration.binding(OPEN_KEY, "key.lunaria.open", KeybindMigration.historicOpen()),
                KeybindMigration.binding(ITEM_ANIMATION_KEY, "key.lunaria.itemanimation", "key.pekaclient.itemanimation"),
                KeybindMigration.binding(LEAP_KEY, "key.lunaria.leap", KeybindMigration.historicLeap()),
                KeybindMigration.binding(CHAT_TAB_KEY, "key.lunaria.chatTab", "key.pekaclient.chatTab"),
                KeybindMigration.binding(CHAT_EDITOR_KEY, "key.lunaria.chatEditor", "key.pekaclient.chatEditor"),
                KeybindMigration.binding(ITEM_LOCK_SWAP_KEY, "key.lunaria.itemLockSwap", "key.pekaclient.itemLockSwap"),
                KeybindMigration.binding(WARDROBE_SHORTCUT_KEY, "key.lunaria.wardrobeShortcut",
                        KeybindMigration.friendWardrobe()),
                KeybindMigration.binding(PETS_SHORTCUT_KEY, "key.lunaria.petsShortcut", KeybindMigration.friendPets()),
                KeybindMigration.binding(ItemProtection.PROTECT_KEY, "key.lunaria.protectItem",
                        "key.pekaclient.protectItem"));
        // The Controls screen owns vanilla's options.txt copy; the feature config is authoritative on first load.
        // A saved zero is intentional: it means the player cleared the feature binding in the settings shell.
        if (ItemLockSwapKeybind.validStoredKeyCode(ItemLockSwapSettings.keyCode)) {
            ITEM_LOCK_SWAP_KEY.setKeyCode(ItemLockSwapSettings.keyCode);
            KeyBinding.resetKeyBindingArrayAndHash();
        }
        // A saved Shortcut value wins. If an older config has no value, retain the Controls binding already loaded by
        // Minecraft; fresh assist properties explicitly contain the new W/P defaults.
        boolean shortcutKeysChanged = false;
        if (AssistSettings.validShortcutKeyCode(AssistSettings.wardrobeShortcutKeyCode)) {
            WARDROBE_SHORTCUT_KEY.setKeyCode(AssistSettings.wardrobeShortcutKeyCode);
            shortcutKeysChanged = true;
        } else {
            AssistSettings.wardrobeShortcutKeyCode = WARDROBE_SHORTCUT_KEY.getKeyCode();
        }
        if (AssistSettings.validShortcutKeyCode(AssistSettings.petsShortcutKeyCode)) {
            PETS_SHORTCUT_KEY.setKeyCode(AssistSettings.petsShortcutKeyCode);
            shortcutKeysChanged = true;
        } else {
            AssistSettings.petsShortcutKeyCode = PETS_SHORTCUT_KEY.getKeyCode();
        }
        if (shortcutKeysChanged) KeyBinding.resetKeyBindingArrayAndHash();
        StartupTasks.add("Reading the HUD layout", HudManager::ensureLoaded);
        StartupTasks.add("Applying the theme", Theme::refresh);
        StartupTasks.add("Reading fishing counters", FishingStats::load);
        StartupTasks.add("Loading dungeon map templates", PuzzleRoomDatabase::load);
        ClientCommandHandler.instance.registerCommand(new SvCommand());
        ClientCommandHandler.instance.registerCommand(new SvDungeonCommand());
        ClientCommandHandler.instance.registerCommand(new ItemDebugCommand());
        ClientCommandHandler.instance.registerCommand(new QuickNoteCommand());
        ClientCommandHandler.instance.registerCommand(new PriceSetCommand("setprice"));
        ClientCommandHandler.instance.registerCommand(new PriceSetCommand("priceset"));
        ClientCommandHandler.instance.registerCommand(new TerminalSimulatorCommand());
        ClientCommandHandler.instance.registerCommand(new TerminalDebugCommand());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ChatChannelTabs());
        // Must be registered on the real Forge bus: it observes finished GuiScreen frames without changing input.
        MinecraftForge.EVENT_BUS.register(new GuiDebugOverlay());
        MinecraftForge.EVENT_BUS.register(m7Overlay);
        MinecraftForge.EVENT_BUS.register(dungeonMapOverlay);
        MinecraftForge.EVENT_BUS.register(dungeonSyncManager);
        MinecraftForge.EVENT_BUS.register(dungeonEspController);
        MinecraftForge.EVENT_BUS.register(deadMobRenderFilter);
        MinecraftForge.EVENT_BUS.register(terminalSolver);
        MinecraftForge.EVENT_BUS.register(fishingStats);
        MinecraftForge.EVENT_BUS.register(slayerTracker);
        MinecraftForge.EVENT_BUS.register(itemLockSwapOverlay);
        MinecraftForge.EVENT_BUS.register(itemLockSwapController);
        MinecraftForge.EVENT_BUS.register(HudManager.MINING_ASSIST.controller());
        MinecraftForge.EVENT_BUS.register(dungeonBossTracker);
        MinecraftForge.EVENT_BUS.register(dungeonTerminalAssistController);
        MinecraftForge.EVENT_BUS.register(dungeonWaypointController);
        MinecraftForge.EVENT_BUS.register(DungeonRunTracker.INSTANCE);
        DungeonRunTracker.markRegistered();
        MinecraftForge.EVENT_BUS.register(DungeonScoreTracker.INSTANCE);
        MinecraftForge.EVENT_BUS.register(roomClearAlertController);
        MinecraftForge.EVENT_BUS.register(dungeonChestCloser);
        MinecraftForge.EVENT_BUS.register(spiritBowLocator);
        SpiritBowLocator.markRegistered();
        MinecraftForge.EVENT_BUS.register(autoSprintController);
        MinecraftForge.EVENT_BUS.register(shortcutController);
        MinecraftForge.EVENT_BUS.register(wardrobeShortcutController);
        MinecraftForge.EVENT_BUS.register(commandKeybindController);
        MinecraftForge.EVENT_BUS.register(inventoryAssistController);

        // Retained friend-source render/QoL and dungeon solver features. Legacy SlotLocking and its old container
        // overlay intentionally remain inactive: Item Lock & Swap is the sole owner of the shared L-key gesture.
        MinecraftForge.EVENT_BUS.register(bloodMobHighlight);
        MinecraftForge.EVENT_BUS.register(mobHighlight);
        MinecraftForge.EVENT_BUS.register(witherDoorHighlight);
        MinecraftForge.EVENT_BUS.register(spiritBowTracer);
        MinecraftForge.EVENT_BUS.register(equipmentOverlay);
        MinecraftForge.EVENT_BUS.register(entityVisualCleanup);
        MinecraftForge.EVENT_BUS.register(removeIrritatingMessages);
        MinecraftForge.EVENT_BUS.register(itemProtection);
        MinecraftForge.EVENT_BUS.register(HudManager.DUNGEON_PROGRESS);
        MinecraftForge.EVENT_BUS.register(boulderSolver);
        MinecraftForge.EVENT_BUS.register(creeperSolver);
        MinecraftForge.EVENT_BUS.register(silverFishSolver);
        MinecraftForge.EVENT_BUS.register(teleportMazeSolver);
        MinecraftForge.EVENT_BUS.register(threeWeirdosSolver);
        MinecraftForge.EVENT_BUS.register(waterSolver);
        MinecraftForge.EVENT_BUS.register(firstDeviceSolver);
        MinecraftForge.EVENT_BUS.register(thirdDeviceSolver);
        MinecraftForge.EVENT_BUS.register(clickInOrderSolver);
        MinecraftForge.EVENT_BUS.register(clickOnColorsSolver);
        MinecraftForge.EVENT_BUS.register(correctPanesSolver);
        MinecraftForge.EVENT_BUS.register(hideTooltips);
        MinecraftForge.EVENT_BUS.register(mazeSolver);
        MinecraftForge.EVENT_BUS.register(startingWithSolver);
        MinecraftForge.EVENT_BUS.register(terminalTracker);
        MinecraftForge.EVENT_BUS.register(terminalDebug);
        MinecraftForge.EVENT_BUS.register(terminalGuiLayer);
        MinecraftForge.EVENT_BUS.register(terminalWaypoints);
    }

    /**
     * Rebinds the feature capture key and returns a human-readable collision warning, if another control uses it.
     * A collision is permitted (as Minecraft normally permits it) but is always made explicit in the hub/chat.
     */
    public static String rebindItemLockSwapKey(int keyCode) {
        // Escape is always handled by the capture owner as cancel. Zero is the deliberate Backspace-cleared state.
        if (!ItemLockSwapKeybind.validStoredKeyCode(keyCode) || keyCode == ItemLockSwapKeybind.ESCAPE) {
            return null;
        }
        String conflict = null;
        Minecraft mc = Minecraft.getMinecraft();
        if (keyCode > 0 && mc != null && mc.gameSettings != null && mc.gameSettings.keyBindings != null) {
            KeyBinding[] bindings = mc.gameSettings.keyBindings;
            for (int i = 0; i < bindings.length; i++) {
                KeyBinding binding = bindings[i];
                if (binding != null && binding != ITEM_LOCK_SWAP_KEY && binding.getKeyCode() == keyCode) {
                    conflict = binding.getKeyDescription();
                    break;
                }
            }
        }
        ITEM_LOCK_SWAP_KEY.setKeyCode(keyCode);
        ItemLockSwapSettings.keyCode = keyCode;
        KeyBinding.resetKeyBindingArrayAndHash();
        return conflict;
    }

    /** Same explicit capture contract as Item Lock: Escape is owned by the UI, and zero is an intentional clear. */
    public static String rebindWardrobeShortcutKey(int keyCode) {
        if (!AssistSettings.validShortcutKeyCode(keyCode) || keyCode == Keyboard.KEY_ESCAPE) return null;
        String conflict = rebindShortcutKey(WARDROBE_SHORTCUT_KEY, keyCode);
        AssistSettings.wardrobeShortcutKeyCode = keyCode;
        return conflict;
    }

    /** Pets uses the same persisted visible capture behavior as Wardrobe. */
    public static String rebindPetsShortcutKey(int keyCode) {
        if (!AssistSettings.validShortcutKeyCode(keyCode) || keyCode == Keyboard.KEY_ESCAPE) return null;
        String conflict = rebindShortcutKey(PETS_SHORTCUT_KEY, keyCode);
        AssistSettings.petsShortcutKeyCode = keyCode;
        return conflict;
    }

    /** Applies the documented W/P defaults only after the player explicitly chooses Shortcut reset. */
    public static void resetShortcutKeysToDefaults() {
        rebindWardrobeShortcutKey(AssistSettings.DEFAULT_WARDROBE_SHORTCUT_KEY_CODE);
        rebindPetsShortcutKey(AssistSettings.DEFAULT_PETS_SHORTCUT_KEY_CODE);
    }

    private static String rebindShortcutKey(KeyBinding own, int keyCode) {
        String conflict = null;
        Minecraft mc = Minecraft.getMinecraft();
        if (keyCode > 0 && mc != null && mc.gameSettings != null && mc.gameSettings.keyBindings != null) {
            KeyBinding[] bindings = mc.gameSettings.keyBindings;
            for (int i = 0; i < bindings.length; i++) {
                KeyBinding binding = bindings[i];
                if (binding != null && binding != own && binding.getKeyCode() == keyCode) {
                    conflict = binding.getKeyDescription();
                    break;
                }
            }
        }
        own.setKeyCode(keyCode);
        KeyBinding.resetKeyBindingArrayAndHash();
        return conflict;
    }

    /** Remembers a connection and installs the retained outgoing-packet bridge once Netty exposes packet_handler. */
    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (event == null || event.manager == null) return;
        pendingPacketBridgeManager = event.manager;
        packetBridgeWaitStartedAt = System.currentTimeMillis();
        packetBridgeWaitNoticeShown = false;
        installPacketBridgeIfReady();
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (event == null || event.manager == null || pendingPacketBridgeManager == event.manager) {
            pendingPacketBridgeManager = null;
        }
    }

    private void installPacketBridgeIfReady() {
        NetworkManager manager = pendingPacketBridgeManager;
        if (manager == null) return;
        try {
            if (manager.channel() == null) return;
            ChannelPipeline pipeline = manager.channel().pipeline();
            if (pipeline.get("lunaria_packet_handler") != null) {
                pendingPacketBridgeManager = null;
                return;
            }
            if (pipeline.get("packet_handler") == null) {
                if (!packetBridgeWaitNoticeShown && System.currentTimeMillis() - packetBridgeWaitStartedAt > 3000L) {
                    packetBridgeWaitNoticeShown = true;
                    System.err.println("[Lunaria] Waiting for packet_handler before enabling retained click guards.");
                }
                return;
            }
            pipeline.addBefore("packet_handler", "lunaria_packet_handler", new PacketHandler());
            pendingPacketBridgeManager = null;
            System.out.println("[Lunaria] Outbound packet bridge installed.");
        } catch (RuntimeException ex) {
            if (!packetBridgeWaitNoticeShown) {
                packetBridgeWaitNoticeShown = true;
                System.err.println("[Lunaria] Packet bridge installation will retry: " + ex.getMessage());
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        installPacketBridgeIfReady();
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            // Controls can rebind this vanilla KeyBinding outside our hub; mirror it into feature config for next launch.
            if (ItemLockSwapSettings.keyCode != ITEM_LOCK_SWAP_KEY.getKeyCode()) {
                ItemLockSwapSettings.keyCode = ITEM_LOCK_SWAP_KEY.getKeyCode();
            }
            if (AssistSettings.wardrobeShortcutKeyCode != WARDROBE_SHORTCUT_KEY.getKeyCode()) {
                AssistSettings.wardrobeShortcutKeyCode = WARDROBE_SHORTCUT_KEY.getKeyCode();
            }
            if (AssistSettings.petsShortcutKeyCode != PETS_SHORTCUT_KEY.getKeyCode()) {
                AssistSettings.petsShortcutKeyCode = PETS_SHORTCUT_KEY.getKeyCode();
            }
            reportItemAnimationWorldJoin(mc);
            if (!loadingShown && StartupTasks.hasWork()) {
                // Show the loading screen only while real startup tasks are pending. Shown once per launch.
                loadingShown = true;
                mc.displayGuiScreen(new LoadingScreen(mc.currentScreen));
            }
            // Before the world moves. Also resets mining counts on world change, respawn and disconnect.
            // Hurt Camera Shake is applied only at vanilla EntityRenderer's tilt amplitude; there is deliberately no
            // fallback that mutates hurtTime, because that would affect non-camera visuals.
            // Item Animation is woven into Minecraft's authoritative hand/equip methods at class-load time; it has
            // no per-tick installation retry and its live settings are read only from those renderer callbacks.
            HudManager.tick(mc);
            DungeonContext.tick(mc);
            dragonKill.tick(System.currentTimeMillis());
            return;
        }
        // Advances the finite right-click arm on the client tick only. Render sampling never shortens this window.
        aspectTeleportTransition.tick(mc);
        ClientActions.run(mc);
        if (OPEN_KEY.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new AppleSettingsScreen(null));
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
        while (CHAT_TAB_KEY.isPressed()) {
            if (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) {
                ChatChannelTabs.cycleNext();
            }
        }
        while (CHAT_EDITOR_KEY.isPressed()) {
            if (mc.currentScreen == null) {
                ClientActions.requestChatEditor();
            }
        }
    }

    /**
     * Samples an already-applied local position discontinuity immediately before vanilla's world render. The renderer
     * changes only the temporary camera translation used for this frame; it does not touch player position, input, or
     * packets.
     */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            aspectTeleportTransition.renderTick(Minecraft.getMinecraft());
        }
    }

    /**
     * Emits the real launcher/coremod state once per joined world. Persisted sliders alone are not evidence that
     * Forge accepted the transformer, so the client log and in-game chat show the concise Hook state.
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
        String line = "Lunaria " + version + " | Item Animation: /lunaria itemanimation | "
                + ItemAnimationHookStatus.getDisplay();
        System.out.println("[Lunaria] " + line);
        String color = ItemAnimationHookStatus.isActive() ? "\u00a7a" : "\u00a7c";
        mc.thePlayer.addChatMessage(new ChatComponentText(color + line));
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
            mc.thePlayer.addChatMessage(new ChatComponentText("\u00a7b[Lunaria] \u00a7f" + text));
        }
    }

    /** A left-click on a held Spirit Leap item triggers the leap, so the server opens the menu the GUI replaces. */
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        // Forge emits this same event for wheel and button input. Only opt-in gameplay wheel movement is cancelled:
        // GuiChat/inventory screens keep their own scroll paths, and number-key hotbar selection is keyboard input.
        if (event != null && HotbarScrollPolicy.shouldCancel(AppleSettings.disableHotbarScrolling, event.dwheel,
                mc.currentScreen != null)) {
            event.setCanceled(true);
            return;
        }
        // Observation only: retain the real right-click unchanged while arming the render-only Aspect transition.
        if (event != null && event.button == 1 && event.buttonstate && mc.currentScreen == null) {
            aspectTeleportTransition.observeUse(mc);
        }
        if (event == null || event.button != 0 || !event.buttonstate) {
            return;
        }
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

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        // The editor draws the elements itself, so skip the in-game copy while it is open. Aspect smoothing is
        // already applied inside the world render coordinate path; there is intentionally no HUD veil here.
        if (!(mc.currentScreen instanceof HudEditorScreen) && !(mc.currentScreen instanceof DungeonHudEditorScreen)
                && HudManager.visibleInGame()) {
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
            Minecraft.getMinecraft().displayGuiScreen(new AppleSettingsScreen(event.gui));
        }
    }

    /** Keeps vanilla GuiDownloadTerrain lifecycle alive while rendering Lunaria's post-mod terrain artwork over it. */
    @SubscribeEvent
    public void onTerrainScreenDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event != null && event.gui instanceof GuiDownloadTerrain) {
            LunariaDownloadingTerrainScreen.draw(event.gui.width, event.gui.height);
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
        // Earliest screen Lunaria controls: the first main menu, shown only while startup work is pending.
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
        // Do not rely on the 20-tick cache here: this is the exact moment a vanilla chest would be replaced. A
        // missing or ambiguous sidebar result is deliberately false, so the server's original screen stays visible.
        DungeonContext.Result dungeon = DungeonContext.evaluateNow(Minecraft.getMinecraft());
        if (dungeon.inDungeon && SpiritLeap.isLeapMenu(chest.inventorySlots, dungeon.inDungeon)) {
            event.gui = new LeapScreen(chest.inventorySlots);
        }
    }
}
