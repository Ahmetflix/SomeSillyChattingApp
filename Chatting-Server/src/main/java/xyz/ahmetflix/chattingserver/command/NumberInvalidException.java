package xyz.ahmetflix.chattingserver.command;

public class NumberInvalidException extends CommandException {

    public NumberInvalidException(String message) {
        super(message);
    }
}
