package xyz.ahmetflix.chattingclient;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import xyz.ahmetflix.chattingclient.packet.listeners.login.PacketLoginListener;
import xyz.ahmetflix.chattingclient.ping.TestPing;
import xyz.ahmetflix.chattingserver.UserProfile;
import xyz.ahmetflix.chattingserver.connection.EnumProtocol;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.impl.handshaking.PacketHandshakingInSetProtocol;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginInStart;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

    private static final ThreadPoolExecutor field_148302_b = new ScheduledThreadPoolExecutor(5, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).build());
    private static NetworkManager networkManager;

    public static void main(String[] args) throws Exception {
        Configurator.setRootLevel(Level.INFO);
        InetAddress inetaddress = InetAddress.getByName("127.0.0.1");
        networkManager = TestPing.createNetworkManagerAndConnect(inetaddress, 3131, true);
        networkManager.setListener(new PacketLoginListener(networkManager, new Client()));
        networkManager.handle(new PacketHandshakingInSetProtocol("127.0.0.1", 3131, EnumProtocol.LOGIN));
        networkManager.handle(new PacketLoginInStart(new UserProfile(null, "gotseven")));
        int tickCount = 0;
        while (true) {
            if (tickCount % 1000 == 0) {
                try
                {
                    TestPing.ping("127.0.0.1", 3131);
                }
                catch (UnknownHostException var2)
                {
                    System.out.println("cant resolve hostname");
                }
                catch (Exception var3)
                {
                    System.out.println("cant connect server");
                }
            }
            if (networkManager != null)
            {
                if (networkManager.isChannelOpen())
                {
                    networkManager.tick();
                }
                else
                {
                    networkManager.checkDisconnected();
                    networkManager = null;
                }
            }
            TestPing.pingPendingNetworks();
            Thread.sleep(50);
            tickCount++;
        }

    }

}
