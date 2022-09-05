package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayOut;

import java.io.IOException;

public class PacketPlayOutChat implements Packet<PacketListenerPlayOut> {

    private String message;

    public PacketPlayOutChat() {
    }

    public PacketPlayOutChat(String msg) {
        this.message = msg;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void read(PacketDataSerializer serializer) throws IOException {
        this.message = serializer.readString(Short.MAX_VALUE);
    }

    @Override
    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeString(this.message);
    }

    @Override
    public void handle(PacketListenerPlayOut handler) {
        handler.handleChat(this);
    }
}
