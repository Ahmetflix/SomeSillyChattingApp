package xyz.ahmetflix.chattingserver.connection.packet.listeners.status;

import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.UserProfile;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusInPing;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusInStart;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusOutPong;
import xyz.ahmetflix.chattingserver.connection.packet.impl.status.PacketStatusOutServerInfo;
import xyz.ahmetflix.chattingserver.status.ServerStatusResponse;
import xyz.ahmetflix.chattingserver.user.ChatUser;

public class PacketStatusListener implements PacketStatusInListener {

    private static final String closeStr = "Status request has been handled.";
    private final Server server;
    private final NetworkManager networkManager;
    private boolean isHandled;

    public PacketStatusListener(Server server, NetworkManager networkmanager) {
        this.server = server;
        this.networkManager = networkmanager;
    }

    @Override
    public void onDisconnect(String reason) {

    }

    @Override
    public void handleStatusStart(PacketStatusInStart packetstatusinstart) {
        if (this.isHandled) {
            this.networkManager.close(PacketStatusListener.closeStr);
            return;
        }
        this.isHandled = true;
        final Object[] users = server.getUsersList().users.toArray();

        java.util.List<UserProfile> profiles = new java.util.ArrayList<>(users.length);
        for (Object user : users) {
            if (user != null) {
                profiles.add(((ChatUser) user).getProfile());
            }
        }

        ServerStatusResponse.UserCountData userSample = new ServerStatusResponse.UserCountData(server.getUsersList().getMaxUsers(), profiles.size());

        if (!profiles.isEmpty()) {
            java.util.Collections.shuffle(profiles);
            profiles = profiles.subList(0, Math.min(profiles.size(), 12));
        }

        userSample.setUsers(profiles.toArray(new UserProfile[0]));

        ServerStatusResponse ping = server.getStatusResponse();
        ping.setPlayerCountData(userSample);

        this.networkManager.handle(new PacketStatusOutServerInfo(ping));
    }

    @Override
    public void handlePing(PacketStatusInPing packetstatusinping) {
        this.networkManager.handle(new PacketStatusOutPong(packetstatusinping.getPing()));
        this.networkManager.close(PacketStatusListener.closeStr);
    }
}
