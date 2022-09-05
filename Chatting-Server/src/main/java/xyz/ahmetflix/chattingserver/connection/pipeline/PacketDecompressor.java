package xyz.ahmetflix.chattingserver.connection.pipeline;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import xyz.ahmetflix.chattingserver.connection.packet.PacketDataSerializer;

import java.util.List;
import java.util.zip.Inflater;

public class PacketDecompressor extends ByteToMessageDecoder {
    private final Inflater inflater;
    private final com.velocitypowered.natives.compression.VelocityCompressor compressor;
    private int threshold;

    public PacketDecompressor(int compressionThreshold) {
        this(null, compressionThreshold);
    }

    public PacketDecompressor(VelocityCompressor compressor, int compressionThreshold) {
        this.threshold = compressionThreshold;
        this.inflater = compressor == null ? new Inflater() : null;
        this.compressor = compressor;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() != 0) {
            PacketDataSerializer serializer = new PacketDataSerializer(in);
            int claimedUncompressedSize = serializer.readVarInt();
            if (claimedUncompressedSize == 0) {
                out.add(serializer.readBytes(serializer.readableBytes()));
            } else {
                if (claimedUncompressedSize < this.threshold) {
                    throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize
                            + " is below server threshold of " + this.threshold);
                }

                if (claimedUncompressedSize > 2097152) {
                    throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize
                            + " is larger than protocol maximum of " + 2097152);
                }
                if (this.inflater != null) {
                    byte[] var6 = new byte[serializer.readableBytes()];
                    serializer.readBytes(var6);
                    this.inflater.setInput(var6);
                    byte[] var7 = new byte[claimedUncompressedSize];
                    this.inflater.inflate(var7);
                    out.add(Unpooled.wrappedBuffer(var7));
                    this.inflater.reset();
                    return;
                }
                ByteBuf compatibleIn = MoreByteBufUtils.ensureCompatible(ctx.alloc(), this.compressor, in);
                ByteBuf uncompressed = MoreByteBufUtils.preferredBuffer(ctx.alloc(), this.compressor,
                        claimedUncompressedSize);
                try {
                    this.compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
                    out.add(uncompressed);
                    in.clear();
                } catch (Exception e) {
                    uncompressed.release();
                    throw e;
                } finally {
                    compatibleIn.release();
                }
            }
        }
    }

    @Override
    public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        if (this.compressor != null) {
            this.compressor.close();
        }
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}