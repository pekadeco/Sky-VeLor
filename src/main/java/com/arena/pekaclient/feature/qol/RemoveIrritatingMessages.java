package com.arena.pekaclient.feature.qol;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.feature.SkyblockContext;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Local-only chat cleanup matching the relevant Better NEF filter categories. */
public final class RemoveIrritatingMessages {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ClientChatReceivedEvent event) {
        if (event == null || event.message == null) return;
        String formatted = event.message.getFormattedText();
        String plain = event.message.getUnformattedText();
        if (ChatNoiseFilterPolicy.shouldHideDungeonDialogue(plain, DungeonContext.isInDungeonNow(),
                AppleSettings.muteDungeonBossCrowdMessages)) {
            event.setCanceled(true);
            return;
        }
        if (!AppleSettings.removeIrritatingMessagesEnabled) return;
        if (ChatNoiseFilterPolicy.shouldHide(formatted, plain, SkyblockContext.isInSkyblock(), false,
                AppleSettings.chatDisableWatchdogInfo, AppleSettings.chatDisableFriendJoin,
                AppleSettings.chatDisableZombieRareDrops, AppleSettings.chatDisableSellingRanks,
                AppleSettings.chatDisableServerAnnouncements, AppleSettings.chatDisableGameInvites, false)) {
            event.setCanceled(true);
        }
    }
}
