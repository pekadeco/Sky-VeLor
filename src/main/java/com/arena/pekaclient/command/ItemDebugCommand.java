package com.arena.pekaclient.command;

import com.arena.pekaclient.feature.ItemAnimation;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHookStatus;
import com.arena.pekaclient.render.itemanimation.ItemAnimationHooks;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Client-side launcher/renderer diagnosis: {@code /velor itemdebug}. */
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

        String state = ItemAnimationHookStatus.isActive() ? "ACTIVE" : "FAILED";
        say(sender, "Item Animation: toggle=" + (ItemAnimation.isEnabled() ? "ON" : "OFF")
                + ", hook=" + state + " (" + ItemAnimationHookStatus.getReason() + ").");
        // These are the same effective live values read by the direct ItemRenderer callbacks, not a saved snapshot.
        say(sender, String.format(Locale.ROOT,
                "Live: X=%+.2f, Y=%+.2f, Z=%+.2f, Size=%.2fx, Speed=%.2fx",
                ItemAnimation.getX(), ItemAnimation.getY(), ItemAnimation.getZ(),
                ItemAnimation.getSize(), ItemAnimation.getSpeed()));
        say(sender, String.format(Locale.ROOT,
                "Hook calls last second=%d, visual swing=%.3f, total hand calls=%d.",
                ItemAnimationHooks.getRenderCallsLastSecond(), ItemAnimationHooks.getVisualSwingProgress(),
                ItemAnimationHooks.getRenderCalls()));
    }

    private static void say(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText("\u00a7b[Sky VeLor] \u00a7f" + text));
    }
}
