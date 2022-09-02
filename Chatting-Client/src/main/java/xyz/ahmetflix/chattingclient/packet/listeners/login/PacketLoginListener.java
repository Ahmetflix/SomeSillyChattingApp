package xyz.ahmetflix.chattingclient.packet.listeners.login;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingclient.Client;
import xyz.ahmetflix.chattingserver.UserProfile;
import xyz.ahmetflix.chattingserver.connection.EnumProtocol;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutDisconnect;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutSetCompression;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.PacketLoginOutSuccess;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.login.PacketLoginOutListener;

public class PacketLoginListener implements PacketLoginOutListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Client client;
    private final NetworkManager networkManager;
    private UserProfile profile;

    public PacketLoginListener(NetworkManager networkManagerIn, Client client)
    {
        this.networkManager = networkManagerIn;
        this.client = client;
    }

    @Override
    public void onDisconnect(String reason) {
        // TODO: show in ui
        LOGGER.error("Connection failed: " + reason);
    }

    @Override
    public void handleSuccess(PacketLoginOutSuccess packetIn) {
        this.profile = packetIn.getProfile();
        LOGGER.info("Login success");
        this.networkManager.setProtocol(EnumProtocol.PLAY);
        // TODO: set play listener
    }

    @Override
    public void handleDisconnect(PacketLoginOutDisconnect packetIn) {
        this.networkManager.close(packetIn.getReason());
    }

    @Override
    public void handleSetCompression(PacketLoginOutSetCompression packetIn) {
        if (!this.networkManager.isLocalChannel()) {
            this.networkManager.setCompressionTreshold(packetIn.getThreshold());
        }
    }
}
