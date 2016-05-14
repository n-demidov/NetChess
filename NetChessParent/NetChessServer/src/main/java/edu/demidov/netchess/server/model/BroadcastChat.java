package edu.demidov.netchess.server.model;

import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.users.User;
import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Рассылает сообщения пользователям
 */
public class BroadcastChat
{
    
    private static final String TIME_FORMAT = "HH:mm:ss";
    private static final String FORMAT_STRING = "[%s %s - %s]";
    private static final String USER_DISCONNECTED = FORMAT_STRING + " offline";
    private static final String USER_LOGGED_IN = FORMAT_STRING + " online";
    private static final String USER_CHATTED = FORMAT_STRING + ":\n%s";
    
    private static BroadcastChat instance;
    private final static Logger log = LoggerFactory.getLogger(BroadcastChat.class);
    
    public static synchronized BroadcastChat getInstance()
    {
        if (instance == null) instance = new BroadcastChat();
        return instance;
    }
    
    private BroadcastChat() {}
    
    /**
     * Вывод в чат сообщения об отключении - рассылаем всем пользователям on-line
     * @param user 
     * @param closedChannel 
     */
    public void userDisconnected(final User user, final Channel closedChannel)
    {
        log.trace("userDisconnected user={}, closedChannel={}", user, closedChannel);
        final String userExitMsg = String.format(
                USER_DISCONNECTED,
                user.getName(),
                new SimpleDateFormat(TIME_FORMAT).format(System.currentTimeMillis()),
                formatRemoteAddress(closedChannel.remoteAddress()));
        
        final NetworkMessage message = new NetworkMessage(NetworkMessage.Type.ChatNewMessage);
        message.put(NetworkMessage.TEXT, userExitMsg);
        ConnectionManager.getInstance().sendToAllOnline(message);
    }
    
    /**
     * Вывод в чат сообщения о подключении - рассылаем всем пользователям on-line
     * @param user 
     */
    public void userLoggedIn(final User user)
    {
        log.trace("userLoggedIn user={}", user);
        final String userConnectedMsg = String.format(
                USER_LOGGED_IN,
                user.getName(),
                new SimpleDateFormat(TIME_FORMAT).format(System.currentTimeMillis()),
                formatRemoteAddress(ConnectionManager.getInstance().getRemoteAddress(user)));
        
        final NetworkMessage message = new NetworkMessage(NetworkMessage.Type.ChatNewMessage);
        message.put(NetworkMessage.TEXT, userConnectedMsg);
        ConnectionManager.getInstance().sendToAllOnline(message);
    }
    
    /**
     * Вывод в чат сообщения пользователя - рассылаем всем пользователям on-line
     * @param user
     * @param chatMessage 
     */
    public void userChatted(final User user, final String chatMessage)
    {
        log.trace("userChatted user={}, chatMessage={}", user, chatMessage);
        final String newMessage = String.format(
                USER_CHATTED,
                user.getName(),
                new SimpleDateFormat(TIME_FORMAT).format(System.currentTimeMillis()),
                formatRemoteAddress(ConnectionManager.getInstance().getRemoteAddress(user)),
                chatMessage);
        
        final NetworkMessage message = new NetworkMessage(NetworkMessage.Type.ChatNewMessage);
        message.put(NetworkMessage.TEXT, newMessage);
        ConnectionManager.getInstance().sendToAllOnline(message);
    }
    
    private String formatRemoteAddress(final SocketAddress socketAddress)
    {
        if (socketAddress == null) return "";
        return socketAddress.toString().replace("/", "");
    }
    
}
