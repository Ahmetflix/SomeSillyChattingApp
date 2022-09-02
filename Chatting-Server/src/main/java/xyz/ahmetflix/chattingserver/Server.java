package xyz.ahmetflix.chattingserver;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.ResourceLeakDetector;
import jline.console.ConsoleReader;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.connection.pipeline.ServerConnection;
import xyz.ahmetflix.chattingserver.crash.CrashReport;
import xyz.ahmetflix.chattingserver.crash.ReportedException;
import xyz.ahmetflix.chattingserver.main.Main;
import xyz.ahmetflix.chattingserver.status.ServerStatusResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Server implements Runnable, IAsyncTaskHandler, ICommandListener {

    public static final Logger LOGGER = LogManager.getLogger();
    private static Server instance;
    public static int currentTick = (int) (System.currentTimeMillis() / 50);
    private Thread serverThread;
    private ServerConnection serverConnection;
    private final ServerStatusResponse statusResponse = new ServerStatusResponse();
    private long nanoTimeSinceStatusRefresh = 0L;
    private String motd;
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
                // TODO: add this to init
                WatchdogThread.doStart(10);

                this.statusResponse.setServerDescription(this.motd);
                this.addFavicon(this.statusResponse);

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
                WatchdogThread.doStop();
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
        long nanos = System.nanoTime();

        ++this.ticks;
        this.heartbeat();
        if (nanos - this.nanoTimeSinceStatusRefresh >= 5000000000L) {
            this.nanoTimeSinceStatusRefresh = nanos;
            this.statusResponse.setPlayerCountData(new ServerStatusResponse.UserCountData(this.getMaxUsers(), this.getUserCount()));
            UserProfile[] userProfiles = new UserProfile[Math.min(this.getUserCount(), 12)];
            int j = nextInt(ThreadLocalRandom.current(), 0, this.getUserCount() - userProfiles.length);

            for (int k = 0; k < userProfiles.length; ++k) {
                userProfiles[k] = (this.usersList.getUsers().get(j + k)).getProfile();
            }

            Collections.shuffle(Arrays.asList(userProfiles));
            this.statusResponse.getPlayerCountData().setUsers(userProfiles);
        }

        WatchdogThread.tick();
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
            report.getSystemDetails().addCrashSectionCallable("Player Count", () -> Server.this.usersList.getUserCount() + " / " + Server.this.usersList.getMaxUsers() + "; " + Server.this.usersList.getUsers());
        }

        return report;

    }

    public File workspace() {
        return new File(".");
    }

    public ServerConnection getServerConnection() {
        return this.serverConnection == null ? this.serverConnection = new ServerConnection(this) : this.serverConnection;
    }

    public void addFavicon(ServerStatusResponse response) {
        File file = new File(workspace(), "server-icon.png");

        if (file.isFile()) {
            ByteBuf bytebuf = Unpooled.buffer();

            try {
                BufferedImage bufferedimage = ImageIO.read(file);

                Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide");
                Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high");
                ImageIO.write(bufferedimage, "PNG", new ByteBufOutputStream(bytebuf));
                ByteBuf bytebuf1 = Base64.encode(bytebuf);

                response.setFavicon("data:image/png;base64," + bytebuf1.toString(Charsets.UTF_8));
            } catch (Exception exception) {
                LOGGER.error("Couldn\'t load server icon", exception);
            } finally {
                bytebuf.release();
            }
        }
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public int getUserCount() {
        return this.usersList.getUserCount();
    }

    public int getMaxUsers() {
        return this.usersList.getMaxUsers();
    }

    public UsersList getUsersList() {
        return usersList;
    }

    public void setUsersList(UsersList usersList) {
        this.usersList = usersList;
    }

    public ServerStatusResponse getStatusResponse() {
        return statusResponse;
    }

    // TODO: move to any math class
    public static int nextInt(Random var0, int var1, int var2) {
        return var1 >= var2 ? var1 : var0.nextInt(var2 - var1 + 1) + var1;
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
