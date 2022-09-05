package xyz.ahmetflix.chattingserver.command;

import xyz.ahmetflix.chattingserver.ICommandListener;

public interface IAdminCommand {
    void notifyOperators(ICommandListener sender, ICommand command, int flags, String msgFormat, Object... msgParams);
}
