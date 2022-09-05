package xyz.ahmetflix.chattingserver;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.ResourceLeakDetector;
import jline.console.ConsoleReader;
import joptsimple.OptionSet;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.command.CommandBase;
import xyz.ahmetflix.chattingserver.command.CommandDispatcher;
import xyz.ahmetflix.chattingserver.command.ICommandManager;
import xyz.ahmetflix.chattingserver.connection.pipeline.ServerConnection;
import xyz.ahmetflix.chattingserver.crash.CrashReport;
import xyz.ahmetflix.chattingserver.crash.ReportedException;
import xyz.ahmetflix.chattingserver.main.Main;
import xyz.ahmetflix.chattingserver.status.ServerStatusResponse;
import xyz.ahmetflix.chattingserver.tickloop.ReentrantIAsyncHandler;
import xyz.ahmetflix.chattingserver.tickloop.TasksPerTick;
import xyz.ahmetflix.chattingserver.user.UserCache;
import xyz.ahmetflix.chattingserver.user.UserProfile;
import xyz.ahmetflix.chattingserver.user.UsersList;
import xyz.ahmetflix.chattingserver.util.RunUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

public abstract class Server extends ReentrantIAsyncHandler<TasksPerTick> implements Runnable, IAsyncTaskHandler, ICommandListener {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final File userCacheFile = new File("usercache.json");

