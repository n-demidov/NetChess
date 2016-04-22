package edu.demidov.netchess.server.model.network;

import edu.demidov.netchess.common.model.network.NetworkMessage;
import io.netty.channel.Channel;
import java.util.Calendar;
import java.util.Date;

/**
 * Класс содержит NetworkMessage, прсиланное с клиента.
 * А также дополнительные свойства: io.netty.channel.Channel клиента и время получения сообщения.
 */
public class ServerNetworkMessage
{
    
    private final NetworkMessage netMsg;
    private final Channel channel;
    private final Date timeReceived;

    public ServerNetworkMessage(final NetworkMessage netMsg, final Channel channel)
    {
        assert netMsg != null;
        assert channel != null;
        
        this.netMsg = netMsg;
        this.channel = channel;
        this.timeReceived = Calendar.getInstance().getTime();
    }

    public NetworkMessage getNetMsg()
    {
        return netMsg;
    }

    public Channel getChannel()
    {
        return channel;
    }

    public Date getTimeReceived()
    {
        return timeReceived;
    }

    @Override
    public String toString()
    {
        return "ServerNetworkMessage{" +
                "netMsg=" + netMsg +
                ", channel=" + channel +
                ", timeReceived=" + timeReceived +
                '}';
    }
    
}
