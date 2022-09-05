package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayOut;

import java.io.IOException;

public class PacketPlayOutTabComplete implements Packet<PacketListenerPlayOut> {
    private String[] matches;

    public PacketPlayOutTabComplete() {
    }

    public PacketPlayOutTabComplete(String[] matches) {
        this.matches = matches;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.matches = new String[serializer.readVarInt()];

        for(int i = 0; i < this.matches.length; ++i) {
            this.matches[i] = serializer.readString(32767);
        }

    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeVarInt(this.matches.length);

        for (String match : this.matches) {
            serializer.writeString(match);
        }

    }

    public void handle(PacketListenerPlayOut handler) {
        handler.handleTabComplete(this);
    }

    public String[] getMatches() {
        return matches;
    }
}
