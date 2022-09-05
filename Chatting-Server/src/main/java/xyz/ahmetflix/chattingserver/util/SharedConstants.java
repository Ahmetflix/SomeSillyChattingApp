package xyz.ahmetflix.chattingserver.util;

public class SharedConstants {
    public static final char[] allowedCharacters = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};

    public static boolean isAllowedChatCharacter(char var0) {
        return var0 != 167 && var0 >= ' ' && var0 != 127;
    }
}
