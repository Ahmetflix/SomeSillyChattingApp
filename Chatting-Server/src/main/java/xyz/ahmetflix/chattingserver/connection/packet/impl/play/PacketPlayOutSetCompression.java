package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayOut;

import java.io.IOException;

public class PacketPlayOutSetCompression implements Packet<PacketListenerPlayOut> {
    private int threshold;

    public PacketPlayOutSetCompression() {
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.threshold = serializer.readVarInt();
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeVarInt(this.threshold);
    }

    public void handle(PacketListenerPlayOut handler) {
        handler.handleSetCompression(this);
    }

    public int getThreshold() {
        return threshold;
    }
}
