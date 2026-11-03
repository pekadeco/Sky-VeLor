package com.arena.pekaclient.itemlockswap;

import net.minecraft.entity.player.EntityPlayer;

/** Static entry points woven into vanilla methods. Object bridge parameters keep the injected descriptors stable. */
public final class ItemLockSwapHooks {

    private ItemLockSwapHooks() {
    }

    /** Called before PlayerControllerMP serializes any window-click packet. */
    public static boolean shouldBlockWindowClick(int windowId, int slotId, int mouseButton, int mode, Object player) {
        if (!(player instanceof EntityPlayer)) return false;
        EntityPlayer entity = (EntityPlayer) player;
        boolean lockSwapBlocked = ItemLockSwapProtection.shouldBlockWindowClick(windowId, slotId, mouseButton, mode, entity);
        if (lockSwapBlocked) {
            ItemLockSwapController.notifyBlocked("Item action blocked by Item Lock / Item Swap");
            return true;
        }
        return false;
    }

    /** Called at EntityPlayerSP.dropOneItem(boolean)'s entry, covering Q and Ctrl+Q on selected hotbar positions. */
    public static boolean shouldBlockHotbarDrop(Object player, boolean dropAll) {
        if (!(player instanceof EntityPlayer)) return false;
        EntityPlayer entity = (EntityPlayer) player;
        boolean lockSwapBlocked = ItemLockSwapProtection.shouldBlockSelectedHotbarDrop(entity, dropAll);
        if (lockSwapBlocked) {
            ItemLockSwapController.notifyBlocked(dropAll
                    ? "Item drop blocked by Item Lock / Item Swap (Ctrl+Q)"
                    : "Item drop blocked by Item Lock / Item Swap (Q)");
            return true;
        }
        return false;
    }
}
