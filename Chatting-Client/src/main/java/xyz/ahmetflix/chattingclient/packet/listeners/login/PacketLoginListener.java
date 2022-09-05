package xyz.ahmetflix.chattingclient.packet.listeners.login;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingclient.Client;
import xyz.ahmetflix.chattingclient.ClientChatUser;
import xyz.ahmetflix.chattingclient.packet.listeners.play.ClientUserConnection;
import xyz.ahmetflix.chattingserver.connection.EnumProtocol;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.impl.login.*;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.login.PacketLoginOutListener;
import xyz.ahmetflix.chattingserver.crpyt.CryptManager;
import xyz.ahmetflix.chattingserver.user.UserProfile;

import javax.crypto.SecretKey;
import java.security.PublicKey;

public class PacketLoginListener implements PacketLoginOutListener {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Client client;
    private final NetworkManager networkManager;

    public PacketLoginListener(NetworkManager networkManagerIn, Client client) {
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
        UserProfile profile = packetIn.getProfile();
        LOGGER.info("Login success");
        this.client.user = new ClientChatUser(profile);
        this.networkManager.setProtocol(EnumProtocol.PLAY);
        this.networkManager.setListener(this.client.user.connection = new ClientUserConnection(this.client, this.networkManager, profile));
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

    @Override
    public void handleEncryptionBegin(PacketLoginOutEncryptionBegin packet) {
        final SecretKey secretKey = CryptManager.createNewSharedKey();
        PublicKey publicKey = packet.getPublicKey();

        this.networkManager.handle(new PacketLoginInEncryptionBegin(secretKey, publicKey, packet.getVerifyToken()), f -> PacketLoginListener.this.networkManager.enableEncryption(secretKey));
    }
}
