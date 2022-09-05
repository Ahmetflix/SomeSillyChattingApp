package xyz.ahmetflix.chattingserver.dedicated;

import joptsimple.OptionSet;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.ICommandListener;
import xyz.ahmetflix.chattingserver.PropertyManager;
import xyz.ahmetflix.chattingserver.WatchdogThread;
import xyz.ahmetflix.chattingserver.command.ServerCommand;
import xyz.ahmetflix.chattingserver.crpyt.CryptManager;
import xyz.ahmetflix.chattingserver.user.UsersList;
import xyz.ahmetflix.chattingserver.util.ForwardLogHandler;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.util.LoggerOutputStream;
import xyz.ahmetflix.chattingserver.util.TerminalConsoleWriterThread;
import xyz.ahmetflix.chattingserver.main.Main;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;

public class DedicatedServer extends Server {

    private static final Logger LOGGER = LogManager.getLogger();

    public PropertyManager propertyManager;
    private final java.util.Queue<ServerCommand> commandQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public DedicatedServer(OptionSet options, Thread thread) {
        super(options, userCacheFile, thread);
    }

    @Override
    protected boolean init() throws IOException {
        long j = System.nanoTime();
        Thread thread = new Thread("Server console handler") {
            @Override
            public void run() {
                if (!Main.useConsole) {
                    return;
                }

                jline.console.ConsoleReader bufferedreader = reader;
                String s;

                try {
                    while (!isStopped() && isRunning()) {
                        if (Main.useJline) {
                            s = bufferedreader.readLine(">", null);
                        } else {
                            s = bufferedreader.readLine();
                        }
                        if (s != null && s.trim().length() > 0) {
                            issueCommand(s, DedicatedServer.this);
                        }
                    }
                } catch (IOException ioexception) {
                    DedicatedServer.LOGGER.error("Exception handling console input", ioexception);
                }

            }
        };

        java.util.logging.Logger global = java.util.logging.Logger.getLogger("");
        global.setUseParentHandlers(false);
        for (java.util.logging.Handler handler : global.getHandlers()) {
            global.removeHandler(handler);
        }
        global.addHandler(new ForwardLogHandler());

        final org.apache.logging.log4j.core.Logger logger = ((org.apache.logging.log4j.core.Logger) LogManager
                .getRootLogger());
        for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
            if (appender instanceof org.apache.logging.log4j.core.appender.ConsoleAppender) {
                logger.removeAppender(appender);
            }
        }

        new Thread(new TerminalConsoleWriterThread(System.out, this.reader)).start();

        System.setOut(new PrintStream(new LoggerOutputStream(logger, Level.INFO), true));
        System.setErr(new PrintStream(new LoggerOutputStream(logger, Level.WARN), true));

        thread.setDaemon(true);
        thread.start();

        DedicatedServer.LOGGER.info("Starting server version "+getVersion());
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            DedicatedServer.LOGGER.warn(
                    "To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar server.jar\"");
        }

        DedicatedServer.LOGGER.info("Loading properties");
        this.propertyManager = new PropertyManager(this.options);

        this.setServerIp(this.propertyManager.getString("server-ip", ""));
        this.setMotd(this.propertyManager.getString("motd", "A Server"));

        InetAddress inetaddress = null;

        if (this.getServerIp().length() > 0) {
            inetaddress = InetAddress.getByName(this.getServerIp());
        }

        if (this.getPort() < 0) {
            this.setPort(this.propertyManager.getInt("server-port", 7700));
        }

        this.setUsersList(new DedicatedUserList(this));
        WatchdogThread.doStart(10);

        DedicatedServer.LOGGER.info("Generating keypair");
        this.setKeyPair(CryptManager.generateKeyPair());

        DedicatedServer.LOGGER.info("Starting server on "
                + (this.getServerIp().length() == 0 ? "*" : this.getServerIp()) + ":" + this.getPort());

        try {
            this.getServerConnection().addEndpoint(inetaddress, this.getPort());
        } catch (IOException ioexception) {
            DedicatedServer.LOGGER.warn("**** FAILED TO BIND TO PORT!");
            DedicatedServer.LOGGER.warn("The exception was: {}", ioexception.toString());
            DedicatedServer.LOGGER.warn("Perhaps a server is already running on that port?");
            return false;
        }

        long i1 = System.nanoTime() - j;
        String s3 = String.format("%.3fs", i1 / 1.0E9D);

        DedicatedServer.LOGGER.info("Done (" + s3 + ")! For help, type \"help\" or \"?\"");
        return true;
    }

    @Override
    public void heartbeat() {
        super.heartbeat();
        this.executeServerCommands();
    }

    public void issueCommand(String s, ICommandListener icommandlistener) {
        this.commandQueue.add(new ServerCommand(s, icommandlistener));
    }

    public void executeServerCommands() {
        ServerCommand servercommand;
        while ((servercommand = this.commandQueue.poll()) != null) {
            this.getCommandManager().executeCommand(servercommand.source, servercommand.command);
        }
    }

    public DedicatedUserList getDedicatedUserList() {
        return (DedicatedUserList) super.getUsersList();
    }

    @Override
    public UsersList getUsersList() {
        return this.getDedicatedUserList();
    }

    @Override
    public int getNetworkCompressionTreshold() {
        return this.propertyManager.getInt("network-compression-threshold", super.getNetworkCompressionTreshold());
    }

    @Override
    public boolean shouldBroadcastConsoleToOps() {
        return this.propertyManager.getBoolean("broadcast-console-to-ops", true);
    }

    public PropertyManager getPropertyManager() {
        return propertyManager;
    }
}
