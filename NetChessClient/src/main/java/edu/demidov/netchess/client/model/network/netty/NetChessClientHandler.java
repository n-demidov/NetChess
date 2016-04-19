package edu.demidov.netchess.client.model.network.netty;

import edu.demidov.netchess.common.model.network.MessageQueue;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetChessClientHandler extends ChannelInboundHandlerAdapter
{
    
    private final MessageQueue<NetworkMessage> messageQueue = MessageQueue.getInstance();
    private final static Logger log = LoggerFactory.getLogger(NetChessClientHandler.class);

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg)
    {
        log.trace("channelRead channel={}, msg={}", ctx.channel(), msg);
        try
        {
            // Помещаем сообщение в очередь сообщений
            final NetworkMessage netMsg = (NetworkMessage) msg;
            messageQueue.putMessage(netMsg);
        } catch (final InterruptedException ex)
        {
            log.error("channelRead channel={}, msg={}", ctx.channel(), msg, ex);
        } finally
        {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
    {
        log.warn("exceptionCaught channel={}, cause={}", ctx.channel(), cause);
        try
        {
            ctx.close();        // Close the connection when an exception is raised.

            // Помещаем сообщение в очередь обработки
            final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.ConnectionClosed);
            netMsg.put(NetworkMessage.TEXT, cause.getLocalizedMessage());
            messageQueue.putMessage(netMsg);
        } catch (final InterruptedException ex)
        {
            log.error("exceptionCaught channel={}, cause={}", ctx.channel(), cause, ex);
        }
    }
    
}
