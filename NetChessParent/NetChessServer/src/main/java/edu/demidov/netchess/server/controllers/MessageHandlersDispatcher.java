package edu.demidov.netchess.server.controllers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.controllers.handlers.ChatSendHandler;
import edu.demidov.netchess.server.controllers.handlers.ConnectionClosedHandler;
import edu.demidov.netchess.server.controllers.handlers.ConnectionOpenedHandler;
import edu.demidov.netchess.server.controllers.handlers.CreateUserHandler;
import edu.demidov.netchess.server.controllers.handlers.GameActionHandler;
import edu.demidov.netchess.server.controllers.handlers.GetCurrentGameHandler;
import edu.demidov.netchess.server.controllers.handlers.GetIncomingInvitersHandler;
import edu.demidov.netchess.server.controllers.handlers.GetOnlineUsersHandler;
import edu.demidov.netchess.server.controllers.handlers.InviteToPlayHandler;
import edu.demidov.netchess.server.controllers.handlers.InviteToPlayResponseHandler;
import edu.demidov.netchess.server.controllers.handlers.LoginUserHandler;
import edu.demidov.netchess.server.controllers.handlers.NetworkMessageHandler;
import edu.demidov.netchess.server.model.exceptions.AccessConnectedUserException;
import edu.demidov.netchess.server.model.invitations.InvitationManager;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Класс отвечает за обработку сообщений ServerNetworkMessage:
 * - проверяет авторизацию канала;
 * - подбирает подходящий обработчик из хэш-таблицы (каждому типу должен соответствовать свой обраточик).
 */
public class MessageHandlersDispatcher
{

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    
    // Таблица соответствий типов сообщений (NetworkMessage) и обработчиков для них
    private final Map<NetworkMessage.Type, NetworkMessageHandler> HANDLERS;
    
    // Типы сообщений, для которых не надо проверять атворизацию. Для них sender будет null в ServerNetworkMessage.
    private final List<NetworkMessage.Type> EXCLUDED_CHECK_AUTH;

    private static MessageHandlersDispatcher instance;
    private final static Logger log = LoggerFactory.getLogger(MessageHandlersDispatcher.class);
    private final static Marker fatal = MarkerFactory.getMarker("FATAL");

    public MessageHandlersDispatcher(final InvitationManager invitationManager)
    {
        final InviteToPlayResponseHandler inviteToPlayResponseHandler = InviteToPlayResponseHandler.getInstance();
        inviteToPlayResponseHandler.setInviteManager(invitationManager);

        final InviteToPlayHandler inviteToPlayHandler = InviteToPlayHandler.getInstance();
        inviteToPlayHandler.setInviteManager(invitationManager);

        HANDLERS = new HashMap<>();
        HANDLERS.put(NetworkMessage.Type.LoginUser, LoginUserHandler.getInstance());
        HANDLERS.put(NetworkMessage.Type.CreateUser, CreateUserHandler.getInstance());
        HANDLERS.put(NetworkMessage.Type.ChatSend, ChatSendHandler.getInstance());
        HANDLERS.put(NetworkMessage.Type.GetOnlineUsers, GetOnlineUsersHandler.getInstance());

        HANDLERS.put(NetworkMessage.Type.InviteToPlay, inviteToPlayHandler);
        HANDLERS.put(NetworkMessage.Type.GetIncomingInviters, GetIncomingInvitersHandler.getInstance());
        HANDLERS.put(NetworkMessage.Type.InviteToPlayResponse, inviteToPlayResponseHandler);

        HANDLERS.put(NetworkMessage.Type.GetCurrentGame, GetCurrentGameHandler.getInstance());
        HANDLERS.put(NetworkMessage.Type.DoAction, GameActionHandler.getInstance());

        HANDLERS.put(NetworkMessage.Type.ConnectionClosed, ConnectionClosedHandler.getInstance());
        HANDLERS.put(NetworkMessage.Type.ConnectionOpened, ConnectionOpenedHandler.getInstance());

        EXCLUDED_CHECK_AUTH = new ArrayList<>();
        EXCLUDED_CHECK_AUTH.add(NetworkMessage.Type.CreateUser);
        EXCLUDED_CHECK_AUTH.add(NetworkMessage.Type.LoginUser);
        EXCLUDED_CHECK_AUTH.add(NetworkMessage.Type.ConnectionOpened);
        EXCLUDED_CHECK_AUTH.add(NetworkMessage.Type.ConnectionClosed);
    }
    
    /**
     * Обрабатывает сообщение (ServerNetworkMessage).
     * Ищет и вызывает нужный обработчик для сообщения, если нужно - проверяет авторизацию.
     * @param snm 
     */
    public void process(final ServerNetworkMessage snm)
    {
        log.trace("process snm={}", snm);
        try
        {
            // Вызываем проверку авторизации для канала
            checkAuthConnection(snm);
            // Ищем нужный обработчик клиентского сообщения
            dispatchMessageToHandler(snm);
        } catch (final AccessConnectedUserException ex)
        {
            // Отправляем ошибку
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.AuthError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendAndClose(snm.getChannel(), errMsg);
        } catch (final IllegalRequestParameter ex)
        {
            log.trace("process: illegal request parameter from client: {}, snm={}", ex.getLocalizedMessage(), snm);
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.SomeError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendAndClose(snm.getChannel(), errMsg);
        }
    }
    
    /**
     * Проверяет авторизацию установленного подключения.
     * Если всё ок - добавляет найденного пользователя в  экзмепляр ServerNetworkMessage,
     * иначе выбрасывает исключение.
     * Не проверяет авторизацию для некоторых типов запросов.
     */
    private void checkAuthConnection(final ServerNetworkMessage snm)
            throws AccessConnectedUserException
    {
        log.trace("checkAuthConnection snm={}", snm);
        final NetworkMessage.Type typeMsg = snm.getNetMsg().getType();
        
        // Исключения, для которых проводить проверку не надо. Для них польз-ль будет null.
        if (EXCLUDED_CHECK_AUTH.contains(typeMsg))
        {
            log.trace("checkAuthConnection: cancel auth (typeMsg={}), snm={}", typeMsg, snm);
            return;
        } 

        snm.setSender(connectionManager.accessConnectedUser(snm.getChannel()));
    }
    
    // Ищет и вызывает нужный обработчик для сообщения
    private void dispatchMessageToHandler(final ServerNetworkMessage snm)
            throws IllegalRequestParameter
    {
        log.trace("dispatchMessage snm={}", snm);
        final NetworkMessage.Type msgType = snm.getNetMsg().getType();
        
        if (HANDLERS.containsKey(msgType))
        {
            HANDLERS.get(msgType).process(snm);
        } else
        {
            log.error(fatal, "process: can't find handler for '{}' NetworkMessage.Type"
                    + " (in the map of handlers); snm={}", msgType, snm);
        }
    }

}
