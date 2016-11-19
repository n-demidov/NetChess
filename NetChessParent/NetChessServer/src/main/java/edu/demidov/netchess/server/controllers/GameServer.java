package edu.demidov.netchess.server.controllers;

import edu.demidov.netchess.common.model.network.MessageQueue;
import edu.demidov.netchess.game.logic.api.ChessLogic;
import edu.demidov.netchess.game.logic.impl.ChessLogicImpl;
import edu.demidov.netchess.server.model.game.ChessGames;
import edu.demidov.netchess.server.model.invitations.InvitationManager;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.network.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class GameServer
{
    private static final long SLEEP = 1000L;
    
    private final NettyServer nettyServer;
    private final MessageQueue<ServerNetworkMessage> messageQueue;
    private final MessageHandlersDispatcher handlersDispatcher;
    private final ConnectionManager connectionManager;
    private final InvitationManager inviteManager;
    private final ChessGames chessGames;
    private final ChessLogic gameLogic;
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
        chessGames = ChessGames.getInstance();
        gameLogic = ChessLogicImpl.getInstance();
        clientUpdater = ClientUpdater.getInstance();
        clientUpdater.setChessGames(chessGames);
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

        inviteManager.addListener(chessGames);
        gameLogic.addListener(chessGames);

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
                handlersDispatcher.process(snm);    // Обрабатывает входящее сообщение
            }
            log.trace("messageQueue is empty");
            
            connectionManager.manageConnections();
            inviteManager.checkTTLs();          // Вызываем обработчик временем жизни приглашений
            chessGames.manageGamesTime();      // Вызываем обработчик времени партий
            clientUpdater.checkSendingUpdates();

            // Усыпляем поток
            Thread.sleep(SLEEP);
        }

        // Закрываем NettyServer
        nettyServer.stop();
    }
}
