package xyz.ahmetflix.chattingserver.connection;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.AbstractEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import xyz.ahmetflix.chattingserver.ITickable;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.UserConnection;
import xyz.ahmetflix.chattingserver.connection.pipeline.*;
import xyz.ahmetflix.chattingserver.user.ChatUser;
import xyz.ahmetflix.chattingserver.util.CryptException;
import xyz.ahmetflix.chattingserver.util.LazyInitVar;
import xyz.ahmetflix.chattingserver.connection.packet.CancelledPacketHandleException;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayOutKeepAlive;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.PacketPlayOutKickDisconnect;

import javax.crypto.SecretKey;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NetworkManager extends SimpleChannelInboundHandler<Packet> {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final Marker networkMarker = MarkerManager.getMarker("NETWORK");
    public static final Marker networkPacketsMarker = MarkerManager.getMarker("NETWORK_PACKETS").addParents(networkMarker);
    public static final AttributeKey<EnumProtocol> protocolAttributeKey = AttributeKey.valueOf("protocol");
    public static final LazyInitVar<NioEventLoopGroup> CLIENT_NIO_EVENTLOOP = new LazyInitVar<>() {
        protected NioEventLoopGroup init() {
            return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyInitVar<EpollEventLoopGroup> CLIENT_EPOLL_EVENTLOOP = new LazyInitVar<>() {
        protected EpollEventLoopGroup init() {
            return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build());
        }
    };
    private final EnumProtocolDirection direction;
    private final Queue<NetworkManager.QueuedPacket> packetsQueue = Queues.newConcurrentLinkedQueue();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    public Channel channel;
    public SocketAddress socketAddress;
    public boolean preparing = true;
    private PacketListener packetListener;
    private String termationReason;
    private boolean isEncrypted;
    private boolean disconnected;

    public NetworkManager(EnumProtocolDirection enumprotocoldirection) {
        this.direction = enumprotocoldirection;
    }

    public void channelActive(ChannelHandlerContext channelhandlercontext) throws Exception {
        super.channelActive(channelhandlercontext);
        this.channel = channelhandlercontext.channel();
        this.socketAddress = this.channel.remoteAddress();
        this.preparing = false;

        try {
            this.setProtocol(EnumProtocol.HANDSHAKING);
        } catch (Throwable throwable) {
            NetworkManager.LOGGER.fatal(throwable);
        }

    }

    public void setProtocol(EnumProtocol enumprotocol) {
        this.channel.attr(NetworkManager.protocolAttributeKey).set(enumprotocol);
        this.channel.config().setAutoRead(true);
        NetworkManager.LOGGER.debug("Enabled auto read");
    }

    public void channelInactive(ChannelHandlerContext channelhandlercontext) throws Exception {
        this.close("End of stream");
    }

    public void exceptionCaught(ChannelHandlerContext channelhandlercontext, Throwable throwable) throws Exception {
        String msg;

        if (throwable instanceof TimeoutException) {
            msg = "Timed out";
        } else {
            msg = "Internal Exception: " + throwable;
        }

        this.close(msg);
        throwable.printStackTrace();
    }

    protected void processPacket(ChannelHandlerContext channelhandlercontext, Packet packet) throws Exception {
        if (this.channel.isOpen()) {
            try {
                packet.handle(this.packetListener);
            } catch (CancelledPacketHandleException cancelledpackethandleexception) {
                ;
            }
        }
    }

    public void setListener(PacketListener packetlistener) {
        Validate.notNull(packetlistener, "packetListener", new Object[0]);
        NetworkManager.LOGGER.debug("Set listener of {} to {}", new Object[] { this, packetlistener});
        this.packetListener = packetlistener;
    }

    public void handle(Packet packet) {
        if (this.isChannelOpen()) {
            this.flushQueue();
            this.dispatchPacket(packet, null, Boolean.TRUE);
        } else {
            this.readWriteLock.writeLock().lock();

            try {
                this.packetsQueue.add(new NetworkManager.QueuedPacket(packet, (GenericFutureListener[]) null));
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }

    @SafeVarargs
    public final void handle(Packet packet, GenericFutureListener<? extends Future<? super Void>> listener, GenericFutureListener<? extends Future<? super Void>>... listeners) {
        if (this.isChannelOpen()) {
            this.flushQueue();
            this.dispatchPacket(packet, ArrayUtils.insert(0, listeners, listener), Boolean.TRUE);
        } else {
            this.readWriteLock.writeLock().lock();

            try {
                this.packetsQueue.add(new NetworkManager.QueuedPacket(packet, ArrayUtils.insert(0, listeners, listener)));
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }

    }

    private void dispatchPacket(final Packet packet, final GenericFutureListener<? extends Future<? super Void>>[] listeners, Boolean flushConditional) {
        this.packetWrites.getAndIncrement();
        boolean effectiveFlush = flushConditional == null ? this.canFlush : flushConditional;
        final boolean flush = effectiveFlush || packet instanceof PacketPlayOutKeepAlive || packet instanceof PacketPlayOutKickDisconnect;

        final EnumProtocol packetProtocol = EnumProtocol.getProtocolForPacket(packet);
        final EnumProtocol currentProtocol = this.channel.attr(NetworkManager.protocolAttributeKey).get();

        if (currentProtocol != packetProtocol) {
            NetworkManager.LOGGER.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop()) {
            if (packetProtocol != currentProtocol) {
                this.setProtocol(packetProtocol);
            }

            ChannelFuture channelfuture = flush ? this.channel.writeAndFlush(packet) : this.channel.write(packet);

            if (listeners != null) {
                channelfuture.addListeners(listeners);
            }

            channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            Runnable choice1 = null;
            AbstractEventExecutor.LazyRunnable choice2 = null;
            if (flush) {
                choice1 = () -> {
                    if (packetProtocol != currentProtocol) {
                        this.setProtocol(packetProtocol);
                    }

                    try {
                        ChannelFuture channelFuture = this.channel.writeAndFlush(packet);
                        if (listeners != null) {
                            channelFuture.addListeners(listeners);
                        }
                        channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    } catch (Exception e) {
                        LOGGER.error("NetworkException: " + getUser(), e);
                        close("Internal Exception: " + e.getMessage());
                    }
                };
            } else {
                choice2 = () -> {
                    if (packetProtocol != currentProtocol) {
                        this.setProtocol(packetProtocol);
                    }
                    try {
                        ChannelFuture channelFuture = this.channel.write(packet);
                        if (listeners != null) {
                            channelFuture.addListeners(listeners);
                        }
                        channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    } catch (Exception e) {
                        LOGGER.error("NetworkException: " + getUser(), e);
                        close("Internal Exception: " + e.getMessage());
                    }
                };
            }
            this.channel.eventLoop().execute(choice1 != null ? choice1 : choice2);
        }

    }

    private void flushQueue() {
        if (this.packetsQueue.isEmpty()) {
            return;
        }
        if (this.channel != null && this.channel.isOpen()) {
            this.readWriteLock.readLock().lock();
            boolean needsFlush = this.canFlush;
            boolean hasWrotePacket = false;
            try {
                while (!this.packetsQueue.isEmpty()) {
                    NetworkManager.QueuedPacket queuedPacket = this.packetsQueue.poll();
                    Packet packet = queuedPacket.packet;
                    if (hasWrotePacket && (needsFlush || this.canFlush)) {
                        flush();
                    }

                    this.dispatchPacket(packet, queuedPacket.listeners, (this.packetsQueue.size() < 1 && (needsFlush || this.canFlush)) ? Boolean.TRUE : Boolean.FALSE);
                    hasWrotePacket = true;
                }
            } finally {
                this.readWriteLock.readLock().unlock();
            }

        }
    }

    public void tick() {
        this.flushQueue();
        if (this.packetListener instanceof ITickable) {
            ((ITickable) this.packetListener).update();
        }

        this.channel.flush();
    }

    public SocketAddress getSocketAddress() {
        return this.socketAddress;
    }

    public void close(String message) {
        this.preparing = false;
        if (this.channel.isOpen()) {
            this.channel.close();
            this.termationReason = message;
        }

    }

    public boolean isLocalChannel() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public void enableEncryption(SecretKey secretkey) throws CryptException {
        if (!this.isEncrypted) {
            try {
                com.velocitypowered.natives.encryption.VelocityCipher decryption = com.velocitypowered.natives.util.Natives.cipher
                        .get().forDecryption(secretkey);
                com.velocitypowered.natives.encryption.VelocityCipher encryption = com.velocitypowered.natives.util.Natives.cipher
                        .get().forEncryption(secretkey);

                this.isEncrypted = true;
                this.channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(decryption));
                this.channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(encryption));
            } catch (java.security.GeneralSecurityException e) {
                throw new CryptException(e);
            }
        }
    }

    public ChatUser getUser() {
        if (getPacketListener() instanceof UserConnection) {
            return ((UserConnection) getPacketListener()).user;
        } else {
            return null;
        }
    }

    public boolean isChannelOpen() {
        return this.channel != null && this.channel.isOpen();
    }

    public boolean hasNoChannel() {
        return this.channel == null;
    }

    public PacketListener getPacketListener() {
        return this.packetListener;
    }

    public String getTermationReason() {
        return this.termationReason;
    }

    public void disableAutoRead() {
        this.channel.config().setAutoRead(false);
    }

    public void setCompressionTreshold(int compressionTreshold) {
        if (compressionTreshold >= 0) {
            VelocityCompressor compressor = Natives.compress.get().create(-1);
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                ((PacketDecompressor) this.channel.pipeline().get("decompress")).setThreshold(compressionTreshold);
            } else {
                this.channel.pipeline().addBefore("decoder", "decompress", new PacketDecompressor(compressor, compressionTreshold));
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                ((PacketCompressor) this.channel.pipeline().get("decompress")).setThreshold(compressionTreshold);
            } else {
                this.channel.pipeline().addBefore("encoder", "compress", new PacketCompressor(compressor, compressionTreshold));
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                this.channel.pipeline().remove("compress");
            }
        }

    }

    public void checkDisconnected() {
        if (this.channel != null && !this.channel.isOpen()) {
            if (!this.disconnected) {
                this.disconnected = true;
                if (this.getTermationReason() != null) {
                    this.getPacketListener().onDisconnect(this.getTermationReason());
                } else if (this.getPacketListener() != null) {
                    this.getPacketListener().onDisconnect("Disconnected");
                }
                this.packetsQueue.clear();
            } else {
                NetworkManager.LOGGER.warn("handleDisconnection() called twice");
            }

        }
    }

    protected void channelRead0(ChannelHandlerContext channelhandlercontext, Packet object) throws Exception {
        if (this.isChannelOpen()) {
            this.processPacket(channelhandlercontext, object);
        }
    }

    volatile boolean canFlush = true;
    private final java.util.concurrent.atomic.AtomicInteger packetWrites = new java.util.concurrent.atomic.AtomicInteger();
    private int flushPacketsStart;
    private final Object flushLock = new Object();

    public void disableAutomaticFlush() {
        synchronized (this.flushLock) {
            this.flushPacketsStart = this.packetWrites.get();
            this.canFlush = false;
        }
    }

    public void enableAutomaticFlush() {
        synchronized (this.flushLock) {
            this.canFlush = true;
            if (this.packetWrites.get() != this.flushPacketsStart) {
                this.flush();
            }
        }
    }

    private void flush() {
        if (this.channel.eventLoop().inEventLoop()) {
            this.channel.flush();
        }
    }

    static class QueuedPacket {

        private final Packet packet;
        private final GenericFutureListener<? extends Future<? super Void>>[] listeners;

        @SafeVarargs
        public QueuedPacket(Packet packet, GenericFutureListener<? extends Future<? super Void>>... agenericfuturelistener) {
            this.packet = packet;
            this.listeners = agenericfuturelistener;
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
