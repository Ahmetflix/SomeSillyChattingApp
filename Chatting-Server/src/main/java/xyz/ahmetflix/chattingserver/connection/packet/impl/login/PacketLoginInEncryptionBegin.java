package xyz.ahmetflix.chattingserver.connection.packet.impl.login;

import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.login.PacketLoginInListener;
import xyz.ahmetflix.chattingserver.crpyt.CryptManager;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class PacketLoginInEncryptionBegin implements Packet<PacketLoginInListener> {

    private byte[] secretKeyEncrypted = new byte[0];
    private byte[] verifyTokenEncrypted = new byte[0];

    public PacketLoginInEncryptionBegin() {
    }

    public PacketLoginInEncryptionBegin(SecretKey secretKey, PublicKey publicKey, byte[] verifyToken)
    {
        this.secretKeyEncrypted = CryptManager.encryptData(publicKey, secretKey.getEncoded());
        this.verifyTokenEncrypted = CryptManager.encryptData(publicKey, verifyToken);
    }


    @Override
    public void read(PacketDataSerializer serializer) throws IOException {
        this.secretKeyEncrypted = serializer.readByteArray(256);
        this.verifyTokenEncrypted = serializer.readByteArray(256);
    }

    @Override
    public void write(PacketDataSerializer serializer) throws IOException {
        serializer.writeByteArray(this.secretKeyEncrypted);
        serializer.writeByteArray(this.verifyTokenEncrypted);
    }

    @Override
    public void handle(PacketLoginInListener handler) {
        handler.handleEncryption(this);
    }

    public SecretKey getSecretKey(PrivateKey privatekey) {
        return CryptManager.decryptSharedKey(privatekey, this.secretKeyEncrypted);
    }

    public byte[] getVerifyToken(PrivateKey privatekey) {
        return privatekey == null ? this.verifyTokenEncrypted : CryptManager.decryptData(privatekey, this.verifyTokenEncrypted);
    }
}
