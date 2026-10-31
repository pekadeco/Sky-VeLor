package com.arena.pekaclient.feature.mining;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.SkyblockContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Locale;

/** Tracks supported pickaxe ability cooldowns from local system chat. No messages or player data are uploaded. */
public final class MiningAbilityTracker {

    private static String abilityName;
    private static long readyAtMillis;
    private static boolean notificationPending;
    private static boolean connectedToFakepixel;

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!AppleSettings.miningAbilityNotifier || event == null || event.message == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || !SkyblockContext.isInSkyblock() || !isFakepixelServer(mc)) return;

        MiningAbilityCooldown.AbilityUse use = MiningAbilityCooldown.parseUseMessage(event.message.getUnformattedText());
        if (use == null) return;
        abilityName = use.name;
        readyAtMillis = System.currentTimeMillis() + use.cooldownSeconds * 1000L;
        notificationPending = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || !SkyblockContext.isInSkyblock() || !isFakepixelServer(mc)) {
            clear();
            return;
        }
        connectedToFakepixel = true;
        if (!AppleSettings.miningAbilityNotifier) {
            clearTrackedAbility();
            return;
        }
        if (!notificationPending || abilityName == null || System.currentTimeMillis() < readyAtMillis) return;
        notificationPending = false;
        if (mc.ingameGUI != null) {
            mc.ingameGUI.displayTitle(EnumChatFormatting.GOLD + "Mining Ability Ready", EnumChatFormatting.GRAY + abilityName, 2, 55, 2);
        }
    }

    public static String statusText() {
        return MiningAbilityCooldown.statusText(System.currentTimeMillis(), connectedToFakepixel,
                abilityName, readyAtMillis);
    }

    private static void clear() {
        clearTrackedAbility();
        connectedToFakepixel = false;
    }

    private static void clearTrackedAbility() {
        abilityName = null;
        readyAtMillis = 0L;
        notificationPending = false;
    }

    private static boolean isFakepixelServer(Minecraft mc) {
        if (mc == null) return false;
        ServerData server = mc.getCurrentServerData();
        return server != null && server.serverIP != null
                && server.serverIP.toLowerCase(Locale.ROOT).contains("fakepixel");
    }
}
