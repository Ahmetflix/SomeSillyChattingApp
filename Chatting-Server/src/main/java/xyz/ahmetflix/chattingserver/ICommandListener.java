package xyz.ahmetflix.chattingserver;

// TODO: add chat component & support
public interface ICommandListener {

    String getName();

    default String getDisplayName() { return getName(); };

    void addChatMessage(String message);

    boolean canCommandSenderUseCommand(int permLevel, String commandName);

}
