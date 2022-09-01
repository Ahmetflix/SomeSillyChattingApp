package xyz.ahmetflix.chattingserver.connection.packet;

import java.io.IOException;

public interface Packet<T extends PacketListener> {
    void read(PacketDataSerializer buf) throws IOException;

    void write(PacketDataSerializer buf) throws IOException;

    void handle(T handler);
}