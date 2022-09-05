package xyz.ahmetflix.chattingserver.command.impl;

import xyz.ahmetflix.chattingserver.ICommandListener;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.command.CommandBase;
import xyz.ahmetflix.chattingserver.command.CommandException;

public class CommandStop extends CommandBase {

    public String getCommandName() {
        return "stop";
    }

    public String getCommandUsage(ICommandListener sender) {
        return "/stop";
    }

    public void processCommand(ICommandListener sender, String[] args) throws CommandException {
        notifyOperators(sender, this, "Stopping the server");

        Server.getInstance().safeShutdown();
    }
}
