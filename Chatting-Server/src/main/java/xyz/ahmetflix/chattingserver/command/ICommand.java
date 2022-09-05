package xyz.ahmetflix.chattingserver.command;

import xyz.ahmetflix.chattingserver.ICommandListener;

import java.util.List;

public interface ICommand extends Comparable<ICommand> {

    String getCommandName();

    String getCommandUsage(ICommandListener sender);

    List<String> getCommandAliases();

    void processCommand(ICommandListener sender, String[] args) throws CommandException;

    boolean canCommandSenderUseCommand(ICommandListener sender);

    List<String> addTabCompletionOptions(ICommandListener sender, String[] args);

    boolean isUsernameIndex(String[] args, int index);
}
