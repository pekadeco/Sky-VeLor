package com.arena.pekaclient.command;

import com.arena.pekaclient.config.AppleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Collections;
import java.util.List;

/** /qn (alias /quicknote) opens the quick note pad. Client-side only. */
public final class QuickNoteCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "qn";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("quicknote");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/qn (alias: /quicknote)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!AppleSettings.qnCommand) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    "\u00a7b[Sky VeLor] \u00a7f/qn is off. Turn it on in the hub, Quality of Life, Commands."));
            return;
        }
        ClientActions.requestQuickNotes();
    }
}
