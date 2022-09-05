package xyz.ahmetflix.chattingserver.util;

import xyz.ahmetflix.chattingserver.IAsyncTaskHandler;
import xyz.ahmetflix.chattingserver.connection.packet.CancelledPacketHandleException;
import xyz.ahmetflix.chattingserver.connection.packet.Packet;
import xyz.ahmetflix.chattingserver.connection.packet.PacketListener;

public class UserConnectionUtils {
    public static <T extends PacketListener> void ensureMainThread(final Packet<T> packet, final T listener, IAsyncTaskHandler handler) throws CancelledPacketHandleException {
        if (!handler.isMainThread()) {
            handler.postToMainThread(() -> packet.handle(listener));
            throw CancelledPacketHandleException.INSTANCE;
        }
    }
}
