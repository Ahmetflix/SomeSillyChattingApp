package xyz.ahmetflix.chattingserver.connection.packet.impl.status;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.status.PacketStatusOutListener;
import xyz.ahmetflix.chattingserver.status.ServerStatusResponse;

import java.io.IOException;

public class PacketStatusOutServerInfo implements Packet<PacketStatusOutListener> {
    private static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(ServerStatusResponse.UserCountData.class, new ServerStatusResponse.UserCountData.Serializer()).registerTypeAdapter(ServerStatusResponse.class, new ServerStatusResponse.Serializer()).create();
    private ServerStatusResponse serverStatusResponse;

    public PacketStatusOutServerInfo() {
    }

    public PacketStatusOutServerInfo(ServerStatusResponse serverStatusResponse) {
        this.serverStatusResponse = serverStatusResponse;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.serverStatusResponse = GSON.fromJson(serializer.readString(32767), ServerStatusResponse.class);
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeString(GSON.toJson(this.serverStatusResponse));
    }

    public void handle(PacketStatusOutListener var1) {
        var1.handleServerInfo(this);
    }

    public ServerStatusResponse getServerStatusResponse() {
        return serverStatusResponse;
    }
}
