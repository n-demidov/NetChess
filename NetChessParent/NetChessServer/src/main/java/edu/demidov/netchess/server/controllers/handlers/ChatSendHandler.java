package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.model.BroadcastChat;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatSendHandler implements NetworkMessageHandler
{
    
    private final BroadcastChat chat = BroadcastChat.getInstance();

    private static ChatSendHandler instance;
    private final static Logger log = LoggerFactory.getLogger(ChatSendHandler.class);

    public static synchronized ChatSendHandler getInstance()
    {
        if (instance == null)
        {
            instance = new ChatSendHandler();
        }
        return instance;
    }

    private ChatSendHandler() {}

    /**
     * Принимает запрос об отправке сообщения в чат
     * @param snm
     * @throws IllegalRequestParameter 
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter
    {
        log.trace("process snm={}", snm);
        
        // Получаем текст из сообщения
        final String receivedMessage = snm.getNetMsg().getParam(NetworkMessage.CHAT_TEXT, String.class);

        // Рассылаем сообщение всем пользователям on-line
        chat.userChatted(snm.getSender(), receivedMessage);
    }
    
}
