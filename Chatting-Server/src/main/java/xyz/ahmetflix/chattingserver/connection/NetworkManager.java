package xyz.ahmetflix.chattingserver.connection;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
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
import xyz.ahmetflix.chattingserver.LazyInitVar;
import xyz.ahmetflix.chattingserver.connection.packet.CancelledPacketHandleException;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;

import javax.crypto.SecretKey;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NetworkManager extends SimpleChannelInboundHandler<Packet> {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final Marker networkMarker = MarkerManager.getMarker("NETWORK");
    public static final Marker networkPacketsMarker = MarkerManager.getMarker("NETWORK_PACKETS", NetworkManager.networkMarker);
    public static final AttributeKey<EnumProtocol> protocolAttributeKey = AttributeKey.valueOf("protocol");
    public static final LazyInitVar<NioEventLoopGroup> CLIENT_NIO_EVENTLOOP = new LazyInitVar<NioEventLoopGroup>() {
        protected NioEventLoopGroup init() {
            return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyInitVar<EpollEventLoopGroup> CLIENT_EPOLL_EVENTLOOP = new LazyInitVar<EpollEventLoopGroup>() {
        protected EpollEventLoopGroup init() {
            return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build());
        }
    };
    public static final LazyInitVar<LocalEventLoopGroup> CLIENT_LOCAL_EVENTLOOP = new LazyInitVar<LocalEventLoopGroup>() {
        protected LocalEventLoopGroup init() {
            return new LocalEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Client IO #%d").setDaemon(true).build());
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
        NetworkManager.LOGGER.info("Set listener of {} to {}", new Object[] { this, packetlistener});
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

    public void handle(Packet packet, GenericFutureListener<? extends Future<? super Void>> listener, GenericFutureListener<? extends Future<? super Void>>... listeners) {
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
        final boolean flush = effectiveFlush /*|| packet instanceof PacketPlayOutKeepAlive || packet instanceof PacketPlayOutKickDisconnect*/;

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
                        LOGGER.error("NetworkException: " /* + getUser() */, e);
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
                        LOGGER.error("NetworkException: " /* + getUser() */, e);
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

    public void enableEncryption(SecretKey secretkey) {
        this.isEncrypted = true;
        // dont encrypting because of dont authorizing for now
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

    public void setCompressionTreshold(int i) {
        /*if (i >= 0) {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                ((PacketDecompressor) this.channel.pipeline().get("decompress")).a(i);
            } else {
                this.channel.pipeline().addBefore("decoder", "decompress", new PacketDecompressor(i));
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                ((PacketCompressor) this.channel.pipeline().get("decompress")).a(i);
            } else {
                this.channel.pipeline().addBefore("encoder", "compress", new PacketCompressor(i));
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                this.channel.pipeline().remove("compress");
            }
        }*/

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
        this.processPacket(channelhandlercontext, object);
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

        public QueuedPacket(Packet packet, GenericFutureListener<? extends Future<? super Void>>... agenericfuturelistener) {
            this.packet = packet;
            this.listeners = agenericfuturelistener;
        }
    }

}
