package xyz.ahmetflix.chattingserver.command;

import xyz.ahmetflix.chattingserver.ICommandListener;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.command.impl.CommandStop;
import xyz.ahmetflix.chattingserver.user.ChatUser;

public class CommandDispatcher extends CommandHandler implements IAdminCommand {

    public CommandDispatcher() {
        this.registerCommand(new CommandStop());
        CommandBase.setAdminCommander(this);
    }

    @Override
    public void notifyOperators(ICommandListener sender, ICommand command, int flags, String msgFormat, Object... msgParams) {
        Server server = Server.getInstance();

        String msg = "["+sender.getName()+": "+String.format(msgFormat, msgParams)+"]";

        for (ChatUser user : server.getUsersList().getUsers()) {
            if (user != sender && server.getUsersList().isOp(user.getProfile()) && command.canCommandSenderUseCommand(sender)) {
                boolean flag = sender instanceof Server && Server.getInstance().shouldBroadcastConsoleToOps();
                if (flag || !(sender instanceof Server)) {
                    user.addChatMessage(msg);
                }
            }
        }

        if (sender != server) {
            server.addChatMessage(msg);
        }

        sender.addChatMessage(String.format(msgFormat, msgParams));
    }
}
