package xyz.ahmetflix.chattingserver.connection.packet.listeners.handshaking;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.connection.EnumProtocol;
import xyz.ahmetflix.chattingserver.connection.NetworkManager;
import xyz.ahmetflix.chattingserver.connection.packet.impl.handshaking.PacketHandshakingInSetProtocol;
import xyz.ahmetflix.chattingserver.connection.packet.listeners.status.PacketStatusListener;

import java.net.InetAddress;
import java.util.Map;

public class HandshakeListener implements PacketHandshakingInListener {

    private static final com.google.gson.Gson gson = new com.google.gson.Gson();
    private static final Map<InetAddress, Long> throttleTracker = new Object2LongOpenHashMap<>();
    private static int throttleCounter = 0;

    private final Server server;
    private final NetworkManager networkManager;

    public HandshakeListener(Server server, NetworkManager networkmanager) {
        this.server = server;
        this.networkManager = networkmanager;
    }

    @Override
    public void handleSetProtocol(PacketHandshakingInSetProtocol packethandshakinginsetprotocol) {
        switch (packethandshakinginsetprotocol.getRequestedState()) {
            case LOGIN: {
                this.networkManager.setProtocol(EnumProtocol.LOGIN);
                String text;

                try {
                    long currentTime = System.currentTimeMillis();
                    long connectionThrottle = 4000L;
                    InetAddress address = ((java.net.InetSocketAddress) this.networkManager.getSocketAddress()).getAddress();

                    synchronized (throttleTracker) {
                        if (throttleTracker.containsKey(address) && !"127.0.0.1".equals(address.getHostAddress())
                                && currentTime - throttleTracker.get(address) < connectionThrottle) {
                            throttleTracker.put(address, currentTime);
                            text = "Connection throttled! Please wait before reconnecting.";
                            //this.networkManager.handle(new PacketLoginOutDisconnect(text));
                            this.networkManager.close(text);
                            return;
                        }

                        throttleTracker.put(address, currentTime);
                        throttleCounter++;
                        if (throttleCounter > 200) {
                            throttleCounter = 0;

                            throttleTracker.entrySet().removeIf(entry -> entry.getValue() > connectionThrottle);
                        }
                    }
                } catch (Throwable t) {
                    org.apache.logging.log4j.LogManager.getLogger().debug("Failed to check connection throttle", t);
                }

                // TODO: add login listener
                //this.networkManager.setListener(new LoginListener(this.server, this.networkManager));

                //((LoginListener) this.networkManager.getPacketListener()).hostname = packethandshakinginsetprotocol.hostname + ":" + packethandshakinginsetprotocol.port;
                break;
            }
            case STATUS: {

                this.networkManager.setProtocol(EnumProtocol.STATUS);
                this.networkManager.setListener(new PacketStatusListener(this.server, this.networkManager));
                break;
            }
            default:
                throw new UnsupportedOperationException("Invalid intention " + packethandshakinginsetprotocol.getRequestedState());
        }
    }

    @Override
    public void onDisconnect(String reason) {

    }
}
