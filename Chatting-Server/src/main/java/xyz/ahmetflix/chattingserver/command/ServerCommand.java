package xyz.ahmetflix.chattingserver.command;

import xyz.ahmetflix.chattingserver.ICommandListener;

public class ServerCommand {
    public final String command;
    public final ICommandListener source;

    public ServerCommand(String command, ICommandListener source) {
        this.command = command;
        this.source = source;
    }
}
