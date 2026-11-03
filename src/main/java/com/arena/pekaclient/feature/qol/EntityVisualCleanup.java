package com.arena.pekaclient.feature.qol;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.SkyblockContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Local render-only Flaming Fists cleanup retained from the friend build.
 * Hide Dead Mobs is owned by the newer DeadMobRenderFilter so both controls cannot compete.
 */
public final class EntityVisualCleanup {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderLiving(RenderLivingEvent.Pre<?> event) {
        if (event == null || event.entity == null) return;
        EntityLivingBase entity = event.entity;

        if (!AppleSettings.hideFlamingFists || !SkyblockContext.isInSkyblock()
                || !(entity instanceof EntityArmorStand)) return;

        EntityArmorStand armorStand = (EntityArmorStand) entity;
        ItemStack helmet = armorStand.getEquipmentInSlot(4);
        if (EntityVisualPolicy.isFlamingFistTexture(readSkullTexture(helmet))) {
            // Cancel render only; don't mark the entity invisible, so toggling the setting takes effect immediately.
            event.setCanceled(true);
        }
    }

    private static String readSkullTexture(ItemStack stack) {
        if (stack == null || stack.getItem() != Items.skull || !stack.hasTagCompound()) return "";
        NBTTagCompound root = stack.getTagCompound();
        if (!root.hasKey("SkullOwner", 10)) return "";
        NBTTagCompound owner = root.getCompoundTag("SkullOwner");
        if (!owner.hasKey("Properties", 10)) return "";
        NBTTagCompound properties = owner.getCompoundTag("Properties");
        if (!properties.hasKey("textures", 9)) return "";
        NBTTagList textures = properties.getTagList("textures", 10);
        if (textures.tagCount() == 0) return "";
        NBTTagCompound first = textures.getCompoundTagAt(0);
        return first.hasKey("Value", 8) ? first.getString("Value") : "";
    }
}
