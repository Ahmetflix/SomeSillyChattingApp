package xyz.ahmetflix.chattingclient;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import xyz.ahmetflix.chattingclient.packet.listeners.login.PacketLoginListener;
import xyz.ahmetflix.chattingclient.ping.TestPing;
import xyz.ahmetflix.chattingserver.user.UserProfile;
import xyz.ahmetflix.chattingserver.connection.EnumProtocol;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.impl.handshaking.PacketHandshakingInSetProtocol;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginInStart;

import java.io.File;
import java.net.*;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

    private static NetworkManager networkManager;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        OptionParser optionparser = new OptionParser();
        OptionSpec<String> server = optionparser.accepts("server").withRequiredArg();
        OptionSpec<Integer> port = optionparser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(Integer.valueOf(25565), new Integer[0]);
        OptionSpec<File> clientDir = optionparser.accepts("clientDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."), new File[0]);
        OptionSpec<String> username = optionparser.accepts("username").withRequiredArg().defaultsTo("User" + System.currentTimeMillis() % 1000L, new String[0]);
        OptionSet optionset = optionparser.parse(args);
        File clientFolder = optionset.valueOf(clientDir);
        String usernames = username.value(optionset);
        String serverIp = optionset.valueOf(server);
        int serverPort = optionset.valueOf(port);

        Thread.currentThread().setName("Client thread");
        (new Client(clientFolder, usernames, serverIp, serverPort)).run();

    }

}
