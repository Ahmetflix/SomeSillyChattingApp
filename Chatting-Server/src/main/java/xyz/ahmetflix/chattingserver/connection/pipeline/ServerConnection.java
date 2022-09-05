package xyz.ahmetflix.chattingserver.connection.pipeline;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.util.LazyInitVar;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.ServerPipeline;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayOutKickDisconnect;
import xyz.ahmetflix.chattingserver.crash.CrashReport;
import xyz.ahmetflix.chattingserver.crash.CrashReportSystemDetails;
import xyz.ahmetflix.chattingserver.crash.ReportedException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ServerConnection {

    private static final WriteBufferWaterMark SERVER_WRITE_MARK = new WriteBufferWaterMark(1 << 20, 1 << 21);
    private static final Logger LOGGER = LogManager.getLogger();

    public static final LazyInitVar<NioEventLoopGroup> eventLoops = new LazyInitVar<NioEventLoopGroup>() {
        protected NioEventLoopGroup init() {
            return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Server IO #%d").setDaemon(true).build());
        }
    };

    public static final LazyInitVar<EpollEventLoopGroup> SERVER_EPOLL_EVENTLOOP = new LazyInitVar<EpollEventLoopGroup>() {
        protected EpollEventLoopGroup init() {
            return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyInitVar<LocalEventLoopGroup> SERVER_LOCAL_EVENTLOOP = new LazyInitVar<LocalEventLoopGroup>() {
        protected LocalEventLoopGroup init() {
            return new LocalEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Server IO #%d").setDaemon(true).build());
        }
    };

    private final Server server;
    public volatile boolean isAlive;
    private final List<ChannelFuture> endpoints = Collections.synchronizedList(Lists.<ChannelFuture>newArrayList());
    private final List<NetworkManager> networkManagers = Collections.synchronizedList(Lists.<NetworkManager>newArrayList());
    public final java.util.Queue<NetworkManager> pending = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private void addPending() {
        NetworkManager manager;
        while ((manager = pending.poll()) != null) {
            this.networkManagers.add(manager);
        }
    }

    public ServerConnection(Server server)
    {
        this.server = server;
        this.isAlive = true;
    }

    public void addEndpoint(InetAddress address, int port) throws IOException {
        synchronized (this.endpoints) {
            Class<? extends ServerChannel> socketClass;
            LazyInitVar<? extends EventLoopGroup> eventLoops;

            if (Epoll.isAvailable()) {
                socketClass = EpollServerSocketChannel.class;
                eventLoops = ServerConnection.SERVER_EPOLL_EVENTLOOP;
                LOGGER.info("Using epoll channel type");
            } else {
                socketClass = NioServerSocketChannel.class;
                eventLoops = ServerConnection.eventLoops;
                LOGGER.info("Using default channel type");
            }

            this.endpoints.add(((new ServerBootstrap().channel(socketClass)).childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, SERVER_WRITE_MARK).childHandler(new ServerPipeline(this)).group(eventLoops.get()).localAddress(address, port)).bind().syncUninterruptibly());
        }
    }

    public void terminate() {
        this.isAlive = false;

        for (ChannelFuture channelfuture : this.endpoints) {
            try {
                channelfuture.channel().close().sync();
            } catch (InterruptedException interruptedexception) {
                LOGGER.error("Interrupted whilst closing channel");
            } finally {
                eventLoops.get().shutdownGracefully();
            }
        }
    }

    public void tick() {
        synchronized (this.networkManagers) {
            this.addPending();
            Iterator<NetworkManager> iterator = this.networkManagers.iterator();
            while (iterator.hasNext())
            {
                final NetworkManager networkmanager = iterator.next();

                if (!networkmanager.hasNoChannel())
                {
                    if (!networkmanager.isChannelOpen())
                    {
                        if (networkmanager.preparing) continue;
                        iterator.remove();
                        networkmanager.checkDisconnected();
                    }
                    else
                    {
                        try
                        {
                            networkmanager.tick();
                        }
                        catch (Exception exception)
                        {
                            if (networkmanager.isLocalChannel())
                            {
                                CrashReport crashreport = CrashReport.makeCrashReport(exception, "Ticking memory connection");
                                CrashReportSystemDetails details = crashreport.makeDetails("Ticking connection");
                                details.addCrashSectionCallable("Connection", networkmanager::toString);
                                throw new ReportedException(crashreport);
                            }

                            LOGGER.warn((String)("Failed to handle packet for " + networkmanager.getSocketAddress()), (Throwable)exception);
                            String reason = "Internal server error";
                            networkmanager.handle(new PacketPlayOutKickDisconnect(reason), new GenericFutureListener<Future<? super Void>>() {
                                @Override
                                public void operationComplete(Future<? super Void> future) throws Exception {
                                    networkmanager.close(reason);
                                }
                            }, new GenericFutureListener[0]);
                            networkmanager.disableAutoRead();
                        }
                    }
                }
            }
        }
    }

    public Server getServer() {
        return server;
    }
}
