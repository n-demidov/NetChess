package edu.demidov.netchess.server.model.network.netty;

import edu.demidov.netchess.common.model.network.MessageQueue;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetChessServerHandler extends ChannelInboundHandlerAdapter
{
    
    private final MessageQueue messageQueue = MessageQueue.getInstance();
    private final static Logger log = LoggerFactory.getLogger(NetChessServerHandler.class.getName());
    
    @Override
    public void channelActive(final ChannelHandlerContext ctx)
    {
        log.debug("channelActive channel={}", ctx.channel());
        try
        {
            // Помещаем сообщение в очередь обработки
            final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.ConnectionOpened);
            final ServerNetworkMessage snm
                    = new ServerNetworkMessage(netMsg, ctx.channel());
            messageQueue.putMessage(snm);
        } catch (final InterruptedException ex)
        {
            log.error("channelActive channel={}", ctx.channel(), ex);
        }
    }
    
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg)
    {
        log.trace("channelRead channel={}, msg={}", ctx.channel(), msg);
        try
        {
            // Помещаем сообщение в очередь сообщений
            final ServerNetworkMessage snm
                    = new ServerNetworkMessage((NetworkMessage) msg, ctx.channel());
            messageQueue.putMessage(snm);
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
        log.trace("exceptionCaught channel={}, cause={}", ctx.channel(), cause);
        try
        {
            ctx.close();    // Close the connection when an exception is raised.

            // Помещаем сообщение в очередь обработки
            final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.ConnectionClosed);
            final ServerNetworkMessage snm
                    = new ServerNetworkMessage(netMsg, ctx.channel());
            messageQueue.putMessage(snm);
        } catch (final InterruptedException ex)
        {
            log.error("exceptionCaught channel={}, cause={}", ctx.channel(), cause, ex);
        }
    }
    
}
