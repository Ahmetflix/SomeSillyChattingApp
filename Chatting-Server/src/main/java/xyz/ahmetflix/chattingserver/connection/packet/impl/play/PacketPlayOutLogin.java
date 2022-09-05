package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayOut;

import java.io.IOException;

public class PacketPlayOutLogin implements Packet<PacketListenerPlayOut> {
    private int id;
    private int maxUsers;

    public PacketPlayOutLogin() {
    }

    public PacketPlayOutLogin(int id, int maxUsers) {
        this.id = id;
        this.maxUsers = maxUsers;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.id = serializer.readInt();
        this.maxUsers = serializer.readUnsignedByte();
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeInt(this.id);
        serializer.writeByte(this.maxUsers);
    }

    public void handle(PacketListenerPlayOut handler) {
        handler.handleLogin(this);
    }

    public int getId() {
        return id;
    }

    public int getMaxUsers() {
        return maxUsers;
    }
}
