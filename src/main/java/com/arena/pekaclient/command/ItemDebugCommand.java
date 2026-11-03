package com.arena.pekaclient.command;

import com.arena.pekaclient.assist.dungeon.DungeonRunTracker;
import com.arena.pekaclient.assist.dungeon.SpiritBowLocator;
import com.arena.pekaclient.feature.ChatChannelTabs;
import com.arena.pekaclient.feature.ItemAnimation;
import com.arena.pekaclient.dungeonesp.DungeonEspController;
import com.arena.pekaclient.gui.GuiDebugOverlay;
import com.arena.pekaclient.itemlockswap.ItemLockSwapController;
import com.arena.pekaclient.itemlockswap.ItemLockSwapHookStatus;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHookStatus;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHooks;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Client-side feature launchers and compact renderer/runtime diagnostics. */
public final class ItemDebugCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "lunariadebug";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/lunaria itemanimation|ia|itemdebug|lockdebug|espdebug|espdump|selftest|chateditor|chatdebug|layoutdebug";
    }

    static boolean isFeatureRoute(String route) {
        if (route == null) return false;
        return "lockdebug".equalsIgnoreCase(route) || "espdebug".equalsIgnoreCase(route)
                || "espdump".equalsIgnoreCase(route) || "itemanimation".equalsIgnoreCase(route)
                || "ia".equalsIgnoreCase(route) || "chateditor".equalsIgnoreCase(route)
                || "chatdebug".equalsIgnoreCase(route) || "layoutdebug".equalsIgnoreCase(route)
                || "itemdebug".equalsIgnoreCase(route);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 1 && "lockdebug".equalsIgnoreCase(args[0])) {
            say(sender, "Item Lock / Swap: " + ItemLockSwapController.debugStatus());
            return;
        }
        if (args.length == 1 && "espdebug".equalsIgnoreCase(args[0])) {
            say(sender, DungeonEspController.debugStatus());
            return;
        }
        if (args.length == 1 && "espdump".equalsIgnoreCase(args[0])) {
            String[] dump = DungeonEspController.espDump();
            for (int i = 0; i < dump.length; i++) say(sender, dump[i]);
            return;
        }
        if (args.length == 1 && "selftest".equalsIgnoreCase(args[0])) {
            say(sender, "Item Lock / Swap hook: " + (ItemLockSwapHookStatus.isActive() ? "ACTIVE" : "FAILED")
                    + " (" + ItemLockSwapHookStatus.getReason() + ").");
            say(sender, "Dungeon Run HUD hook: " + DungeonRunTracker.selfTestStatus() + ".");
            say(sender, "Spirit Bow Locator hook: " + SpiritBowLocator.selfTestStatus() + ".");
            return;
        }
        if (args.length == 1 && ("itemanimation".equalsIgnoreCase(args[0])
                || "ia".equalsIgnoreCase(args[0]))) {
            // The next client tick opens the direct target after Minecraft has closed the chat screen.
            ClientActions.requestItemAnimation();
            return;
        }
        if (args.length == 1 && "chateditor".equalsIgnoreCase(args[0])) {
            ClientActions.requestChatEditor();
            return;
        }
        if (args.length == 1 && "chatdebug".equalsIgnoreCase(args[0])) {
            ChatChannelTabs.toggleDebug(sender);
            return;
        }
        if (args.length == 1 && "layoutdebug".equalsIgnoreCase(args[0])) {
            GuiDebugOverlay.toggle(sender);
            return;
        }
        if (args.length != 1 || !"itemdebug".equalsIgnoreCase(args[0])) {
            say(sender, "Usage: /lunaria itemanimation (or /lunaria ia), /lunaria itemdebug, /lunaria lockdebug, /lunaria espdebug, /lunaria espdump, /lunaria selftest, /lunaria chateditor, /lunaria chatdebug, or /lunaria layoutdebug");
            return;
        }

        String state = ItemAnimationHookStatus.isActive() ? "ACTIVE" : "FAILED";
        say(sender, "Item Animation: toggle=" + (ItemAnimation.isEnabled() ? "ON" : "OFF")
                + ", hook=" + state + " (" + ItemAnimationHookStatus.getReason() + ").");
        // These are the same effective live values read by the direct ItemRenderer callbacks, not a saved snapshot.
        say(sender, String.format(Locale.ROOT,
                "Live: X=%+.2f, Y=%+.2f, Z=%+.2f, Size=%.2fx, Speed=%.2fx, Smooth=%s %d%%",
                ItemAnimation.getX(), ItemAnimation.getY(), ItemAnimation.getZ(),
                ItemAnimation.getSize(), ItemAnimation.getSpeed(),
                ItemAnimation.isSmoothSwingEnabled() ? "ON" : "OFF", ItemAnimation.getSmoothness()));
        say(sender, String.format(Locale.ROOT,
                "Hook calls last second=%d, visual swing=%.3f, total hand calls=%d.",
                ItemAnimationHooks.getRenderCallsLastSecond(), ItemAnimationHooks.getVisualSwingProgress(),
                ItemAnimationHooks.getRenderCalls()));
    }

    private static void say(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText("\u00a7b[Lunaria] \u00a7f" + text));
    }
}
