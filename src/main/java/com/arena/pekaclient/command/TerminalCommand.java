package com.arena.pekaclient.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.Collections;
import java.util.List;

/** /terminal (alias /term) opens the 1-14 terminal. Client-side only. */
public final class TerminalCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "terminal";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("term");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/terminal (alias: /term)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        // Opened on the next tick: the chat screen closes after this command returns and would close ours.
        ClientActions.requestTerminal();
    }
}
