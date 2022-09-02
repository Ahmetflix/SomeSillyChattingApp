package xyz.ahmetflix.chattingserver.connection.packet.impl.login;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.login.PacketLoginOutListener;

import java.io.IOException;

public class PacketLoginOutDisconnect implements Packet<PacketLoginOutListener> {
    private String reason;

    public PacketLoginOutDisconnect() {
    }

    public PacketLoginOutDisconnect(String reason) {
        this.reason = reason;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.reason = serializer.readString(Short.MAX_VALUE);
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeString(this.reason);
    }

    public void handle(PacketLoginOutListener handler) {
        handler.handleDisconnect(this);
    }

    public String getReason() {
        return reason;
    }
}