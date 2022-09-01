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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
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
    public static final Marker networkPacketsMarket = MarkerManager.getMarker("NETWORK_PACKETS", NetworkManager.networkMarker);
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
        throwable.printStackTrace(); // Spigot
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
            this.dispatchPacket(packet, null);
        } else {
            this.readWriteLock.writeLock().lock();

            try {
                this.packetsQueue.add(new NetworkManager.QueuedPacket(packet, (GenericFutureListener[]) null));
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }

    private void dispatchPacket(final Packet packet, final GenericFutureListener<? extends Future<? super Void>>[] agenericfuturelistener) {
        final EnumProtocol enumprotocol = EnumProtocol.getFromPacket(packet);
        final EnumProtocol enumprotocol1 = this.channel.attr(NetworkManager.protocolAttributeKey).get();

        if (enumprotocol1 != enumprotocol) {
            NetworkManager.LOGGER.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop()) {
            if (enumprotocol != enumprotocol1) {
                this.setProtocol(enumprotocol);
            }

            ChannelFuture channelfuture = this.channel.writeAndFlush(packet);

            if (agenericfuturelistener != null) {
                channelfuture.addListeners(agenericfuturelistener);
            }

            channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            this.channel.eventLoop().execute(new Runnable() {
                public void run() {
                    if (enumprotocol != enumprotocol1) {
                        NetworkManager.this.setProtocol(enumprotocol);
                    }

                    ChannelFuture channelfuture = NetworkManager.this.channel.writeAndFlush(packet);

                    if (agenericfuturelistener != null) {
                        channelfuture.addListeners(agenericfuturelistener);
                    }

                    channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }
            });
        }

    }

    private void flushQueue() {
        if (this.channel != null && this.channel.isOpen()) {
            this.readWriteLock.readLock().lock();

            try {
                while (!this.packetsQueue.isEmpty()) {
                    NetworkManager.QueuedPacket queuedPacket = this.packetsQueue.poll();

                    this.dispatchPacket(queuedPacket.packet, queuedPacket.listeners);
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
        //this.channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(MinecraftEncryption.a(2, secretkey)));
        //this.channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(MinecraftEncryption.a(1, secretkey)));
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

    protected void channelRead0(ChannelHandlerContext channelhandlercontext, Packet object) throws Exception { // CraftBukkit - fix decompile error
        this.processPacket(channelhandlercontext, (Packet) object);
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
