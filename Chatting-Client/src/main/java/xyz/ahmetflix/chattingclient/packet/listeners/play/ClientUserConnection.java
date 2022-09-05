package xyz.ahmetflix.chattingclient.packet.listeners.play;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingclient.Client;
import xyz.ahmetflix.chattingclient.NetworkUserInfo;
import xyz.ahmetflix.chattingclient.frame.ClientFrame;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.impl.play.*;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.play.PacketListenerPlayOut;
import xyz.ahmetflix.chattingserver.user.UserProfile;
import xyz.ahmetflix.chattingserver.util.UserConnectionUtils;

import java.util.Map;
import java.util.UUID;

public class ClientUserConnection implements PacketListenerPlayOut {

    private static final Logger LOGGER = LogManager.getLogger();

    private final NetworkManager networkManager;
    private final UserProfile profile;

    private Client client;

    private final Map<UUID, NetworkUserInfo> userInfoMap = Maps.<UUID, NetworkUserInfo>newHashMap();
    public int currentServerMaxUsers = 20;

    public ClientUserConnection(Client client, NetworkManager networkManager, UserProfile profile)
    {
        this.client = client;
        this.networkManager = networkManager;
        this.profile = profile;
    }

    @Override
    public void onDisconnect(String reason) {
        // TODO: ui
        LOGGER.info("Disconnected from server: " + reason);
    }

    @Override
    public void handleChat(PacketPlayOutChat packet) {
        // TODO: ui
        UserConnectionUtils.ensureMainThread(packet, this, this.client);

        LOGGER.info(packet.getMessage());
        ClientFrame.chatMessages.add(packet.getMessage());
    }

    @Override
    public void handleTabComplete(PacketPlayOutTabComplete packet) {
        // TODO: ui
        UserConnectionUtils.ensureMainThread(packet, this, this.client);
        String[] astring = packet.getMatches();

        LOGGER.info("Tab Complete Matches: " + StringUtils.join(astring, ", "));
    }

    @Override
    public void handleKickDisconnect(PacketPlayOutKickDisconnect packet) {
        this.networkManager.close(packet.getReason());
    }

    @Override
    public void handleKeepAlive(PacketPlayOutKeepAlive packet) {
        this.networkManager.handle(new PacketPlayInKeepAlive(packet.getKeepAlive()));
    }

    @Override
    public void handleLogin(PacketPlayOutLogin packet) {
        UserConnectionUtils.ensureMainThread(packet, this, this.client);
        this.currentServerMaxUsers = packet.getMaxUsers();
        this.client.user.setId(packet.getId());
    }

    @Override
    public void handleUserInfo(PacketPlayOutUserInfo packet) {
        UserConnectionUtils.ensureMainThread(packet, this, this.client);

        for (PacketPlayOutUserInfo.UserInfoData userInfoData : packet.getUsers()) {
            if (packet.getAction() == PacketPlayOutUserInfo.EnumUserInfoAction.REMOVE_USER) {
                this.userInfoMap.remove(userInfoData.getProfile().getId());
            } else {
                NetworkUserInfo networkUserInfo = this.userInfoMap.get(userInfoData.getProfile().getId());

                if (packet.getAction() == PacketPlayOutUserInfo.EnumUserInfoAction.ADD_USER) {
                    networkUserInfo = new NetworkUserInfo(userInfoData);
                    this.userInfoMap.put(networkUserInfo.getUserProfile().getId(), networkUserInfo);
                }

                if (networkUserInfo != null) {
                    switch (packet.getAction()) {
                        case ADD_USER, UPDATE_LATENCY -> networkUserInfo.setResponseTime(userInfoData.getPing());
                    }
                }
            }
        }
    }

    @Override
    public void handleSetCompression(PacketPlayOutSetCompression packet) {
        if (!this.networkManager.isLocalChannel()) {
            this.networkManager.setCompressionTreshold(packet.getThreshold());
        }
    }

    public void sendPacket(Packet<?> packet) {
        this.networkManager.handle(packet);
    }
}
