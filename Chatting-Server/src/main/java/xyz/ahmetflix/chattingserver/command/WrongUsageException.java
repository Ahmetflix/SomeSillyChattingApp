package xyz.ahmetflix.chattingserver.command;

public class WrongUsageException extends SyntaxErrorException {
    public WrongUsageException(String message)
    {
        super(message);
    }
}
