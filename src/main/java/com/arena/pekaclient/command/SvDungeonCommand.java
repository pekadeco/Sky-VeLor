package com.arena.pekaclient.command;

import com.arena.pekaclient.assist.dungeon.DungeonClearTracker;
import com.arena.pekaclient.feature.DungeonContext;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Collections;
import java.util.List;

/**
 * Client-only dungeon diagnostics. It is deliberately independent of the hub toggle so a player can paste live
 * Fakepixel sidebar/tab formatting to support without opening any GUI or sending a server command.
 */
public final class SvDungeonCommand extends CommandBase {
    private static final String PREFIX = "\u00a7b[Lunaria] \u00a77";

    @Override
    public String getCommandName() {
        return "svdungeon";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("svd");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/luna dungeon debug";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args == null || args.length != 1 || !"debug".equalsIgnoreCase(args[0])) {
            say(sender, "Usage: /luna dungeon debug");
            return;
        }
        writeDebug(sender);
    }

    /** Shared by the Lunaria root command so diagnostic output cannot drift between aliases. */
    static void writeDebug(ICommandSender sender) {
        DungeonClearTracker.DebugSnapshot snapshot = DungeonClearTracker.INSTANCE.captureDebugSnapshot(
                Minecraft.getMinecraft());
        DungeonContext.Result context = snapshot.context;
        say(sender, "Dungeon debug begin -- copy all lines below");
        say(sender, "detected dungeon=" + value(context.dungeon) + ", floor=" + value(context.floor)
                + ", inDungeon=" + context.inDungeon);
        say(sender, "detector reason=" + value(context.reason) + ", evidence="
                + value(context.matchedScoreboardLine));
        say(sender, "raw sidebar title: " + raw(snapshot.sidebarTitle));
        sayRawList(sender, "raw sidebar", snapshot.sidebarLines);
        sayRawList(sender, "raw tab", snapshot.tabLines);
        say(sender, "raw sidebar cleared=" + number(snapshot.clearedPercent) + "%");
        say(sender, "Dungeon debug end");
    }

    private static void sayRawList(ICommandSender sender, String label, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            say(sender, label + ": <none>");
            return;
        }
        say(sender, label + " lines (" + lines.size() + "):");
        for (int i = 0; i < lines.size(); i++) {
            say(sender, label + "[" + i + "]: " + raw(lines.get(i)));
        }
    }

    private static String value(String text) {
        return text == null || text.trim().length() == 0 ? "--" : text;
    }

    private static String number(int value) {
        return value < 0 ? "--" : Integer.toString(value);
    }

    /** Keep formatting codes visible/pasteable rather than allowing a captured sidebar line to recolour chat. */
    private static String raw(String line) {
        String result = line == null || line.length() == 0 ? "<empty>" : line.replace('\u00a7', '&');
        return result.length() > 180 ? result.substring(0, 177) + "..." : result;
    }

    private static void say(ICommandSender sender, String line) {
        if (sender != null) sender.addChatMessage(new ChatComponentText(PREFIX + line));
    }
}
