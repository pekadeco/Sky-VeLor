package com.arena.pekaclient.command;

import com.arena.pekaclient.PekaClientMod;
import com.arena.pekaclient.input.MenuKeyBinding;
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
import net.minecraft.util.BlockPos;

import java.util.Collections;
import java.util.List;

/** /luna opens the Lunaria hub; prior full-name and VeLor commands remain compatibility aliases. */
public final class SvCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "luna";
    }

    @Override
    public List<String> getCommandAliases() {
        // These old public routes intentionally remain valid through the settings migration period.
        return java.util.Arrays.asList("lunaria", "velor");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/luna [resetkey|selftest|dungeon debug|itemanimation|ia|itemdebug|lockdebug|espdebug|espdump|chateditor|chatdebug|layoutdebug]";
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "resetkey", "selftest", "dungeon", "itemanimation", "ia",
                    "itemdebug", "lockdebug", "espdebug", "espdump", "chateditor", "chatdebug", "layoutdebug");
        }
        if (args.length == 2 && "dungeon".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "debug");
        }
        return Collections.emptyList();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args != null && args.length == 1 && "resetkey".equalsIgnoreCase(args[0])) {
            String conflict = PekaClientMod.rebindMenuKey(MenuKeyBinding.DEFAULT_KEY);
            say(sender, conflict == null ? "Open Menu Key reset to Right Shift."
                    : "Open Menu Key reset to Right Shift; also bound to " + conflict + ".");
            return;
        }
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
        // All names share the same command object and existing feature-route implementation.
        if (args != null && args.length == 1 && ItemDebugCommand.isFeatureRoute(args[0])) {
            new ItemDebugCommand().processCommand(sender, args);
            return;
        }
        if (args != null && args.length > 0) {
            say(sender, "Usage: " + getCommandUsage(sender));
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
