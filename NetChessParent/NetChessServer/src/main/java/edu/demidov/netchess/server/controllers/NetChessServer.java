package edu.demidov.netchess.server.controllers;

import edu.demidov.netchess.common.model.network.MessageQueue;
import edu.demidov.netchess.game.api.ChessLogic;
import edu.demidov.netchess.game.impl.ChessLogicImpl;
import edu.demidov.netchess.game.rules.impl.ChessRulesImpl;
import edu.demidov.netchess.server.model.Options;
import edu.demidov.netchess.server.model.game.ChessGames;
import edu.demidov.netchess.server.model.invitations.Invitations;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.network.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class NetChessServer {

    private static final long SLEEP = 1000L;
    private final static Logger log = LoggerFactory.getLogger(NetChessServer.class);
    private final static Marker fatal = MarkerFactory.getMarker("FATAL");
    private static NetChessServer instance;
    private NettyServer nettyServer;
    private MessageQueue<ServerNetworkMessage> messageQueue;
    private MessageHandlersDispatcher handlersDispatcher;
    private ConnectionManager connectionManager;
    private Invitations inviteManager;
    private ChessGames chessGames;
    private ClientUpdater clientUpdater;

    private NetChessServer() {
        configureObjects();
    }

    public static synchronized NetChessServer getInstance() {
        if (instance == null) {
            instance = new NetChessServer();
        }
        return instance;
    }

    public static void main(final String[] args) {
        try {
            NetChessServer.getInstance().start();
        } catch (final InterruptedException ex) {
            log.error(fatal, "main args={}", args, ex);
            System.out.println(ex.getLocalizedMessage());
        }
    }

    /**
     * Запускает игровой сервер.
     * Запускает сервер по заданному ip и порту. И ожидает подключения клиентов.
     *
     * @throws InterruptedException
     */
    public void start() throws InterruptedException {
        log.info("start");

        nettyServer.run();

        // Запускаем цикл по обработке сообщений
        log.info("ready");
        boolean isActive = true;
        while (isActive) {
            // Обрабатываем входящие сообщения с клиента и от NettyServer
            while (messageQueue.hasMessages()) {
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

    private void configureObjects() {
        log.info("configureObjects");

        chessGames = ChessGames.getInstance();
        final ChessLogic gameLogic = ChessLogicImpl.getInstance();
        gameLogic.setChessRules(new ChessRulesImpl());
        gameLogic.addListener(chessGames);

        inviteManager = new Invitations(
                Options.INVITATIONS_FREQ_MANAGE_MINUTES,
                Options.INVITATIONS_TTL_MINUTES);
        inviteManager.addListener(chessGames);

        nettyServer = NettyServer.getInstance();
        messageQueue = MessageQueue.getInstance();
        handlersDispatcher = new MessageHandlersDispatcher(inviteManager);
        connectionManager = ConnectionManager.getInstance();
        clientUpdater = ClientUpdater.getInstance();
        clientUpdater.setChessGames(chessGames);
        clientUpdater.setInviteManager(inviteManager);
    }

}
