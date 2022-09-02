package xyz.ahmetflix.chattingserver.connection;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.handshaking.HandshakeListener;
import xyz.ahmetflix.chattingserver.connection.pipeline.*;

public class ServerPipeline extends ChannelInitializer<SocketChannel> {

    private final ServerConnection serverConnection;

    public ServerPipeline(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    @Override
    protected void initChannel(SocketChannel channel) {
        try {
            ChannelConfig config = channel.config();
            config.setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
            config.setOption(ChannelOption.IP_TOS, 0x18);
            config.setAllocator(ByteBufAllocator.DEFAULT);
        } catch (Exception ignored) {
        }

        channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30))
                .addLast("splitter", new PacketSplitter())
                .addLast("decoder", new PacketDecoder(EnumProtocolDirection.SERVERBOUND))
                .addLast("prepender", PacketPrepender.INSTANCE)
                .addLast("encoder", new PacketEncoder(EnumProtocolDirection.CLIENTBOUND));
        NetworkManager networkmanager = new NetworkManager(EnumProtocolDirection.SERVERBOUND);
        this.serverConnection.pending.add(networkmanager);
        channel.pipeline().addLast("packet_handler", networkmanager);
        networkmanager.setListener(new HandshakeListener(this.serverConnection.getServer(), networkmanager));
    }

}
