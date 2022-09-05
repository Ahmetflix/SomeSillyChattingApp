package xyz.ahmetflix.chattingserver.command;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import xyz.ahmetflix.chattingserver.ICommandListener;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.user.ChatUser;

import java.util.*;

public abstract class CommandBase implements ICommand
{
    private static IAdminCommand theAdmin;

    public int getRequiredPermissionLevel() {
        return 4;
    }

    public List<String> getCommandAliases() {
        return Collections.<String>emptyList();
    }

    public boolean canCommandSenderUseCommand(ICommandListener sender) {
        return sender.canCommandSenderUseCommand(this.getRequiredPermissionLevel(), this.getCommandName());
    }

    public List<String> addTabCompletionOptions(ICommandListener sender, String[] args) {
        return null;
    }

    public static int parseInt(String input) throws NumberInvalidException {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException var2) {
            throw new NumberInvalidException("Invalid number: " + input);
        }
    }

    public static int parseInt(String input, int min) throws NumberInvalidException {
        return parseInt(input, min, Integer.MAX_VALUE);
    }

    public static int parseInt(String input, int min, int max) throws NumberInvalidException {
        int i = parseInt(input);

        if (i < min) {
            throw new NumberInvalidException("Number "+ i +" is too small! Min: " + min);
        } else if (i > max) {
            throw new NumberInvalidException("Number "+ i +" is too big! Max: " + max);
        } else {
            return i;
        }
    }

    public static long parseLong(String input) throws NumberInvalidException {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException var2) {
            throw new NumberInvalidException("Invalid number: "+input);
        }
    }

    public static long parseLong(String input, long min, long max) throws NumberInvalidException {
        long i = parseLong(input);

        if (i < min) {
            throw new NumberInvalidException("Number "+ i +" is too small! Min: " + min);
        } else if (i > max) {
            throw new NumberInvalidException("Number "+ i +" is too big! Max: " + max);
        } else {
            return i;
        }
    }

    public static double parseDouble(String input) throws NumberInvalidException {
        try {
            double d0 = Double.parseDouble(input);

            if (!Doubles.isFinite(d0)) {
                throw new NumberInvalidException("Invalid number: "+input);
            } else {
                return d0;
            }
        } catch (NumberFormatException var3) {
            throw new NumberInvalidException("Invalid number: "+input);
        }
    }

    public static double parseDouble(String input, double min) throws NumberInvalidException {
        return parseDouble(input, min, Double.MAX_VALUE);
    }

    public static double parseDouble(String input, double min, double max) throws NumberInvalidException {
        double d0 = parseDouble(input);

        if (d0 < min) {
            throw new NumberInvalidException("Number "+ d0 +" is too small! Min: " + min);
        } else if (d0 > max) {
            throw new NumberInvalidException("Number "+ d0 +" is too big! Max: " + max);
        } else {
            return d0;
        }
    }

    public static boolean parseBoolean(String input) throws CommandException {
        if (!input.equals("true") && !input.equals("1")) {
            if (!input.equals("false") && !input.equals("0")) {
                throw new CommandException("Invalid boolean: "+input);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static ChatUser getCommandSenderAsUser(ICommandListener sender) throws UserNotFoundException {
        if (sender instanceof ChatUser) {
            return (ChatUser)sender;
        } else {
            throw new UserNotFoundException("You must specify which user you wish to perform this action on.");
        }
    }

    public static ChatUser getUser(ICommandListener sender, String username) throws UserNotFoundException {
        ChatUser chatUser = Server.getInstance().getUsersList().getUserByUUID(UUID.fromString(username));

        if (chatUser == null) {
            chatUser = Server.getInstance().getUsersList().getUser(username);
        }

        if (chatUser == null) {
            throw new UserNotFoundException("User not found.");
        } else {
            return chatUser;
        }
    }

    public static String getUserName(ICommandListener sender, String query) {
        try {
            return getUser(sender, query).getName();
        } catch (UserNotFoundException ex) {
            return query;
        }
    }

    public static String buildString(String[] args, int startPos) {
        StringBuilder stringbuilder = new StringBuilder();

        for (int i = startPos; i < args.length; ++i) {
            if (i > startPos) {
                stringbuilder.append(" ");
            }

            String s = args[i];
            stringbuilder.append(s);
        }

        return stringbuilder.toString();
    }

    public static double parseDouble(double base, String input, boolean centerBlock) throws NumberInvalidException
    {
        return parseDouble(base, input, -30000000, 30000000, centerBlock);
    }

    public static double parseDouble(double base, String input, int min, int max, boolean centerBlock) throws NumberInvalidException
    {
        boolean flag = input.startsWith("~");

        if (flag && Double.isNaN(base))
        {
            throw new NumberInvalidException("Invalid number: " + base);
        }
        else
        {
            double d0 = flag ? base : 0.0D;

            if (!flag || input.length() > 1)
            {
                boolean flag1 = input.contains(".");

                if (flag)
                {
                    input = input.substring(1);
                }

                d0 += parseDouble(input);

                if (!flag1 && !flag && centerBlock)
                {
                    d0 += 0.5D;
                }
            }

            if (min != 0 || max != 0)
            {
                if (d0 < (double)min)
                {
                    throw new NumberInvalidException("Number "+ d0 +" is too small! Min: " + min);
                }

                if (d0 > (double)max)
                {
                    throw new NumberInvalidException("Number "+ d0 +" is too big! Max: " + max);
                }
            }

            return d0;
        }
    }

    public static String joinNiceString(Object[] elements)
    {
        StringBuilder stringbuilder = new StringBuilder();

        for (int i = 0; i < elements.length; ++i)
        {
            String s = elements[i].toString();

            if (i > 0)
            {
                if (i == elements.length - 1)
                {
                    stringbuilder.append(" and ");
                }
                else
                {
                    stringbuilder.append(", ");
                }
            }

            stringbuilder.append(s);
        }

        return stringbuilder.toString();
    }

    public static String joinNiceStringFromCollection(Collection<String> strings) {
        return joinNiceString(strings.toArray(new String[strings.size()]));
    }

    public static boolean doesStringStartWith(String original, String region)
    {
        return region.regionMatches(true, 0, original, 0, original.length());
    }

    public static List<String> getListOfStringsMatchingLastWord(String[] args, String... possibilities)
    {
        return getListOfStringsMatchingLastWord(args, Arrays.asList(possibilities));
    }

    public static List<String> getListOfStringsMatchingLastWord(String[] args, Collection<?> possibilities)
    {
        String s = args[args.length - 1];
        List<String> list = Lists.<String>newArrayList();

        if (!possibilities.isEmpty())
        {
            for (String s1 : Iterables.transform(possibilities, Functions.toStringFunction()))
            {
                if (doesStringStartWith(s, s1))
                {
                    list.add(s1);
                }
            }
        }

        return list;
    }

    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    public static void notifyOperators(ICommandListener sender, ICommand command, String msgFormat, Object... msgParams) {
        notifyOperators(sender, command, 0, msgFormat, msgParams);
    }

    public static void notifyOperators(ICommandListener sender, ICommand command, int flags, String msgFormat, Object... msgParams) {
        if (theAdmin != null) {
            theAdmin.notifyOperators(sender, command, flags, msgFormat, msgParams);
        }
    }

    public static void setAdminCommander(IAdminCommand command) {
        theAdmin = command;
    }

    public int compareTo(ICommand compare) {
        return this.getCommandName().compareTo(compare.getCommandName());
    }
}
