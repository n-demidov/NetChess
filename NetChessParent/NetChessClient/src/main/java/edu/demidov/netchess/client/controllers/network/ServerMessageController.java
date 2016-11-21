package edu.demidov.netchess.client.controllers.network;

import edu.demidov.netchess.client.controllers.window.MainWindowController;
import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.network.MessageQueue;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.common.model.users.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Класс обрабатывает приходящие сообщения, взятые из очереди сообщений (с сервера + события канала)
 */
public class ServerMessageController implements Runnable {

    private static final long SLEEP = 500L;
    private static final String SERVER_CHAT = "<Server>: ";
    private static final String RESPONSE_READING_EXCEPTION = "<Ошибка при чтении ответа с сервера>: ";
    private final static Logger log = LoggerFactory.getLogger(ServerMessageController.class);
    private final static Marker fatal = MarkerFactory.getMarker("FATAL");
    private final MessageQueue<NetworkMessage> messageQueue = MessageQueue.getInstance();
    private final MainWindowController mainAppCntrl;
    private volatile boolean isActive = true;

    /**
     * Конструктор принимает контроллер главного окна
     *
     * @param mainAppCntrl
     */
    public ServerMessageController(final MainWindowController mainAppCntrl) {
        this.mainAppCntrl = mainAppCntrl;
    }

    @Override
    public void run() {
        log.debug("run");
        try {
            start();
        } catch (final InterruptedException ex) {
            log.error(fatal, "run", ex);
        }
    }

    /**
     * Останавливает цикл обработки сообщений от сервера
     */
    public void stop() {
        log.debug("stop");
        isActive = false;
    }

    /**
     * Главный цикл, обрабатывает сообщения от сервера
     *
     * @throws InterruptedException
     */
    private void start() throws InterruptedException {
        log.debug("start");
        while (isActive) {
            // Обработчик пришедших с сервера сообщений
            while (messageQueue.hasMessages()) {
                final NetworkMessage takeMessage = messageQueue.takeMessage();
                log.debug("from messageQueue taken: {}", takeMessage);
                process(takeMessage);
            }
            log.trace("messageQueue is empty");

            // Усыпляем поток
            Thread.sleep(SLEEP);
        }
    }

    // Обработчик сообщения
    private void process(final NetworkMessage netMsg) {
        log.debug("process netMsg={}", netMsg);
        // Обработка сообщения
        switch (netMsg.getType()) {
            case LoginUserSuccess:
                processLoginUserSuccess(netMsg);
                break;
            case SomeError:
            case CreateUserError:
            case LoginUserError:
            case AuthError:
            case ConnectionClosed:
                processServerError(netMsg);
                break;
            case ChatNewMessage:
                processChatMsg(netMsg);
                break;
            case SendOnlineUsers:
                processSendOnlineUsers(netMsg);
                break;

            case SendIncomingInvites:
                processSendIncomingInvites(netMsg);
                break;

            case SendCurrentGame:
                processSendCurrentGame(netMsg);
                break;
            case GameActionError:
                processGameError(netMsg);
                break;

            case MultipleMessage:
                processMultipleMessage(netMsg);
                break;

            default:
                log.error(fatal, "process: unknown NetworkMessage type; netMsg={}", netMsg);
                break;
        }
    }

    // Выводим сообщение в чат
    private void processChatMsg(final NetworkMessage netMsg) {
        log.debug("processChatMsg netMsg={}", netMsg);
        try {
            final String newChatMsg = netMsg.getParam(NetworkMessage.TEXT, String.class);
            mainAppCntrl.addToChat(newChatMsg);
        } catch (final IllegalRequestParameter ex) {
            log.error(fatal, "exception, netMsg={}", netMsg, ex);
            mainAppCntrl.addToChat(RESPONSE_READING_EXCEPTION + ex.getLocalizedMessage());
        }
    }

