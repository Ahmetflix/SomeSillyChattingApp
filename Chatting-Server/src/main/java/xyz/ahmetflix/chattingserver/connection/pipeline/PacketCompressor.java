package xyz.ahmetflix.chattingserver.connection.pipeline;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;

import java.util.zip.Deflater;

public class PacketCompressor extends MessageToByteEncoder<ByteBuf> {
    private final byte[] encodeBuf;
    private final Deflater deflater;
    private final com.velocitypowered.natives.compression.VelocityCompressor compressor;
    private int threshold;

    public PacketCompressor(int compressionThreshold) {
        this(null, compressionThreshold);
    }

    public PacketCompressor(VelocityCompressor compressor, int compressionThreshold) {
        this.threshold = compressionThreshold;
        if (compressor == null) {
            this.encodeBuf = new byte[8192];
            this.deflater = new Deflater();
        } else {
            this.encodeBuf = null;
            this.deflater = null;
        }
        this.compressor = compressor;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        int var4 = in.readableBytes();
        PacketDataSerializer serializer = new PacketDataSerializer(out);
        if (var4 < this.threshold) {
            serializer.writeVarInt(0);
            serializer.writeBytes(in);
        } else {
            if (this.deflater != null) {
                byte[] var6 = new byte[var4];
                in.readBytes(var6);
                serializer.writeVarInt(var6.length);
                this.deflater.setInput(var6, 0, var4);
                this.deflater.finish();

                while (!this.deflater.finished()) {
                    int var7 = this.deflater.deflate(this.encodeBuf);
                    serializer.writeBytes(this.encodeBuf, 0, var7);
                }

                this.deflater.reset();
                return;
            }

            serializer.writeVarInt(var4);
            ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), this.compressor, in);
            try {
                this.compressor.deflate(compatibleIn, out);
            } finally {
                compatibleIn.release();
            }
        }
    }
    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect) throws Exception {
        if (this.compressor != null) {
            int initialBufferSize = msg.readableBytes() + 1;
            return com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer(ctx.alloc(), this.compressor,
                    initialBufferSize);
        }

        return super.allocateBuffer(ctx, msg, preferDirect);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (this.compressor != null) {
            this.compressor.close();
        }
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
