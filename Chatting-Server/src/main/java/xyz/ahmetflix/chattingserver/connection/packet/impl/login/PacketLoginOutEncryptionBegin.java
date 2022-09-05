package xyz.ahmetflix.chattingserver.connection.packet.impl.login;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.login.PacketLoginOutListener;
import xyz.ahmetflix.chattingserver.crpyt.CryptManager;

import java.io.IOException;
import java.security.PublicKey;

public class PacketLoginOutEncryptionBegin implements Packet<PacketLoginOutListener> {
    private String hashedServerId;
    private PublicKey publicKey;
    private byte[] verifyToken;

    public PacketLoginOutEncryptionBegin() {
    }

    public PacketLoginOutEncryptionBegin(String serverId, PublicKey key, byte[] verifyToken) {
        this.hashedServerId = serverId;
        this.publicKey = key;
        this.verifyToken = verifyToken;
    }

    public void read(PacketDataSerializer serializer) throws IOException {
        this.hashedServerId = serializer.readString(20);
        this.publicKey = CryptManager.decodePublicKey(serializer.readByteArray(Short.MAX_VALUE));
        this.verifyToken = serializer.readByteArray(Short.MAX_VALUE);
    }

    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeString(this.hashedServerId);
        serializer.writeByteArray(this.publicKey.getEncoded());
        serializer.writeByteArray(this.verifyToken);
    }

    public void handle(PacketLoginOutListener handler) {
        handler.handleEncryptionBegin(this);
    }

    public String getHashedServerId() {
        return hashedServerId;
    }

    public byte[] getVerifyToken() {
        return verifyToken;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
