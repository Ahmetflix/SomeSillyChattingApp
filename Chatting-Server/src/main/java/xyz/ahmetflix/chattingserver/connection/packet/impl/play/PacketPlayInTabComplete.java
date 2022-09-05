package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import org.apache.commons.lang3.StringUtils;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayIn;

import java.io.IOException;

public class PacketPlayInTabComplete implements Packet<PacketListenerPlayIn> {
    private String query;

    public PacketPlayInTabComplete() {
    }

    public PacketPlayInTabComplete(String query) {
        this.query = query;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.query = serializer.readString(32767);
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeString(StringUtils.substring(this.query, 0, 32767));
    }

    public void handle(PacketListenerPlayIn handler) {
        handler.handleTabComplete(this);
    }

    public String getQuery() {
        return query;
    }
}
