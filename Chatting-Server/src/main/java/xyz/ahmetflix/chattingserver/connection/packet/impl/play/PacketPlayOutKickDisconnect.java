package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayOut;

import java.io.IOException;

public class PacketPlayOutKickDisconnect implements Packet<PacketListenerPlayOut> {
    private String reason;

    public PacketPlayOutKickDisconnect() {
    }

    public PacketPlayOutKickDisconnect(String reason) {
        this.reason = reason;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.reason = serializer.readString(Short.MAX_VALUE);
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeString(this.reason);
    }

    public void handle(PacketListenerPlayOut handler) {
        handler.handleKickDisconnect(this);
    }

    public String getReason() {
        return reason;
    }
}
