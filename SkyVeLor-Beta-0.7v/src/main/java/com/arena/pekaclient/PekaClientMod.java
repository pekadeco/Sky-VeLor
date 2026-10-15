package com.arena.pekaclient;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.feature.LeapUse;
import com.arena.pekaclient.feature.SpiritLeap;
import com.arena.pekaclient.gui.AppleHubScreen;
import com.arena.pekaclient.gui.HudEditorScreen;
import com.arena.pekaclient.gui.LeapScreen;
import com.arena.pekaclient.gui.LoadingScreen;
import com.arena.pekaclient.hud.HudManager;
import com.arena.pekaclient.render.MobHighlight;
import com.arena.pekaclient.render.Theme;
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
 *  - a loading screen that runs Sky VeLor's own startup tasks.
 * All hooks are standard Forge 1.8.9 events. No reflection and no mixins. Entity rendering is not changed.
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

    /**
     * Use Spirit Leap: default unbound. Pressing it while holding a Spirit Leap item sends the same right-click
     * the game sends. The server still opens its own leap menu first, and only then can the teammate picker show.
     */
    public static final KeyBinding LEAP_KEY =
            new KeyBinding("key.pekaclient.leap", Keyboard.KEY_NONE, "key.categories.pekaclient");

    private boolean loadingShown;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Settings are read here so the first screen has them. The loading screen runs the rest.
        AppleSettings.load();
        Theme.refresh();
        ClientRegistry.registerKeyBinding(OPEN_KEY);
        ClientRegistry.registerKeyBinding(LEAP_KEY);
        StartupTasks.add("Reading the HUD layout", HudManager::ensureLoaded);
        StartupTasks.add("Applying the theme", Theme::refresh);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START) {
            if (!loadingShown && StartupTasks.hasWork()) {
                // Show the loading screen only while real startup tasks are pending. Shown once per launch.
                loadingShown = true;
                mc.displayGuiScreen(new LoadingScreen(mc.currentScreen));
            }
            // Before the world moves. Also resets mining counts on world change, respawn and disconnect.
            HudManager.tick(mc);
            DungeonContext.tick(mc);
            return;
        }
        if (OPEN_KEY.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new AppleHubScreen(null));
        }
        while (LEAP_KEY.isPressed()) {
            if (mc.currentScreen == null) {
                useSpiritLeap(mc);
            }
        }
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

    /** Records that the player right-clicked a Spirit Leap item, so the next chest can be recognised as its menu. */
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.button != 1 || !event.buttonstate) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.thePlayer == null || !DungeonContext.inDungeon()) {
            return;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held != null && LeapUse.isLeapItemName(held.getDisplayName())) {
            LeapUse.markUsed();
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
            event.buttonList.add(new GuiButton(MENU_BUTTON_ID, event.gui.width - 92, 8, 84, 20, NAME));
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
