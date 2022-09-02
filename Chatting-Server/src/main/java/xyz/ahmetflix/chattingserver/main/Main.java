package xyz.ahmetflix.chattingserver.main;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.UserProfile;
import xyz.ahmetflix.chattingserver.UsersList;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class Main {

    public static boolean useJline = true;
    public static boolean useConsole = true;

    public static void main(String[] args) {
        Configurator.setRootLevel(Level.INFO);
        Server server = new Server(new File(".", "user-cache")) {
            @Override
            protected boolean init() throws IOException {
                this.setUsersList(new UsersList(this) {
                });
                this.getServerConnection().addEndpoint(InetAddress.getByName("127.0.0.1"), 3131);
                this.setMotd("test motd");
                return true;
            }

            @Override
            public ListenableFuture<Object> postToMainThread(Runnable var1) {
                return null;
            }

            @Override
            public boolean isMainThread() {
                return false;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void addChatMessage(String message) {

            }

            @Override
            public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
                return false;
            }
        };
        server.primaryThread.start();
        server.processQueue.add(new Runnable() {
            @Override
            public void run() {
                server.getUsersList().onUserJoin(server.getUsersList().attemptLogin(null, new UserProfile(null, "testuser"), "127.0.0.1"), "hoo");
            }
        });
    }

}
