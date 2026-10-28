package com.arena.pekaclient.assist.visual;

import com.arena.pekaclient.config.AssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Client-side render-only cleanup for dead living mobs and their nameplates.
 *
 * <p>Cancelling the normal living pre-render suppresses the model and its standard special render in one place.
 * The matching Specials hook is retained for renderers that dispatch a nameplate independently. Neither hook changes
 * an entity, a packet, a world collection, health, input, or server state.</p>
 */
public final class DeadMobRenderFilter {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @SuppressWarnings("rawtypes")
    public void onRenderLiving(RenderLivingEvent.Pre event) {
        if (event != null && shouldSuppress(event.entity)) event.setCanceled(true);
    }

    /** Explicitly covers nameplates on server NPCs/armor stands even when a renderer invokes specials separately. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @SuppressWarnings("rawtypes")
    public void onRenderLivingSpecials(RenderLivingEvent.Specials.Pre event) {
        if (event != null && shouldSuppress(event.entity)) event.setCanceled(true);
    }

    private boolean shouldSuppress(EntityLivingBase entity) {
        if (entity == null) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean localPlayer = minecraft != null && entity == minecraft.thePlayer;
        boolean playerEntity = entity instanceof EntityPlayer;
        return DeadMobRenderPolicy.shouldSuppress(AssistSettings.hideDeadMobs, localPlayer, playerEntity,
                isServerNpc(minecraft, entity), entity.isDead, entity.deathTime, entity.getHealth());
    }

    /**
     * Fakeplayer mobs can be EntityPlayer instances on SkyBlock/Fakepixel-style servers. A visible NPC marker is
     * enough; otherwise an entity absent from the ordinary network player list is treated as a server NPC. Real listed
     * players are never hidden by this visual cleanup.
     */
    private boolean isServerNpc(Minecraft minecraft, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return true;
        String displayName = entity.getDisplayName() == null ? "" : entity.getDisplayName().getUnformattedText();
        if (DeadMobRenderPolicy.hasNpcMarker(displayName)) return true;
        if (minecraft == null || minecraft.getNetHandler() == null) return false;
        NetworkPlayerInfo info = minecraft.getNetHandler().getPlayerInfo(entity.getUniqueID());
        return info == null;
    }
}
