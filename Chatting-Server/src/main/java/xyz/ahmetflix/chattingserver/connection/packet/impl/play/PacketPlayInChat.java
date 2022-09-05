package xyz.ahmetflix.chattingserver.connection.packet.impl.play;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayIn;

import java.io.IOException;

public class PacketPlayInChat implements Packet<PacketListenerPlayIn> {

    private String message;

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PacketPlayInChat() {
    }

    public PacketPlayInChat(String message) {
        if (message.length() > 100) {
            message = message.substring(0, 100);
        }

        this.message = message;
    }

    @Override
    public void read(PacketDataSerializer serializer) throws IOException {
        this.message = serializer.readString(100);
    }

    @Override
    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeString(this.message);
    }

    private static final java.util.concurrent.ExecutorService executors = java.util.concurrent.Executors
            .newCachedThreadPool(new com.google.common.util.concurrent.ThreadFactoryBuilder().setDaemon(true)
                    .setNameFormat("Async Chat Thread - #%d").build());

    @Override
    public void handle(final PacketListenerPlayIn handler) {
        if (!message.startsWith("/")) {
            executors.submit(new Runnable() {

                @Override
                public void run() {
                    handler.handleChat(PacketPlayInChat.this);
                }
            });
            return;
        }
        handler.handleChat(this);
    }
}
