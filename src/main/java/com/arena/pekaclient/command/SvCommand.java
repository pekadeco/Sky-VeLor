package com.arena.pekaclient.command;

import com.arena.pekaclient.config.AppleSettings;
import com.arena.pekaclient.assist.dungeon.DungeonTerminalAssistController;
import com.arena.pekaclient.assist.sync.DungeonSyncManager;
import com.arena.pekaclient.assist.visual.VisualAssistHookStatus;
import com.arena.pekaclient.chat.ChatClipboard;
import com.arena.pekaclient.feature.ChatChannelTabs;
import com.arena.pekaclient.feature.DungeonContext;
import com.arena.pekaclient.itemlockswap.ItemLockSwapHookStatus;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHookStatus;
import com.arena.pekaclient.render.blockparticles.BlockParticleHookStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Collections;
import java.util.List;

/** /lunaria opens the Lunaria hub from chat. Historic /sv and /velor routes remain client-side aliases. */
public final class SvCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "lunaria";
    }

    @Override
    public List<String> getCommandAliases() {
        // These old public routes intentionally remain valid through the settings migration period.
        return java.util.Arrays.asList("sv", "velor");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/lunaria [selftest|dungeon debug|itemanimation|itemdebug|lockdebug|espdebug|espdump|chateditor|chatdebug|layoutdebug]";
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
        if (args != null && args.length == 2 && "dungeon".equalsIgnoreCase(args[0])
                && "debug".equalsIgnoreCase(args[1])) {
            SvDungeonCommand.writeDebug(sender);
            return;
        }
        // Historic /velor feature routes now live under /lunaria too. The existing command class owns their
        // implementation so the diagnostic behavior stays byte-for-byte aligned with the retained path.
        if (args != null && args.length == 1 && ItemDebugCommand.isFeatureRoute(args[0])) {
            new ItemDebugCommand().processCommand(sender, args);
            return;
        }
        if (args != null && args.length > 0) {
            say(sender, "Usage: " + getCommandUsage(sender));
            return;
        }
        if (!AppleSettings.svCommand) {
            say(sender, "/lunaria is off. Turn it on in Settings, System, Diagnostics, Client command routes.");
            return;
        }
        ClientActions.requestHub();
    }

    /** Runtime hook report: it intentionally distinguishes a genuine unavailable service from a successful check. */
    private static void selfTest(Minecraft mc) {
        if (mc == null || mc.thePlayer == null) return;
        say(mc.thePlayer, "selftest");
        say(mc.thePlayer, "§7chat hook: §f" + ChatChannelTabs.selfTestStatus());
        say(mc.thePlayer, "§7clipboard: §f" + ChatClipboard.selfTestStatus());
        DungeonContext.Result dungeon = DungeonContext.evaluateNow(mc);
        String matched = dungeon.matchedScoreboardLine.length() == 0 ? "<none>" : dungeon.matchedScoreboardLine;
        say(mc.thePlayer, "§7dungeon: §finDungeon: " + dungeon.inDungeon
                + ", reason: " + dungeon.reason + ", matched scoreboard line: " + matched);
        say(mc.thePlayer, "§7item-animation: §f" + ItemAnimationHookStatus.getDisplay());
        say(mc.thePlayer, "§7block-particle hook: §f" + (BlockParticleHookStatus.isActive() ? "ACTIVE" : "FAILED")
                + " (" + BlockParticleHookStatus.getReason() + ")");
        say(mc.thePlayer, "§7item-lock/swap hook: §f"
                + (ItemLockSwapHookStatus.isActive() ? "ACTIVE" : "FAILED")
                + " (" + ItemLockSwapHookStatus.getReason() + ")");
        say(mc.thePlayer, "§7inventory tooltip hook: §f" + VisualAssistHookStatus.tooltipStatus());
        say(mc.thePlayer, "§7hurt camera shake: §f" + VisualAssistHookStatus.hurtCameraStatus());
        say(mc.thePlayer, "§7terminal assist: §f" + DungeonTerminalAssistController.selfTestStatus());
        say(mc.thePlayer, "§7dungeon sync: §f" + DungeonSyncManager.selfTestStatus());
    }

    private static void say(ICommandSender sender, String text) {
        if (sender != null) sender.addChatMessage(new ChatComponentText("§b[Lunaria] §f" + text));
    }
}
