package xyz.ahmetflix.chattingclient;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import xyz.ahmetflix.chattingclient.ping.TestPing;

import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

    private static final ThreadPoolExecutor field_148302_b = new ScheduledThreadPoolExecutor(5, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).build());

    public static void main(String[] args) throws Exception {
        Configurator.setRootLevel(Level.INFO);
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
            TestPing.pingPendingNetworks();
            Thread.sleep(50);
            tickCount++;
        }

    }

}
