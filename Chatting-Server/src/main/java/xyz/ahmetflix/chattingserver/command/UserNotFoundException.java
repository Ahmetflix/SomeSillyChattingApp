package xyz.ahmetflix.chattingserver.command;

public class UserNotFoundException extends CommandException {
    public UserNotFoundException(String message)
    {
        super(message);
    }
}
