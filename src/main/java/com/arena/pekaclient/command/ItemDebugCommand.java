package com.arena.pekaclient.command;

import com.arena.pekaclient.feature.ItemAnimation;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHooks;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Client-side renderer diagnosis: {@code /velor itemdebug}. */
public final class ItemDebugCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "velor";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/velor itemdebug";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1 || !"itemdebug".equalsIgnoreCase(args[0])) {
            say(sender, "Usage: /velor itemdebug");
            return;
        }

        // These are the same live getters the ASM hand/equip callbacks call, not a saved-config snapshot.
        say(sender, String.format(Locale.ROOT,
                "Item Animation live renderer values: X=%+.2f, Y=%+.2f, Z=%+.2f, Size=%.2fx, Speed=%.2fx",
                ItemAnimation.getX(), ItemAnimation.getY(), ItemAnimation.getZ(),
                ItemAnimation.getSize(), ItemAnimation.getSpeed()));
        say(sender, "Hand hook " + hookState(ItemAnimationHooks.hasRenderHookRun(),
                ItemAnimationHooks.getRenderCalls(), ItemAnimationHooks.getLastRenderNanos())
                + "; equip hook " + hookState(ItemAnimationHooks.hasEquipHookRun(),
                ItemAnimationHooks.getEquipCalls(), ItemAnimationHooks.getLastEquipNanos()) + ".");
    }

    private static String hookState(boolean seen, long calls, long lastNanos) {
        if (!seen) {
            return "waiting for its Minecraft renderer callback";
        }
        long elapsed = Math.max(0L, (System.nanoTime() - lastNanos) / 1000000L);
        return "active (" + calls + " calls, " + elapsed + " ms ago)";
    }

    private static void say(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText("\u00a7b[Sky VeLor] \u00a7f" + text));
    }
}
