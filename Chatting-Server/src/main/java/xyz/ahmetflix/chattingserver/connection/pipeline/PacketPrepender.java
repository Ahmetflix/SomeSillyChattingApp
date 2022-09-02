package xyz.ahmetflix.chattingserver.connection.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

@ChannelHandler.Sharable
public class PacketPrepender extends MessageToMessageEncoder<ByteBuf> {
    public static final PacketPrepender INSTANCE = new PacketPrepender();

    public PacketPrepender() {
    }

    public static void writeVarInt(ByteBuf buf, int i) {
        while ((i & -128) != 0) {
            buf.writeByte(i & 127 | 128);
            i >>>= 7;
        }

        buf.writeByte(i);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf lengthBuf = ctx.alloc().buffer(5);
        writeVarInt(lengthBuf, in.readableBytes());
        out.add(lengthBuf);
        out.add(in.retain());
    }
}