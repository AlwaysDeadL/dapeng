package com.github.dapeng.impl.plugins.netty;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.util.SoaSystemEnvProperties;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Soa Decoder
 *
 * @author craneding
 * @date 16/1/12
 */
public class SoaDecoder extends ByteToMessageDecoder {

    public SoaDecoder() {
        setSingleDecode(false);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // waiting for four bytes
        if (in.readableBytes() < Integer.BYTES) return;

        int readerIndex = in.readerIndex();

        int length = in.readInt();

        if (length == 0) {// 心跳
            ctx.writeAndFlush(ctx.alloc().buffer(1).writeInt(0));

            return;
        }

        if (length > SoaSystemEnvProperties.SOA_MAX_READ_BUFFER_SIZE)
            throw new SoaException("error", "Exceeds the maximum length:(" + length + " > " + SoaSystemEnvProperties.SOA_MAX_READ_BUFFER_SIZE + ")");

        // waiting for complete
        if (in.readableBytes() < length) {
            in.readerIndex(readerIndex);

            return;
        }

        // must be to release one times
        ByteBuf msg = in.slice(readerIndex, length + Integer.BYTES).retain();

        /**
         * 将readerIndex放到报文尾，否则长连接收到第二次报文时会出现不可预料的错误
         */
        in.readerIndex(readerIndex + length + Integer.BYTES);

        out.add(msg);
    }

}
