package com.arena.pekaclient.command;

import com.arena.pekaclient.config.AppleSettings;
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
        return Collections.emptyList();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/sv";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!AppleSettings.svCommand) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    "\u00a7b[Sky VeLor] \u00a7f/sv is off. Turn it on in the hub, QoL, Commands."));
            return;
        }
        ClientActions.requestHub();
    }
}
