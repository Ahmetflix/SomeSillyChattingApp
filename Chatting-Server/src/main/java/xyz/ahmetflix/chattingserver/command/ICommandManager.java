package xyz.ahmetflix.chattingserver.command;

import xyz.ahmetflix.chattingserver.ICommandListener;

import java.util.List;
import java.util.Map;

public interface ICommandManager {

    int executeCommand(ICommandListener sender, String rawCommand);

    List<String> getTabCompletionOptions(ICommandListener sender, String input);

    List<ICommand> getPossibleCommands(ICommandListener sender);

    Map<String, ICommand> getCommands();

}