    private void processServerError(final NetworkMessage netMsg) {
        log.debug("processServerError netMsg={}", netMsg);
        try {
            final String err = netMsg.getParam(NetworkMessage.TEXT, String.class);
            mainAppCntrl.addToChat(SERVER_CHAT + err);
        } catch (final IllegalRequestParameter ex) {
            log.error(fatal, "exception, netMsg={}", netMsg, ex);
            mainAppCntrl.addToChat(RESPONSE_READING_EXCEPTION + ex.getLocalizedMessage());
        }
    }

    private void processLoginUserSuccess(final NetworkMessage netMsg) {
        log.debug("processLoginUserSuccess netMsg={}", netMsg);
        try {
            // Получаем объект UserProfile
            final UserProfile userProfile = netMsg.getParam(NetworkMessage.USER, UserProfile.class);

            // Посылаем событие на контроллер клиента
            mainAppCntrl.loginInfoUpdated(userProfile);
        } catch (final IllegalRequestParameter ex) {
            log.error(fatal, "exception, netMsg={}", netMsg, ex);
            mainAppCntrl.addToChat(RESPONSE_READING_EXCEPTION + ex.getLocalizedMessage());
        }
    }

    private void processSendOnlineUsers(final NetworkMessage netMsg) {
        log.debug("processSendOnlineUsers netMsg={}", netMsg);
        try {
            final Set<UserProfile> onlineUserProfiles
                    = netMsg.getParam(NetworkMessage.USERS, HashSet.class);
            mainAppCntrl.onlineUserProfilesChanged(onlineUserProfiles);
        } catch (final IllegalRequestParameter ex) {
            log.error(fatal, "exception, netMsg={}", netMsg, ex);
            mainAppCntrl.addToChat(RESPONSE_READING_EXCEPTION + ex.getLocalizedMessage());
        }
    }

    private void processSendIncomingInvites(final NetworkMessage netMsg) {
        log.debug("processSendIncomingInvites netMsg={}", netMsg);
        try {
            final Set<UserProfile> userProfilesInvites
                    = netMsg.getParam(NetworkMessage.INVITES, HashSet.class);
            mainAppCntrl.incomingInvitersChanged(userProfilesInvites);
        } catch (final IllegalRequestParameter ex) {
            log.error(fatal, "exception, netMsg={}", netMsg, ex);
            mainAppCntrl.addToChat(RESPONSE_READING_EXCEPTION + ex.getLocalizedMessage());
        }
    }

    private void processSendCurrentGame(final NetworkMessage netMsg) {
        log.debug("processSendCurrentGame netMsg={}", netMsg);
        try {
            final ChessGame game = netMsg.getParam(NetworkMessage.CURRENT_GAME, ChessGame.class, true);
            mainAppCntrl.currentGameUpdated(game);
        } catch (final IllegalRequestParameter ex) {
            log.error(fatal, "exception, netMsg={}", netMsg, ex);
            mainAppCntrl.addToChat(RESPONSE_READING_EXCEPTION + ex.getLocalizedMessage());
        }
    }

    private void processGameError(final NetworkMessage netMsg) {
        log.debug("processGameError netMsg={}", netMsg);
        try {
            final String text = netMsg.getParam(NetworkMessage.TEXT, String.class);
            mainAppCntrl.gameError(text);
        } catch (final IllegalRequestParameter ex) {
            log.error(fatal, "exception, netMsg={}", netMsg, ex);
            mainAppCntrl.addToChat(RESPONSE_READING_EXCEPTION + ex.getLocalizedMessage());
        }
    }

    private void processMultipleMessage(final NetworkMessage netMsg) {
        log.debug("processMultipleMessage netMsg={}", netMsg);
        try {
            final Set<NetworkMessage> messages = netMsg.getParam(NetworkMessage.MULTI_MESSAGES, HashSet.class);

            // Распаковываем сообщение - для каждого вложенного вызываем обработчик
            for (final NetworkMessage subMsg : messages) process(subMsg);
        } catch (final IllegalRequestParameter ex) {
            log.error(fatal, "exception, netMsg={}", netMsg, ex);
            mainAppCntrl.addToChat(RESPONSE_READING_EXCEPTION + ex.getLocalizedMessage());
        }
    }

}
