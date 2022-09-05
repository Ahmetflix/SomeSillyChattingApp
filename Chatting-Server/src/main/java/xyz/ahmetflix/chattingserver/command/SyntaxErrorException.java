package xyz.ahmetflix.chattingserver.command;

public class SyntaxErrorException extends CommandException {

    public SyntaxErrorException(String message)
    {
        super(message);
    }
}
