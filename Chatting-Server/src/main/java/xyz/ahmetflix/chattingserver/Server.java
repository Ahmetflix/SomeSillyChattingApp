package xyz.ahmetflix.chattingserver;

import com.google.common.collect.Lists;
import io.netty.util.ResourceLeakDetector;
import jline.console.ConsoleReader;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.connection.ServerConnection;
import xyz.ahmetflix.chattingserver.crash.CrashReport;
import xyz.ahmetflix.chattingserver.crash.ReportedException;
import xyz.ahmetflix.chattingserver.main.Main;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.FutureTask;

public abstract class Server implements Runnable, IAsyncTaskHandler, ICommandListener {

    public static final Logger LOGGER = LogManager.getLogger();
    private static Server instance;
    public static int currentTick = (int) (System.currentTimeMillis() / 50);
    private Thread serverThread;
    private ServerConnection serverConnection;
    private final UserCache userCache;
    public ConsoleReader reader;
    private UsersList usersList;
    private boolean isRunning = true;
    private boolean isStopped;
    private int ticks;
    protected final Queue<FutureTask<?>> jobs = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final List<ITickable> tickables = Lists.newArrayList();
    public final Thread primaryThread;

    private static final int TPS = 20;
    private static final int TICK_TIME = 1000000000 / TPS;
    private static final int SAMPLE_INTERVAL = 100;
    public final double[] recentTps = new double[ 3 ];

    public Server(File cacheFile) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

        instance = this;

        this.userCache = new UserCache(this, cacheFile);
        // this.commandDispatcher

        if (System.console() == null && System.getProperty("jline.terminal") == null) {
            System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
            Main.useJline = false;
        }

        try {
            reader = new ConsoleReader(System.in, System.out);
            reader.setExpandEvents(false);
        } catch (Throwable e) {
            try {
                System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
                System.setProperty("user.language", "en");
                Main.useJline = false;
                reader = new ConsoleReader(System.in, System.out);
                reader.setExpandEvents(false);
            } catch (IOException ex) {
                LOGGER.warn((String) null, ex);
            }
        }
        Runtime.getRuntime().addShutdownHook(new ServerShutdownThread(this));

        this.serverThread = primaryThread = new Thread(this, "Server thread");
    }

    public void run() {
        try {
            if (this.init()) {
                Arrays.fill( recentTps, 20 );
                long lastTick = System.nanoTime(), catchupTime = 0, curTime, wait, tickSection = lastTick;
                while (this.isRunning) {
                    curTime = System.nanoTime();
                    wait = TICK_TIME - (curTime - lastTick) - catchupTime;
                    if (wait > 0) {
                        Thread.sleep(wait / 1000000);
                        catchupTime = 0;
                        continue;
                    } else {
                        catchupTime = Math.min(1000000000, Math.abs(wait));
                    }

                    if ( currentTick++ % SAMPLE_INTERVAL == 0 )
                    {
                        double currentTps = 1E9 / ( curTime - tickSection ) * SAMPLE_INTERVAL;
                        recentTps[0] = calcTps( recentTps[0], 0.92, currentTps );
                        recentTps[1] = calcTps( recentTps[1], 0.9835, currentTps );
                        recentTps[2] = calcTps( recentTps[2], 0.9945, currentTps );
                        tickSection = curTime;
                    }
                    lastTick = curTime;

                    this.tick();
                }
            }
        } catch (Throwable throwable) {
            LOGGER.error("Encountered an unexpected exception", throwable);
            if ( throwable.getCause() != null )
            {
                LOGGER.error( "\tCause of unexpected exception was", throwable.getCause() );
            }

            CrashReport crashreport;

            if (throwable instanceof ReportedException) {
                crashreport = this.addServerInfoToCrashReport(((ReportedException) throwable).getCrashReport());
            } else {
                crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable));
            }

            File file = new File(new File(this.workspace(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (crashreport.saveToFile(file)) {
                LOGGER.error("This crash report has been saved to: " + file.getAbsolutePath());
            } else {
                LOGGER.error("We were unable to save this crash report to disk.");
            }
        } finally {
            try {
                this.isStopped = true;
                this.stop();
            } catch (Throwable throwable1) {
                LOGGER.error("Exception stopping the server", throwable1);
            } finally {
                try {
                    reader.getTerminal().restore();
                } catch (Exception ignored) {
                }
                System.exit(0);
            }

        }
    }

    protected abstract boolean init() throws IOException;

    protected void tick() {

    }

    public void heartbeat() {
        FutureTask<?> entry;
        int count = this.jobs.size();
        while (count-- > 0 && (entry = this.jobs.poll()) != null) {
            RunUtils.run(entry, LOGGER);
        }

        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }

        // tick server connection
        this.getServerConnection().tick();

        // tick users


        for (ITickable tickable : tickables) {
            tickable.update();
        }
    }

    private boolean hasStopped = false;
    private final Object stopLock = new Object();

    public void stop() {
        synchronized(stopLock) {
            if (hasStopped) return;
            hasStopped = true;
        }

        LOGGER.info("Stopping server");

        if (this.getServerConnection() != null) {
            this.getServerConnection().terminate();
        }

        LOGGER.info("Saving usercache.json");
        this.userCache.save();
    }

    public CrashReport addServerInfoToCrashReport(CrashReport report) {
        if (this.usersList != null)
        {
            report.getSystemDetails().addCrashSectionCallable("Player Count", () -> Server.this.usersList.getPlayerCount() + " / " + Server.this.usersList.getMaxPlayers() + "; " + Server.this.usersList.getUsers());
        }

        return report;

    }

    public File workspace() {
        return new File(".");
    }

    public ServerConnection getServerConnection() {
        return this.serverConnection == null ? this.serverConnection = new ServerConnection(this) : this.serverConnection;
    }

    public static Server getInstance() {
        return instance;
    }

    public static String getVersion() {
        return "1.0";
    }

    private static double calcTps(double avg, double exp, double tps)
    {
        return ( avg * exp ) + ( tps * ( 1 - exp ) );
    }
}
