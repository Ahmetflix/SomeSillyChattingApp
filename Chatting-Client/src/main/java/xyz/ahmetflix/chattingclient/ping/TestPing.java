package xyz.ahmetflix.chattingclient.ping;

import com.google.common.collect.Lists;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import xyz.ahmetflix.chattingserver.util.LazyInitVar;
import xyz.ahmetflix.chattingserver.user.UserProfile;
import xyz.ahmetflix.chattingserver.connection.EnumProtocol;
import xyz.ahmetflix.chattingserver.connection.EnumProtocolDirection;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.impl.handshaking.PacketHandshakingInSetProtocol;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusInPing;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusInStart;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusOutPong;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusOutServerInfo;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.status.PacketStatusOutListener;
import xyz.ahmetflix.chattingserver.connection.pipeline.PacketDecoder;
import xyz.ahmetflix.chattingserver.connection.pipeline.PacketEncoder;
import xyz.ahmetflix.chattingserver.connection.pipeline.PacketPrepender;
import xyz.ahmetflix.chattingserver.connection.pipeline.PacketSplitter;
import xyz.ahmetflix.chattingserver.status.ServerStatusResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static xyz.ahmetflix.chattingserver.connection.NetworkManager.CLIENT_EPOLL_EVENTLOOP;
import static xyz.ahmetflix.chattingserver.connection.NetworkManager.CLIENT_NIO_EVENTLOOP;

public class TestPing {

    private static final List<NetworkManager> pingDestinations = Collections.<NetworkManager>synchronizedList(Lists.<NetworkManager>newArrayList());

    public static void pingPendingNetworks()
    {
        synchronized (pingDestinations)
        {
            Iterator<NetworkManager> iterator = pingDestinations.iterator();

            while (iterator.hasNext())
            {
                NetworkManager networkmanager = (NetworkManager)iterator.next();

                if (networkmanager.isChannelOpen())
                {
                    networkmanager.tick();
                }
                else
                {
                    iterator.remove();
                    networkmanager.checkDisconnected();
                }
            }
        }
    }

    public static void ping(String serverIP, int port) throws UnknownHostException, InterruptedException {
        try
        {
            NetworkManager networkManager = createNetworkManagerAndConnect(InetAddress.getByName(serverIP), port, true);
            pingDestinations.add(networkManager);
            networkManager.setListener(new PacketStatusOutListener()
            {
                private boolean isPingSuccess = false;
                private boolean isRequested = false;
                private long ping = 0L;
                public void handleServerInfo(PacketStatusOutServerInfo packetIn) {
                    if (this.isRequested)
                    {
                        networkManager.close("Received unrequested status");
                    }
                    else
                    {
                        this.isRequested = true;
                        ServerStatusResponse serverstatusresponse = packetIn.getServerStatusResponse();

                        System.out.println(serverstatusresponse.getServerDescription());
                        System.out.println(serverstatusresponse.getPlayerCountData());


                        if (serverstatusresponse.getPlayerCountData() != null)
                        {
                            System.out.println(serverstatusresponse.getPlayerCountData().getOnlineUserCount() + "/" + serverstatusresponse.getPlayerCountData().getMaxUsers());

                            if (ArrayUtils.isNotEmpty(serverstatusresponse.getPlayerCountData().getUsers()))
                            {
                                StringBuilder stringbuilder = new StringBuilder();

                                for (UserProfile profile : serverstatusresponse.getPlayerCountData().getUsers())
                                {
                                    if (stringbuilder.length() > 0)
                                    {
                                        stringbuilder.append("\n");
                                    }

                                    stringbuilder.append(profile.getName());
                                }

                                if (serverstatusresponse.getPlayerCountData().getUsers().length < serverstatusresponse.getPlayerCountData().getOnlineUserCount())
                                {
                                    if (stringbuilder.length() > 0)
                                    {
                                        stringbuilder.append("\n");
                                    }

                                    stringbuilder.append("... and ").append(serverstatusresponse.getPlayerCountData().getOnlineUserCount() - serverstatusresponse.getPlayerCountData().getUsers().length).append(" more ...");
                                }

                                System.out.println(stringbuilder);
                            }
                        }

                        String s = serverstatusresponse.getFavicon() == null ? "data:image/png;base64," : serverstatusresponse.getFavicon();
                        String base64 = s.substring("data:image/png;base64,".length());
                        byte[] imageBytes = Base64.getDecoder().decode(base64);

                        try {
                            Files.write(new File("C:\\Users\\Ahmet\\Desktop\\favicon").toPath(), imageBytes, StandardOpenOption.WRITE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        this.ping = System.currentTimeMillis();
                        networkManager.handle(new PacketStatusInPing(this.ping));
                        this.isPingSuccess = true;
                    }
                }

                public void handlePong(PacketStatusOutPong packetIn)
                {
                    long i = this.ping;
                    long j = System.currentTimeMillis();
                    System.out.println("ping: " + (j - i));
                    networkManager.close("Finished");
                }

                @Override
                public void onDisconnect(String reason) {
                    if (!this.isPingSuccess)
                    {
                        LogManager.getLogger().error("Can\'t ping " + serverIP + ": " + reason);
                    }
                }
            });
            networkManager.handle(new PacketHandshakingInSetProtocol(serverIP, port, EnumProtocol.STATUS));
            networkManager.handle(new PacketStatusInStart());
        }
        catch (Throwable throwable)
        {
            LogManager.getLogger().error(throwable);
        }
    }

    public static NetworkManager createNetworkManagerAndConnect(InetAddress address, int serverPort, boolean useNativeTransport)
    {
        final NetworkManager networkmanager = new NetworkManager(EnumProtocolDirection.CLIENTBOUND);
        Class <? extends SocketChannel > oclass;
        LazyInitVar <? extends EventLoopGroup > lazyloadbase;

        if (Epoll.isAvailable() && useNativeTransport)
        {
            oclass = EpollSocketChannel.class;
            lazyloadbase = CLIENT_EPOLL_EVENTLOOP;
        }
        else
        {
            oclass = NioSocketChannel.class;
            lazyloadbase = CLIENT_NIO_EVENTLOOP;
        }

        ((Bootstrap)((Bootstrap)((Bootstrap)(new Bootstrap()).group((EventLoopGroup)lazyloadbase.get())).handler(new ChannelInitializer<Channel>()
        {
            protected void initChannel(Channel p_initChannel_1_) throws Exception
            {
                try
                {
                    p_initChannel_1_.config().setOption(ChannelOption.TCP_NODELAY, Boolean.valueOf(true));
                }
                catch (ChannelException var3)
                {
                    ;
                }

                p_initChannel_1_.pipeline().addLast((String)"timeout", (ChannelHandler)(new ReadTimeoutHandler(30)))
                        .addLast((String)"splitter", (ChannelHandler)(new PacketSplitter()))
                        .addLast((String)"decoder", (ChannelHandler)(new PacketDecoder(EnumProtocolDirection.CLIENTBOUND)))
                        .addLast((String)"prepender", (ChannelHandler)(PacketPrepender.INSTANCE))
                        .addLast((String)"encoder", (ChannelHandler)(new PacketEncoder(EnumProtocolDirection.SERVERBOUND)))
                        .addLast((String)"packet_handler", (ChannelHandler)networkmanager);
            }
        })).channel(oclass)).connect(address, serverPort).syncUninterruptibly();
        return networkmanager;
    }

}