    // Encrypt
    private KeyPair keyPair;
    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }
    public KeyPair getKeyPair() {
        return keyPair;
    }

    // Instance
    private static Server instance;

    // Threads
    private Thread serverThread;
    public final Thread primaryThread;

    // Netty server manager
    private ServerConnection serverConnection;
    public ServerConnection getServerConnection() {
        return this.serverConnection == null ? this.serverConnection = new ServerConnection(this) : this.serverConnection;
    }

    // Server status manager & status refresh count
    private final ServerStatusResponse statusResponse = new ServerStatusResponse();
    public ServerStatusResponse getStatusResponse() {
        return statusResponse;
    }
    private long nanoTimeSinceStatusRefresh = 0L;
    public void refreshStatusNextTick() {
        this.nanoTimeSinceStatusRefresh = 0L;
    }

    // Server description that shown in status
    private String motd;
    public String getMotd() {
        return motd;
    }
    public void setMotd(String motd) {
        this.motd = motd;
    }

    // users cache
    private final UserCache userCache;
    public UserCache getUserCache() {
        return userCache;
    }

    // users list
    private UsersList usersList;
    public UsersList getUsersList() {
        return usersList;
    }
    public void setUsersList(UsersList usersList) {
        this.usersList = usersList;
    }
    public int getUserCount() {
        return this.usersList.getUserCount();
    }
    public int getMaxUsers() {
        return this.usersList.getMaxUsers();
    }

    // jline console reader
    public ConsoleReader reader;

    // server state
    private boolean isRunning = true;
    public boolean isRunning() {
        return isRunning;
    }
    public void safeShutdown() {
        this.isRunning = false;
    }
    private boolean isStopped;
    public boolean isStopped() {
        return isStopped;
    }

    // runnable things
    protected final Queue<FutureTask<?>> jobs = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final List<ITickable> tickables = Lists.newArrayList();

    // program args
    public OptionSet options;

    // ip & port
    public String serverIp;
    public String getServerIp() {
        return serverIp;
    }
    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }
    public int port = -1;
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }

    // command
    protected final ICommandManager commandManager;
    public ICommandManager getCommandManager() {
        return commandManager;
    }
    protected CommandDispatcher createDispatcher() {
        return new CommandDispatcher();
    }

    public Server(OptionSet options, File cacheFile, Thread thread) {
        super("Server");

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

        instance = this;

        this.userCache = new UserCache(this, cacheFile);
        this.commandManager = this.createDispatcher();

        this.options = options;
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

        this.nextTickTime = getMillis();
        this.serverThread = thread;
        this.primaryThread = thread;
    }

    public void run() {
        try {
            serverStartTime = getNanos();
            if (this.init()) {
                this.statusResponse.setServerDescription(this.motd);
                this.addFavicon(this.statusResponse);

                Arrays.fill( recentTps, 20 );
                long start = System.nanoTime(), curTime, tickSection = start;
                lastTick = start - TICK_TIME;

                while (this.isRunning) {
                    long i = ((curTime = System.nanoTime()) / (1000L * 1000L)) - this.nextTickTime;
                    if (i > 5000L && this.nextTickTime - this.lastOverloadWarning >= 30000L && ticks > 500) {
                        long j = i / 50L;
                        LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);
                        this.nextTickTime += j * 50L;
                        this.lastOverloadWarning = this.nextTickTime;
                    }

                    if ( currentTick++ % SAMPLE_INTERVAL == 0 )
                    {
                        final long diff = curTime - tickSection;

                        java.math.BigDecimal currentTps = TPS_BASE.divide(new java.math.BigDecimal(diff), 30, java.math.RoundingMode.HALF_UP);

                        tps1.add(currentTps, diff);
                        tps5.add(currentTps, diff);
                        tps15.add(currentTps, diff);

                        recentTps[0] = tps1.getAverage();
                        recentTps[1] = tps5.getAverage();
                        recentTps[2] = tps15.getAverage();
                        tickSection = curTime;
                    }
                    lastTick = curTime;

                    this.nextTickTime += 50L;
                    this.tick();
                    this.mayHaveDelayedTasks = true;
                    this.delayedTasksMaxNextTickTime = Math.max(getMillis() + 50L, this.nextTickTime);
                    this.waitUntilNextTick();
                    this.isReady = true;
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

    protected void tick() {
        long nanos = getNanos();

        isOversleep = true;
        this.controlTerminate(() -> !this.canOversleep());
        isOversleep = false;

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

        long endTime = System.nanoTime();
        this.lastMspt = ((double) (endTime - lastTick) / 1000000D);

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

        this.getServerConnection().tick();

        this.getUsersList().tick();

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

    public List<String> getTabCompletions(ICommandListener sender, String input)
    {
        List<String> list = Lists.<String>newArrayList();

        if (input.startsWith("/")) {
            input = input.substring(1);
            boolean flag = !input.contains(" ");
            List<String> list1 = this.commandManager.getTabCompletionOptions(sender, input);

            if (list1 != null) {
                for (String s2 : list1) {
                    if (flag) {
                        list.add("/" + s2);
                    } else {
                        list.add(s2);
                    }
                }
            }
        } else {
            String[] astring = input.split(" ", -1);
            String s = astring[astring.length - 1];

            for (String s1 : this.getUsersList().usersNameArray()) {
                if (CommandBase.doesStringStartWith(s, s1)) {
                    list.add(s1);
                }
            }
        }

        return list;
    }

    public int getNetworkCompressionTreshold() {
        return 256;
    }

    public static long getMillis() {
        return getNanos() / 1000000L;
    }

    public static long getNanos() {
        return System.nanoTime();
    }

    @Override
    public String getName() {
        return "Server";
    }

    @Override
    public void addChatMessage(String message) {
        LOGGER.info(message);
    }

    @Override
    public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
        return true;
    }

    @Override
    public ListenableFuture<Object> postToMainThread(Runnable runnable) {
        Validate.notNull(runnable);
        return this.post(Executors.callable(runnable));
    }

    public <V> ListenableFuture<V> post(Callable<V> callable) {
        Validate.notNull(callable);
        if (!this.isMainThread()) {
            ListenableFutureTask<V> task = ListenableFutureTask.create(callable);

            this.jobs.add(task);
            return task;
        } else {
            try {
                return Futures.immediateFuture(callable.call());
            } catch (Exception exception) {
                return Futures.immediateFailedCheckedFuture(exception);
            }
        }
    }

    @Override
    public boolean isMainThread() {
        return Thread.currentThread() == this.serverThread;
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

    // TPS

    public static int currentTick = (int) (System.currentTimeMillis() / 50);
    private int ticks;
    private static final int TPS = 20;
    private static final long SEC_IN_NANO = 1000000000;
    private static final long TICK_TIME = SEC_IN_NANO / TPS;
    private long lastTick = 0;
    private static final int SAMPLE_INTERVAL = 20;
    public final RollingAverage tps1 = new RollingAverage(60);
    public final RollingAverage tps5 = new RollingAverage(60 * 5);
    public final RollingAverage tps15 = new RollingAverage(60 * 15);
    public double[] recentTps = new double[3];

    public static class RollingAverage {
        private final int size;
        private long time;
        private java.math.BigDecimal total;
        private int index = 0;
        private final java.math.BigDecimal[] samples;
        private final long[] times;

        RollingAverage(int size) {
            this.size = size;
            this.time = size * SEC_IN_NANO;
            this.total = dec(TPS).multiply(dec(SEC_IN_NANO)).multiply(dec(size));
            this.samples = new java.math.BigDecimal[size];
            this.times = new long[size];
            for (int i = 0; i < size; i++) {
                this.samples[i] = dec(TPS);
                this.times[i] = SEC_IN_NANO;
            }
        }

        private static java.math.BigDecimal dec(long t) {
            return new java.math.BigDecimal(t);
        }
        public void add(java.math.BigDecimal x, long t) {
            time -= times[index];
            total = total.subtract(samples[index].multiply(dec(times[index])));
            samples[index] = x;
            times[index] = t;
            time += t;
            total = total.add(x.multiply(dec(t)));
            if (++index == size) {
                index = 0;
            }
        }

        public double getAverage() {
            return total.divide(dec(time), 30, java.math.RoundingMode.HALF_UP).doubleValue();
        }
    }
    private static final java.math.BigDecimal TPS_BASE = new java.math.BigDecimal(1E9).multiply(new java.math.BigDecimal(SAMPLE_INTERVAL));
    private double lastMspt;
    private long nextTickTime;
    private long delayedTasksMaxNextTickTime;
    private boolean mayHaveDelayedTasks;
    private boolean forceTicks;
    private volatile boolean isReady;
    private long lastOverloadWarning;
    public long serverStartTime;
    public volatile Thread shutdownThread;

    public static <S extends Server> S spin(Function<Thread, S> serverFactory) {
        AtomicReference<S> reference = new AtomicReference<>();
        Thread thread = new Thread(() -> reference.get().run(), "Server thread");

        thread.setUncaughtExceptionHandler((thread1, throwable) -> LOGGER.error(throwable));
        S server = serverFactory.apply(thread);

        reference.set(server);
        thread.setPriority(Thread.NORM_PRIORITY + 2);
        thread.start();
        return server;
    }

    private boolean haveTime() {
        if (isOversleep) return canOversleep();
        return this.forceTicks || this.runningTask() || getMillis() < (this.mayHaveDelayedTasks ? this.delayedTasksMaxNextTickTime : this.nextTickTime);
    }

    boolean isOversleep = false;
    private boolean canOversleep() {
        return this.mayHaveDelayedTasks && getMillis() < this.delayedTasksMaxNextTickTime;
    }

    private boolean canSleepForTickNoOversleep() {
        return this.forceTicks || this.runningTask() || getMillis() < this.nextTickTime;
    }

    private void executeModerately() {
        this.runAllRunnable();
        LockSupport.parkNanos("executing tasks", 1000L);
    }

    protected void waitUntilNextTick() {
        this.controlTerminate(() -> !this.canSleepForTickNoOversleep());
    }

    @Override
    protected TasksPerTick packUpRunnable(Runnable runnable) {
        if (this.hasStopped && Thread.currentThread().equals(shutdownThread)) {
            runnable.run();
            runnable = () -> {};
        }
        return new TasksPerTick(this.ticks, runnable);
    }

    @Override
    protected boolean shouldRun(TasksPerTick task) {
        return task.getTick() + 3 < this.ticks || this.haveTime();
    }

    @Override
    public boolean drawRunnable() {
        boolean flag = this.pollTaskInternal();

        this.mayHaveDelayedTasks = flag;
        return flag;
    }

    private boolean pollTaskInternal() {
        return super.drawRunnable();
    }

    @Override
    public Thread getMainThread() {
        return serverThread;
    }

    public abstract PropertyManager getPropertyManager();

    protected abstract boolean init() throws IOException;

    public abstract boolean shouldBroadcastConsoleToOps();
}
