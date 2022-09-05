package xyz.ahmetflix.chattingserver.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.ICommandListener;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandHandler implements ICommandManager {
    private static final Logger logger = LogManager.getLogger();
    private final Map<String, ICommand> commandMap = Maps.<String, ICommand>newHashMap();
    private final Set<ICommand> commandSet = Sets.<ICommand>newHashSet();

    public int executeCommand(ICommandListener sender, String rawCommand)
    {
        rawCommand = rawCommand.trim();

        if (rawCommand.startsWith("/"))
        {
            rawCommand = rawCommand.substring(1);
        }

        String[] astring = rawCommand.split(" ");
        String s = astring[0];
        astring = dropFirstString(astring);
        ICommand icommand = this.commandMap.get(s);
        int j = 0;

        if (icommand == null)
        {
            sender.addChatMessage("Command not found!");
        }
        else if (icommand.canCommandSenderUseCommand(sender))
        {
            if (this.tryExecute(sender, astring, icommand, rawCommand))
            {
                ++j;
            }
        }
        else
        {
            sender.addChatMessage("No permission!");
        }

        return j;
    }

    protected boolean tryExecute(ICommandListener sender, String[] args, ICommand command, String input)
    {
        try {
            command.processCommand(sender, args);
            return true;
        } catch (WrongUsageException wrongusageexception) {
            sender.addChatMessage("Wrong usage! "+ wrongusageexception.getMessage());
        } catch (CommandException commandexception) {
            sender.addChatMessage("Command exception! " + commandexception.getMessage());
        } catch (Throwable ex) {
            sender.addChatMessage("An internal error has occured! " + ex.getMessage());
            logger.warn("Couldn\'t process command: \'" + input + "\'");
        }

        return false;
    }

    public ICommand registerCommand(ICommand command)
    {
        this.commandMap.put(command.getCommandName(), command);
        this.commandSet.add(command);

        for (String s : command.getCommandAliases())
        {
            ICommand icommand = this.commandMap.get(s);

            if (icommand == null || !icommand.getCommandName().equals(s))
            {
                this.commandMap.put(s, command);
            }
        }

        return command;
    }

    private static String[] dropFirstString(String[] input)
    {
        String[] astring = new String[input.length - 1];
        System.arraycopy(input, 1, astring, 0, input.length - 1);
        return astring;
    }

    public List<String> getTabCompletionOptions(ICommandListener sender, String input)
    {
        String[] astring = input.split(" ", -1);
        String s = astring[0];

        if (astring.length == 1)
        {
            List<String> list = Lists.<String>newArrayList();

            for (Map.Entry<String, ICommand> entry : this.commandMap.entrySet())
            {
                if (CommandBase.doesStringStartWith(s, (String)entry.getKey()) && (entry.getValue()).canCommandSenderUseCommand(sender))
                {
                    list.add(entry.getKey());
                }
            }

            return list;
        }
        else
        {
            if (astring.length > 1)
            {
                ICommand icommand = (ICommand)this.commandMap.get(s);

                if (icommand != null && icommand.canCommandSenderUseCommand(sender))
                {
                    return icommand.addTabCompletionOptions(sender, dropFirstString(astring));
                }
            }

            return null;
        }
    }

    public List<ICommand> getPossibleCommands(ICommandListener sender)
    {
        List<ICommand> list = Lists.<ICommand>newArrayList();

        for (ICommand icommand : this.commandSet)
        {
            if (icommand.canCommandSenderUseCommand(sender))
            {
                list.add(icommand);
            }
        }

        return list;
    }

    public Map<String, ICommand> getCommands()
    {
        return this.commandMap;
    }

}
