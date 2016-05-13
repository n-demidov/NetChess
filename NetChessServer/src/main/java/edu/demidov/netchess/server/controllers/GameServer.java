package edu.demidov.netchess.server.controllers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.network.MessageQueue;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.controllers.game.chess.GameController;
import edu.demidov.netchess.server.model.exceptions.AccessConnectedUserException;
import edu.demidov.netchess.server.model.exceptions.OneOfUserIsPlayingException;
import edu.demidov.netchess.server.model.game.GameManager;
import edu.demidov.netchess.server.model.invitations.InvitationManager;
import edu.demidov.netchess.server.model.invitations.InvitationObserver;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.network.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import edu.demidov.netchess.server.controllers.handlers.MessageHandlersDispatcher;
import edu.demidov.netchess.server.model.users.User;

public class GameServer implements InvitationObserver
{

    private static final long SLEEP = 1000L;
    
    private final NettyServer nettyServer;
    private final MessageQueue<ServerNetworkMessage> messageQueue;
    private final MessageHandlersDispatcher handlersDispatcher;
    private final ConnectionManager connectionManager;
    private final InvitationManager inviteManager;
    private final GameController gameController;
    private final GameManager gameManager;
    private final ClientUpdater clientUpdater;
    
    private final static Logger log = LoggerFactory.getLogger(GameServer.class);
    private final static Marker fatal = MarkerFactory.getMarker("FATAL");
    private static GameServer instance;

    public static synchronized GameServer getInstance()
    {
        if (instance == null)
        {
            instance = new GameServer();
        }
        return instance;
    }
    
    private GameServer()
    {
        log.info("GameServer");
        
        // Инициилизация объектов
        nettyServer = NettyServer.getInstance();
        messageQueue = MessageQueue.getInstance();
        handlersDispatcher = MessageHandlersDispatcher.getInstance();
        connectionManager = ConnectionManager.getInstance();
        inviteManager = InvitationManager.getInstance();
        gameController = GameController.getInstance();
        gameManager = GameManager.getInstance();
        clientUpdater = ClientUpdater.getInstance();
    }
    
    public static void main(final String[] args)
    {
        try
        {
            GameServer.getInstance().start();
        } catch (final InterruptedException ex)
        {
            log.error(fatal, "main args={}", args, ex);
            System.out.println(ex.getLocalizedMessage());
        }
    }
    
    /**
     * Запускает игровой сервер.
     * Запускает сервер по заданному ip и порту. И ожидает подключения клиентов.
     * @throws InterruptedException 
     */
    public void start() throws InterruptedException
    {
        log.info("start");
        
        inviteManager.addListener(this);
        gameManager.addListener(clientUpdater);
        nettyServer.run();

        // Запускаем цикл по обработке сообщений
        log.info("ready");
        boolean isActive = true;
        while (isActive)
        {
            // Обрабатываем входящие сообщения с клиента и от NettyServer
            while (messageQueue.hasMessages())
            {
                final ServerNetworkMessage snm = messageQueue.takeMessage();
                log.trace("from messageQueue taken {}", snm);
                process(snm);
            }
            log.trace("messageQueue is empty");
            
            connectionManager.manageConnections();
            inviteManager.checkTTLs();          // Вызываем обработчик временем жизни приглашений
            gameManager.manageGamesTime();      // Вызываем обработчик времени партий
            clientUpdater.manageSendInfoForOnlineUsers();

            // Усыпляем поток
            Thread.sleep(SLEEP);
        }

        // Закрываем NettyServer
        nettyServer.stop();
    }
    
    /**
     * Событие вызывается, когда два игрока договорились сыграть друг с другом
     * @param player1
     * @param player2 
     */
    @Override
    public void usersAgreed(final User player1, final User player2)
    {
        log.debug("usersAgreed player1={}, player2={}", player1, player2);
        try
        {
            // Если один из игроков не в сети - игру запускать не надо
            if (!connectionManager.isUserOnline(player1) ||
                !connectionManager.isUserOnline(player2))
                return;
            
            gameController.startGame(player1, player2);
        } catch (final OneOfUserIsPlayingException ex)
        {
            // Если один из игроков играет - ничего не делаем
            log.trace("usersAgreed: one of user already playing - cancel new game,  player1={}, player2={}", player1, player2);
        }
    }

    // Обрабатывает входящее сообщение
    private void process(final ServerNetworkMessage snm)
    {
        log.trace("process snm={}", snm);
        
        try
        {
            handlersDispatcher.process(snm);
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
    
}
