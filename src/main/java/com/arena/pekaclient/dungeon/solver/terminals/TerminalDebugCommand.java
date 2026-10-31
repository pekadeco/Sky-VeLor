/* Maintained by ii8we for Lunaria. Review LICENSE for upstream attribution and applicable license terms. */
package com.arena.pekaclient.dungeon.solver.terminals;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.BlockPos;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Local-only controls for capturing and exporting Terminal GUI diagnostics. */
public final class TerminalDebugCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "termdebug";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/termdebug <on|off|status|dump|clear>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return Arrays.asList("on", "off", "status", "dump", "clear");
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if ("on".equals(action)) {
            TerminalDebug.setEnabled(true);
            reply(mc, "Terminal GUI debug is ON. Reproduce the issue, then run /termdebug dump.");
        } else if ("off".equals(action)) {
            TerminalDebug.setEnabled(false);
            reply(mc, "Terminal GUI debug is OFF. Recorded entries remain available to dump.");
        } else if ("status".equals(action)) {
            reply(mc, "Terminal GUI debug: " + (TerminalDebug.isEnabled() ? "ON" : "OFF")
                    + "; buffered entries: " + TerminalDebug.getEntryCount() + "/500.");
        } else if ("clear".equals(action)) {
            TerminalDebug.clear();
            reply(mc, "Terminal GUI diagnostic buffer cleared.");
        } else if ("dump".equals(action)) {
            try {
                File file = TerminalDebug.dump();
                reply(mc, "Terminal GUI log saved to: " + relativePath(mc, file));
            } catch (IOException ex) {
                reply(mc, "Could not write terminal log: " + safeMessage(ex));
            } catch (RuntimeException ex) {
                reply(mc, "Could not write terminal log: " + ex.getClass().getSimpleName());
            }
        } else {
            reply(mc, "Usage: /termdebug <on|off|status|dump|clear>");
        }
    }

    private static void reply(Minecraft mc, String message) {
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§b[Lunaria Debug] §f" + message));
        } else {
            System.out.println("[Lunaria Debug] " + message);
        }
    }

    private static String relativePath(Minecraft mc, File file) {
        if (mc != null && mc.mcDataDir != null) {
            String root = mc.mcDataDir.getAbsolutePath();
            String path = file.getAbsolutePath();
            if (path.startsWith(root)) return path.substring(root.length() + 1).replace('\\', '/');
        }
        return file.getPath().replace('\\', '/');
    }

    private static String safeMessage(IOException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage().replace('\n', ' ');
    }
}
