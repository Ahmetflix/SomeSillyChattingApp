package xyz.ahmetflix.chattingserver.connection.packet.listeners.play;

import com.google.common.collect.Lists;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.ITickable;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.*;
import xyz.ahmetflix.chattingserver.crash.CrashReport;
import xyz.ahmetflix.chattingserver.crash.CrashReportSystemDetails;
import xyz.ahmetflix.chattingserver.crash.ReportedException;
import xyz.ahmetflix.chattingserver.user.ChatUser;
import xyz.ahmetflix.chattingserver.util.SharedConstants;
import xyz.ahmetflix.chattingserver.util.UserConnectionUtils;
import xyz.ahmetflix.chattingserver.util.Waitable;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class UserConnection implements PacketListenerPlayIn, ITickable {

    private static final Logger LOGGER = LogManager.getLogger();
    public final NetworkManager networkManager;
    private final Server server;
    public ChatUser user;

    public UserConnection(Server server, NetworkManager networkManager, ChatUser chatUser) {
        this.server = server;
        this.networkManager = networkManager;

        networkManager.setListener(this);
        this.user = chatUser;
        chatUser.connection = this;
    }

    @Override
    public void update() {
        final long currentTime = this.getCurrentMillis();
        final long elapsedTime = currentTime - this.getLastPing();
        if (isPendingPing) {
            if (!this.processedDisconnect && elapsedTime >= KEEPALIVE_LIMIT) {
                this.disconnect("Timed out");
                return;
            }
        } else if (elapsedTime >= 15000L) {
            isPendingPing = true;
            this.setLastPing(currentTime);
            this.setKeepAliveID((int) currentTime);
            this.sendPacket(new PacketPlayOutKeepAlive(this.getKeepAliveID()));
        }
    }

    public void disconnect(String reason) {
        this.networkManager.handle(new PacketPlayOutKickDisconnect(reason), new GenericFutureListener() {
            @Override
            public void operationComplete(Future future) throws Exception {
                UserConnection.this.networkManager.close(reason);
            }
        }, new GenericFutureListener[0]);
        this.onDisconnect(reason);
        this.networkManager.disableAutoRead();
        this.server.postToMainThread(new Runnable() {
            @Override
            public void run() {
                UserConnection.this.networkManager.checkDisconnected();
            }
        });
    }

    @Override
    public void onDisconnect(String reason) {
        if (this.processedDisconnect) {
            return;
        } else {
            this.processedDisconnect = true;
        }

        LOGGER.info(this.user.getName() + " lost connection: " + reason);
        String quitMessage = this.server.getUsersList().disconnect(this.user);
        if ((quitMessage != null) && (quitMessage.length() > 0)) {
            this.server.getUsersList().sendMessage(quitMessage);
        }
    }

    private volatile int chatThrottle;
    private static final AtomicIntegerFieldUpdater<UserConnection> chatSpamField = AtomicIntegerFieldUpdater
            .newUpdater(UserConnection.class, "chatThrottle");

    @Override
    public void handleChat(PacketPlayInChat packet) {
        boolean isSync = packet.getMessage().startsWith("/");
        if (isSync) {
            UserConnectionUtils.ensureMainThread(packet, this, this.server);
        }

        String message = packet.getMessage();
        message = StringUtils.normalizeSpace(message);

        for (int i = 0; i < message.length(); ++i) {
            if (!SharedConstants.isAllowedChatCharacter(message.charAt(i))) {
                if (!isSync) {
                    Waitable waitable = new Waitable() {
                        @Override
                        protected Object evaluate() {
                            UserConnection.this.disconnect("Illegal characters in chat");
                            return null;
                        }
                    };

                    this.server.processQueue.add(waitable);

                    try {
                        waitable.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    this.disconnect("Illegal characters in chat");
                }
                return;
            }
        }

        if (isSync) {
            this.handleCommand(message);
        } else if (message.isEmpty()) {
            LOGGER.warn(this.user.getName() + " tried to send an empty message");
        } else {
            this.chat(message, true);
        }

        if (chatSpamField.addAndGet(this, 20) > 200 && !this.server.getUsersList().isOp(this.user.getProfile())) {
            if (!isSync) {
                Waitable waitable = new Waitable() {
                    @Override
                    protected Object evaluate() {
                        UserConnection.this.disconnect("disconnect.spam");
                        return null;
                    }
                };

                this.server.processQueue.add(waitable);

                try {
                    waitable.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                this.disconnect("You have been disconnected because of spam!");
            }
        }

    }

    @Override
    public void handleTabComplete(PacketPlayInTabComplete packet) {
        UserConnectionUtils.ensureMainThread(packet, this, this.server);
        if (chatSpamField.addAndGet(this, 10) > 500 && !this.server.getUsersList().isOp(this.user.getProfile())) {
            this.disconnect("disconnect.spam");
            return;
        }

        List<String> completions = Lists.newArrayList(this.server.getTabCompletions(this.user, packet.getQuery()));

        this.user.connection.sendPacket(new PacketPlayOutTabComplete(completions.toArray(new String[0])));
    }

    @Override
    public void handleKeepAlive(PacketPlayInKeepAlive packet) {
        if (noKeepalives) {
            return;
        }
        if (isPendingPing && packet.getKeepAlive() == getKeepAliveID()) {
            int i = (int) (this.getCurrentMillis() - getLastPing());
            this.user.ping = (this.user.ping * 3 + i) / 4;
            isPendingPing = false;
        }  else {
            noKeepalives = true;
            LOGGER.warn("{} sent an invalid keepalive! pending keepalive: {} got id: {} expected id: {}",
                    this.user.getName(), isPendingPing, packet.getKeepAlive(), this.getKeepAliveID());
            this.server.postToMainThread(() -> disconnect("invalid keepalive"));
        }
    }

    public void chat(String s, boolean async) {
        if (s.isEmpty()) {
            return;
        }

        if (!async && s.startsWith("/")) {
            if (Thread.currentThread().equals(server.primaryThread)) {
                final String fCommandLine = s;
                Server.LOGGER.log(org.apache.logging.log4j.Level.ERROR, "Command Dispatched Async: " + fCommandLine);
                Waitable wait = new Waitable() {
                    @Override
                    protected Object evaluate() {
                        chat(fCommandLine, false);
                        return null;
                    }
                };

                server.processQueue.add(wait);

                try {
                    wait.get();
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    throw new RuntimeException("Exception processing chat command", e.getCause());
                }
            }
            this.handleCommand(s);
        }  else {

            s = String.format("<%1$s> %2$s", user.getName(), s);
            server.addChatMessage(s);
            for (ChatUser recipient : server.getUsersList().getUsers()) {
                recipient.addChatMessage(s);
            }
        }
    }

    private void handleCommand(String command) {
        LOGGER.info(this.user.getName() + " issued server command: " + command);

        this.server.getCommandManager().executeCommand(this.user, command);
    }

    public void sendPacket(final Packet packet) {
        if (packet == null || this.processedDisconnect) {
            return;
        }

        try {
            this.networkManager.handle(packet);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Sending packet");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.makeDetails("Packet being sent");

            crashreportsystemdetails.addCrashSectionCallable("Packet class", new Callable() {
                public String a() throws Exception {
                    return packet.getClass().getCanonicalName();
                }

                @Override
                public Object call() throws Exception {
                    return this.a();
                }
            });
            throw new ReportedException(crashreport);
        }
    }

    private boolean processedDisconnect;
    public static final long KEEPALIVE_LIMIT = 30000;
    private int keepAliveID;
    private long lastPing;
    private boolean isPendingPing;
    private boolean noKeepalives;

    private void setLastPing(final long lastPing) {
        this.lastPing = lastPing;
    }

    private long getLastPing() {
        return this.lastPing;
    }

    private void setKeepAliveID(final int keepAliveID) {
        this.keepAliveID = keepAliveID;
    }

    private int getKeepAliveID() {
        return this.keepAliveID;
    }

    private long getCurrentMillis() {
        return System.nanoTime() / 1000000L;
    }
}
