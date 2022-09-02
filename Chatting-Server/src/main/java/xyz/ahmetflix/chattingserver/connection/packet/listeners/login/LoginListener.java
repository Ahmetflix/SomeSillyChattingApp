package xyz.ahmetflix.chattingserver.connection.packet.listeners.login;

import com.google.common.base.Charsets;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.FastRandom;
import xyz.ahmetflix.chattingserver.ITickable;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.UserProfile;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginInStart;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutDisconnect;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutSetCompression;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutSuccess;
import xyz.ahmetflix.chattingserver.user.ChatUser;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginListener implements PacketLoginInListener, ITickable {

    private static final AtomicInteger threadId = new AtomicInteger(0);
    private static final java.util.concurrent.ExecutorService authenticatorPool = java.util.concurrent.Executors
            .newCachedThreadPool(r -> {
                        Thread thread = new Thread(r, "User Authenticator #" + threadId.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
            );
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new FastRandom();
    private final byte[] verifyToken = new byte[4];
    private final Server server;
    public final NetworkManager networkManager;
    private LoginState loginState;
    private int connectionTimer;
    private UserProfile loginGameProfile;
    private ChatUser user;
    public String hostname = "";

    public LoginListener(Server server, NetworkManager networkmanager) {
        this.loginState = LoginState.HELLO;
        this.server = server;
        this.networkManager = networkmanager;
        LoginListener.RANDOM.nextBytes(this.verifyToken);
    }

    @Override
    public void update() {
        if (this.loginState == LoginState.READY_TO_ACCEPT) {
            this.tryAcceptUser();
        } else if (this.loginState == LoginState.DELAY_ACCEPT) {
            ChatUser chatUser = this.server.getUsersList().getUserByUUID(this.loginGameProfile.getId());

            if (chatUser == null) {
                this.loginState = LoginState.READY_TO_ACCEPT;
                this.server.getUsersList().initializeConnection(this.networkManager, this.user);
                this.user = null;
            }
        }

        if (this.connectionTimer++ == 600) {
            this.disconnect("Took too long to log in");
        }

    }

    public void disconnect(String s) {
        if (s == null) {
            s = "Kicked with unknown reason";
        }

        try {
            LoginListener.LOGGER.info("Disconnecting " + this.getConnectionInfo() + ": " + s);

            this.networkManager.handle(new PacketLoginOutDisconnect(s));
            this.networkManager.close(s);
        } catch (Exception exception) {
            LoginListener.LOGGER.error("Error whilst disconnecting player", exception);
        }

    }

    public void initUUID() {
        UUID uuid = UUID.nameUUIDFromBytes(("ChatUser:" + this.loginGameProfile.getName()).getBytes(Charsets.UTF_8));;
        this.loginGameProfile = new UserProfile(uuid, this.loginGameProfile.getName());
    }

    public void tryAcceptUser() {
        ChatUser s = this.server.getUsersList().attemptLogin(this, this.loginGameProfile, hostname);

        if (s == null) {
        } else {
            this.loginState = LoginState.ACCEPTED;
            if (this.server.getNetworkCompressionTreshold() >= 0 && !this.networkManager.isLocalChannel()) {
                this.networkManager.handle(new PacketLoginOutSetCompression(this.server.getNetworkCompressionTreshold()), new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        LoginListener.this.networkManager.setCompressionTreshold(LoginListener.this.server.getNetworkCompressionTreshold());
                    }
                }, new GenericFutureListener[0]);
            }

            this.networkManager.handle(new PacketLoginOutSuccess(this.loginGameProfile));
            ChatUser chatUser = this.server.getUsersList().getUserByUUID(this.loginGameProfile.getId());

            if (chatUser != null) {
                this.loginState = LoginState.DELAY_ACCEPT;
                this.user = s;
            } else {
                this.server.getUsersList().initializeConnection(this.networkManager, s);
            }
        }

    }

    @Override
    public void onDisconnect(String reason) {
        LoginListener.LOGGER.info(this.getConnectionInfo() + " lost connection: " + reason);
    }

    public String getConnectionInfo() {
        String socketAddress = networkManager == null ? "null"
                : (networkManager.getSocketAddress() == null ? "null" : networkManager.getSocketAddress().toString());
        return this.loginGameProfile != null ? this.loginGameProfile.toString() + " (" + socketAddress + ")" : socketAddress;
    }

    @Override
    public void handleLoginStart(PacketLoginInStart packetlogininstart) {
        Validate.validState(this.loginState == LoginState.HELLO, "Unexpected hello packet");
        this.loginGameProfile = packetlogininstart.getUserProfile();

        authenticatorPool.execute(() -> {
            try {
                initUUID();
                LoginListener.LOGGER.info("UUID of player " + LoginListener.this.loginGameProfile.getName() + " is " + LoginListener.this.loginGameProfile.getId());
                LoginListener.this.loginState = LoginState.READY_TO_ACCEPT;
            } catch (Exception ex) {
                disconnect("Failed to verify username!");
                server.LOGGER.warn("Exception verifying " + loginGameProfile.getName(), ex);
            }
        });
    }

    protected UserProfile getProfile(UserProfile userProfile) {
        UUID uuid = UUID.nameUUIDFromBytes(("ChatUser:" + userProfile.getName()).getBytes(Charsets.UTF_8));

        return new UserProfile(uuid, userProfile.getName());
    }

    enum LoginState {

        HELLO, READY_TO_ACCEPT, DELAY_ACCEPT, ACCEPTED;
    }
}
