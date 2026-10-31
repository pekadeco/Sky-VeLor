/* Adapted for Lunaria from code originating in NotEnoughFakepixel, through ii8we's private modifications. See LICENSE. */
package com.arena.pekaclient.dungeon.solver.terminals;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.Collections;
import java.util.List;

/** Opens the local terminal practice GUI without sending a command to the server. */
public final class TerminalSimulatorCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "termsim";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/termsim";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        // The chat box closes after the command and would close a screen opened here; the client tick opens it.
        com.arena.pekaclient.command.ClientActions.requestTerminalSimulator();
    }
}
