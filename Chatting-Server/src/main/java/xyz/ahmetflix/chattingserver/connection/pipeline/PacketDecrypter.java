package xyz.ahmetflix.chattingserver.connection.pipeline;

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.awt.print.Paper;
import java.util.List;

public class PacketDecrypter extends MessageToMessageDecoder<ByteBuf> {
    private final VelocityCipher cipher;

    public PacketDecrypter(VelocityCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        ByteBuf compatible = MoreByteBufUtils.ensureCompatible(channelHandlerContext.alloc(), cipher, byteBuf);
        try {
            cipher.process(compatible);
            list.add(compatible);
        } catch (Exception e) {
            compatible.release();
            throw e;
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        cipher.close();
    }
}
