package com.arena.pekaclient.assist.guard;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.util.UUID;

/**
 * Opt-in UUID item guard. The only protected identities are UUIDs explicitly selected by the player with an
 * unbound key. Packet prevention is performed by the existing narrow PlayerController/EntityPlayer core hooks.
 */
public final class ItemGuardController {

    private static final String PREFIX = "\u00a7b[Sky VeLor] \u00a7f";
    private static ItemGuardController live;

    private final ItemGuardStore store = new ItemGuardStore();
    private long lastNoticeAt;

    public ItemGuardController() {
        live = this;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        ensureLoaded();
        while (PekaClientMod.ITEM_GUARD_KEY.isPressed()) {
            if (!AssistSettings.itemGuard) continue;
            toggleHeldItem();
        }
    }

    /** Called from the transformed PlayerControllerMP.windowClick before vanilla builds a packet. */
    public static boolean shouldBlockWindowClick(int slotId, int mode, EntityPlayer player) {
        ItemGuardController controller = live;
        if (controller == null || !AssistSettings.itemGuard || player == null) return false;
        controller.ensureLoaded();
        ItemStack stack = stackForClick(slotId, player);
        boolean protectedItem = controller.isProtected(stack);
        boolean blocked = ItemGuardPolicy.shouldBlockWindowAction(true, controller.store.isHealthy(), protectedItem,
                slotId, mode);
        if (blocked) controller.notice(controller.store.isHealthy()
                ? "Item Guard blocked a destructive item action"
                : "Item Guard storage is unavailable; destructive actions are blocked");
        return blocked;
    }

    /** Called from transformed EntityPlayerSP.dropOneItem(boolean) before vanilla emits Q/Ctrl+Q. */
    public static boolean shouldBlockHeldDrop(EntityPlayer player) {
        ItemGuardController controller = live;
        if (controller == null || !AssistSettings.itemGuard || player == null) return false;
        controller.ensureLoaded();
        boolean blocked = ItemGuardPolicy.shouldBlockHeldDrop(true, controller.store.isHealthy(),
                controller.isProtected(player.getHeldItem()));
        if (blocked) controller.notice(controller.store.isHealthy()
                ? "Item Guard blocked dropping a protected item"
                : "Item Guard storage is unavailable; destructive actions are blocked");
        return blocked;
    }

    public static String statusText() {
        ItemGuardController controller = live;
        if (controller == null) return "controller unavailable";
        controller.ensureLoaded();
        return controller.store.status();
    }

    public static boolean isActive() {
        return live != null && AssistSettings.itemGuard;
    }

    private void toggleHeldItem() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        UUID uuid = uuidOf(mc.thePlayer.getHeldItem());
        ItemGuardStore.Toggle result = store.toggle(uuid);
        switch (result) {
            case ADDED:
                noticeAlways("Item Guard: held item UUID protected locally");
                break;
            case REMOVED:
                noticeAlways("Item Guard: held item UUID no longer protected");
                break;
            case INVALID:
                noticeAlways("Item Guard: hold a SkyBlock item with a valid UUID");
                break;
            case UNAVAILABLE:
            default:
                noticeAlways("Item Guard: local storage is unavailable; no change was made");
                break;
        }
    }

    private boolean isProtected(ItemStack stack) {
        UUID uuid = uuidOf(stack);
        return uuid != null && store.contains(uuid);
    }

    private void ensureLoaded() {
        if (store.isLoaded()) return;
        Minecraft mc = Minecraft.getMinecraft();
        File dir = mc == null ? null : mc.mcDataDir;
        store.load(dir == null ? null : new File(dir, "skyvelor-protected-items.properties"));
    }

    private static ItemStack stackForClick(int slotId, EntityPlayer player) {
        if (slotId == -999) return player.inventory.getItemStack();
        Container container = player.openContainer;
        if (container == null || slotId < 0 || slotId >= container.inventorySlots.size()) return null;
        Slot slot = container.getSlot(slotId);
        return slot == null ? null : slot.getStack();
    }

    /** Strictly reads the SkyBlock ExtraAttributes UUID; a name, lore, or arbitrary NBT value is never guessed. */
    private static UUID uuidOf(ItemStack stack) {
        if (stack == null || stack.getTagCompound() == null) return null;
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey("ExtraAttributes", 10)) return null;
        NBTTagCompound attributes = root.getCompoundTag("ExtraAttributes");
        return attributes == null ? null : ItemIdentity.parse(attributes.getString("uuid"));
    }

    private void notice(String text) {
        if (!AssistSettings.itemGuardNotifications) return;
        noticeAlways(text);
    }

    private void noticeAlways(String text) {
        long now = System.currentTimeMillis();
        if (now - lastNoticeAt < 225L) return;
        lastNoticeAt = now;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(PREFIX + text));
    }
}
