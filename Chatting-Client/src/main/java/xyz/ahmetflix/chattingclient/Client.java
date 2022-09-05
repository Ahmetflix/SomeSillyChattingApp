package xyz.ahmetflix.chattingclient;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingclient.frame.ClientFrame;
import xyz.ahmetflix.chattingclient.frame.UserListBox;
import xyz.ahmetflix.chattingclient.packet.listeners.login.PacketLoginListener;
import xyz.ahmetflix.chattingclient.packet.listeners.play.ClientUserConnection;
import xyz.ahmetflix.chattingserver.IAsyncTaskHandler;
import xyz.ahmetflix.chattingserver.connection.EnumProtocol;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.impl.handshaking.PacketHandshakingInSetProtocol;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginInStart;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.UserConnection;
import xyz.ahmetflix.chattingserver.crash.CrashReport;
import xyz.ahmetflix.chattingserver.crash.ReportedException;
import xyz.ahmetflix.chattingserver.user.ChatUser;
import xyz.ahmetflix.chattingserver.user.UserProfile;
import xyz.ahmetflix.chattingserver.util.RunUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class Client implements IAsyncTaskHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    public static Client INSTANCE;
    public final File clientDataDir;
    public ClientChatUser user;
    public UserProfile profile;
    private String serverName;
    private int serverPort;
    private boolean hasCrashed;
    private CrashReport crashReporter;
    public static byte[] memoryReserve = new byte[10485760];
    volatile boolean running = true;
    public boolean isRunning() {
        return running;
    }

    private final Timer timer = new Timer(20.0F);
    private NetworkManager networkManager;
    private ServerData currentServerData;

    public Client(File clientFolder, String username, String sip, int port) {
        INSTANCE = this;
        clientDataDir = clientFolder;
        LOGGER.info("Setting user: "+ username);
        profile = UserProfile.makeUUIDIfNotExists(new UserProfile(null, username));

        if (sip != null) {
            this.serverName = sip;
            this.serverPort = port;
        }
    }

    public void run() {

        this.running = true;

        try {
            this.startClient();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Initializing game");
            crashreport.makeDetails("Initialization");
            this.displayCrashReport(crashreport);
            return;
        }

        while (true) {
            try {
                while (this.running) {
                    if (!this.hasCrashed || this.crashReporter == null) {
                        try {
                            this.runClientLoop();
                        } catch (OutOfMemoryError error) {
                            this.freeMemory();
                            JOptionPane.showMessageDialog(null, "NOT ENOUGH MEMORY", "OUT OF MEMORY", JOptionPane.ERROR_MESSAGE);
                            System.gc();
                        }
                    } else {
                            this.displayCrashReport(this.crashReporter);
                    }
                }
            } catch (ClientError e) {
                break;
            } catch (ReportedException e) {
                this.freeMemory();
                LOGGER.fatal((String)"Reported exception thrown!", e);
                this.displayCrashReport(e.getCrashReport());
                break;
            } catch (Throwable throwable1) {
                this.freeMemory();
                LOGGER.fatal("Unreported exception thrown!", throwable1);
                this.displayCrashReport(new CrashReport("Unexpected error", throwable1));
                break;
            } finally {
                this.shutdown();
            }

            return;
        }
    }

    private void startClient() throws IOException {
        //DiscordRPC.INSTANCE.start();
        this.startTimerHackThread();

        // start frame
        ClientFrame.createFrame(this);

        if (this.serverName != null) {
            this.networkManager = NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName(serverName), serverPort, true);
            this.networkManager.setListener(new PacketLoginListener(this.networkManager, this));
            this.networkManager.handle(new PacketHandshakingInSetProtocol(serverName, serverPort, EnumProtocol.LOGIN));
            this.networkManager.handle(new PacketLoginInStart(this.profile));
        } else {

        }
    }

    private void startTimerHackThread() {
        Thread thread = new Thread("Timer hack thread") {
            public void run() {
                while (Client.this.running) {
                    try {
                        Thread.sleep(2147483647L);
                    } catch (InterruptedException var2) {
                        ;
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    private void runClientLoop() throws IOException {
        this.timer.updateTimer();

        synchronized (this.scheduledTasks) {
            while (!this.scheduledTasks.isEmpty()) {
                RunUtils.run(this.scheduledTasks.poll(), LOGGER);
            }
        }

        if (UserListBox.getInstance() != null) {
            UserListBox.getInstance().update();
        }

        for (int j = 0; j < this.timer.elapsedTicks; ++j) {
            this.runTick();
        }
    }

    public void runTick() throws IOException {
        if (this.networkManager != null) {
            this.networkManager.tick();
        }
    }

    public void freeMemory() {
        try {
            memoryReserve = new byte[0];
            System.gc();
        } catch (Throwable var2) {
            ;
        }

        System.gc();
    }

    public void shutdown() {
        try {
            LOGGER.info("Stopping!");

            try {
                if (this.networkManager != null) {
                    this.networkManager.close("Terminate");
                }
            } catch (Throwable var5) {
                ;
            }
        } finally {
            if (!this.hasCrashed) {
                System.exit(0);
            }
        }

        System.gc();
    }

    public void displayCrashReport(CrashReport crashReportIn) {
        File crashReportDir = new File(clientDataDir, "crash-reports");
        File crashReportFile = new File(crashReportDir, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-client.txt");
        LOGGER.error(crashReportIn.getCompleteReport());

        if (crashReportIn.getReportFile() != null) {
            LOGGER.error("#@!@# Game crashed! Crash report saved to: #@!@# " + crashReportIn.getReportFile());
            System.exit(-1);
        } else if (crashReportIn.saveToFile(crashReportFile)) {
            LOGGER.error("#@!@# Game crashed! Crash report saved to: #@!@# " + crashReportFile.getAbsolutePath());
            System.exit(-1);
        } else {
            LOGGER.error("#@?@# Game crashed! Crash report could not be saved. #@?@#");
            System.exit(-2);
        }
    }

    public ClientUserConnection getConnection() {
        return this.user != null ? this.user.connection : null;
    }

    public void setCurrentServerData(ServerData currentServerData) {
        this.currentServerData = currentServerData;
    }

    public ServerData getCurrentServerData() {
        return currentServerData;
    }

    private final Queue<FutureTask<?>> scheduledTasks = Queues.newArrayDeque();
    private final Thread clientThread = Thread.currentThread();

    public <V> ListenableFuture<V> postToMainThread(Callable<V> callableToSchedule) {
        Validate.notNull(callableToSchedule);

        if (!this.isMainThread()) {
            ListenableFutureTask<V> listenablefuturetask = ListenableFutureTask.<V>create(callableToSchedule);

            synchronized (this.scheduledTasks) {
                this.scheduledTasks.add(listenablefuturetask);
                return listenablefuturetask;
            }
        } else {
            try {
                return Futures.<V>immediateFuture(callableToSchedule.call());
            } catch (Exception exception) {
                return Futures.immediateFailedCheckedFuture(exception);
            }
        }
    }

    public ListenableFuture<Object> postToMainThread(Runnable runnableToSchedule) {
        Validate.notNull(runnableToSchedule);
        return this.postToMainThread(Executors.callable(runnableToSchedule));
    }

    public boolean isMainThread() {
        return Thread.currentThread() == this.clientThread;
    }

}
