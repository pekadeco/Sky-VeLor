package com.arena.pekaclient.command;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.chat.ChatClipboard;
import com.arena.pekaclient.feature.ChatChannelTabs;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHookStatus;
import com.arena.pekaclient.render.blockparticles.BlockParticleHookStatus;
import com.arena.pekaclient.render.GlobalFontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Collections;
import java.util.List;

/** /sv opens the Sky VeLor hub from chat. Client-side only; nothing is sent to the server. */
public final class SvCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "sv";
    }

    @Override
    public List<String> getCommandAliases() {
        return java.util.Arrays.asList("velor");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/sv [selftest]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (args != null && args.length == 1 && "selftest".equalsIgnoreCase(args[0])) {
            selfTest(mc);
            return;
        }
        if (!AppleSettings.svCommand) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    "\u00a7b[Sky VeLor] \u00a7f/sv is off. Turn it on in the hub, Quality of Life, Commands."));
            return;
        }
        ClientActions.requestHub();
    }

    /** Runtime hook report: it intentionally distinguishes a genuine unavailable service from a successful check. */
    private static void selfTest(Minecraft mc) {
        if (mc == null || mc.thePlayer == null) return;
        mc.thePlayer.addChatMessage(new ChatComponentText("§b[Sky VeLor] §fselftest"));
        mc.thePlayer.addChatMessage(new ChatComponentText("§7chat hook: §f" + ChatChannelTabs.selfTestStatus()));
        mc.thePlayer.addChatMessage(new ChatComponentText("§7font hook: §f" + GlobalFontManager.selfTestStatus()));
        mc.thePlayer.addChatMessage(new ChatComponentText("§7clipboard: §f" + ChatClipboard.selfTestStatus()));
        DungeonContext.Result dungeon = DungeonContext.evaluateNow(mc);
        String matched = dungeon.matchedScoreboardLine.length() == 0 ? "<none>" : dungeon.matchedScoreboardLine;
        mc.thePlayer.addChatMessage(new ChatComponentText("§7dungeon: §finDungeon: " + dungeon.inDungeon
                + ", reason: " + dungeon.reason + ", matched scoreboard line: " + matched));
        mc.thePlayer.addChatMessage(new ChatComponentText("§7item-animation hook: §f" + (ItemAnimationHookStatus.isActive() ? "ACTIVE" : "FAILED") + " (" + ItemAnimationHookStatus.getReason() + ")"));
        mc.thePlayer.addChatMessage(new ChatComponentText("§7block-particle hook: §f" + (BlockParticleHookStatus.isActive() ? "ACTIVE" : "FAILED")
                + " (" + BlockParticleHookStatus.getReason() + ")"));
    }
}
