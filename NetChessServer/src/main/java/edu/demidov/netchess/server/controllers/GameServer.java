package edu.demidov.netchess.server.controllers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.game.chess.ChessAction;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.network.MessageQueue;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.common.model.users.UserProfile;
import edu.demidov.netchess.server.controllers.game.chess.GameController;
import edu.demidov.netchess.server.model.BroadcastChat;
import edu.demidov.netchess.server.model.Options;
import edu.demidov.netchess.server.model.exceptions.AccessConnectedUserException;
import edu.demidov.netchess.server.model.exceptions.IPAddressIsBanException;
import edu.demidov.netchess.server.model.exceptions.NoSuchUserException;
import edu.demidov.netchess.server.model.exceptions.OneOfUserIsPlayingException;
import edu.demidov.netchess.server.model.exceptions.UserCreationException;
import edu.demidov.netchess.server.model.exceptions.UserLoginException;
import edu.demidov.netchess.server.model.game.GameChangedObserver;
import edu.demidov.netchess.server.model.game.GameManager;
import edu.demidov.netchess.server.model.invitations.InvitationManager;
import edu.demidov.netchess.server.model.invitations.InvitationObserver;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.network.netty.NettyServer;
import edu.demidov.netchess.server.model.users.AccountManager;
import edu.demidov.netchess.server.model.users.User;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class GameServer implements InvitationObserver, GameChangedObserver
{

    private static final long SLEEP = 1000L;
    
    private final NettyServer nettyServer;
    private final MessageQueue<ServerNetworkMessage> messageQueue;
    private final ConnectionManager connectionManager;
    private final AccountManager accountManager;
    private final InvitationManager inviteManager;
    private final GameController gameController;
    private final GameManager gameManager;
    private final BroadcastChat chat = BroadcastChat.getInstance();
    private Date nextLaunch = Calendar.getInstance().getTime();
    private final static Logger log = LoggerFactory.getLogger(GameServer.class);
    private final static Marker fatal = MarkerFactory.getMarker("FATAL");

    public GameServer()
    {
        log.info("GameServer");
        
        // Инициилизация объектов
        nettyServer = NettyServer.getInstance();
        messageQueue = MessageQueue.getInstance();
        accountManager = AccountManager.getInstance();
        connectionManager = ConnectionManager.getInstance();
        inviteManager = InvitationManager.getInstance();
        gameController = GameController.getInstance();
        gameManager = GameManager.getInstance();
    }
    
    public static void main(final String[] args)
    {
        try
        {
            new GameServer().start();
        } catch (final InterruptedException | FileNotFoundException ex)
        {
            log.error(fatal, "main args={}", args, ex);
            System.out.println(ex.getLocalizedMessage());
        }
    }
    
    /**
     * Запускает игровой сервер.
     * Запускает сервер по заданному ip и порту. И ожидает подключения клиентов.
     * @throws InterruptedException
     * @throws FileNotFoundException 
     */
    public void start() throws InterruptedException, FileNotFoundException
    {
        log.info("start");
        
        inviteManager.addListener(this);
        gameManager.addListener(this);
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
            manageSendInfoForOnlineUsers();

            // Усыпляем поток
            Thread.sleep(SLEEP);
        }

        // Закрываем NettyServer
        nettyServer.stop();
    }
    
    /**
     * Срабатывает, когда два игрока договорились сыграть друг с другом
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
    
    /**
     * Событие вызывается, когда произошло какое-либо изменение игры.
     * Метод рассылает обновленный объект игры игрокам.
     * @param game 
     */
    @Override
    public void gameChanged(final ChessGame game)
    {
        log.debug("gameChanged game={}", game);
        // Отправляет игру всем игрокам
        final NetworkMessage gameMsg = new NetworkMessage(NetworkMessage.Type.SendCurrentGame);
        gameMsg.put(NetworkMessage.CURRENT_GAME, game);

        for (final User playingUser : gameManager.getPlayingUsers(game))
            connectionManager.sendToUser(playingUser, gameMsg);
    }

    // Обрабатывает входящее сообщение
    private void process(final ServerNetworkMessage snm) throws FileNotFoundException
    {
        log.trace("process snm={}", snm);
        final NetworkMessage netMsg = snm.getNetMsg();
        
        try
        {
            // Вызываем проверку подключения
            checkAuthConnection(snm);
            
            // Обработка сообщения
            switch (netMsg.getType())
            {
                case CreateUser:
                    processCreateUser(snm);
                    break;
                case LoginUser:
                    processLoginUser(snm);
                    break;
                case ChatSend:
                    processChatSend(snm);
                    break;
                case GetOnlineUsers:
                    processGetOnlineUsers(snm);
                    break;
                    
                case InviteToPlay:
                    processInviteToPlay(snm);
                    break;
                case GetIncomingInviters:
                    processGetIncomingInviters(snm);
                    break;
                case InviteToPlayResponse:
                    processInviteToPlayResponse(snm);
                    break;
                case GetCurrentGame:
                    processGetCurrentGame(snm);
                    break;
                case DoAction:
                    processDoAction(snm);
                    break;
                    
                case ConnectionClosed:
                    processConnectionClosed(snm);
                    break;
                case ConnectionOpened:
                    processConnectionOpened(snm);
                    break;
                default:
                    log.error(fatal, "process: IllegalArgumentException, snm={}", snm);
                    return;
            }
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
        } catch (final NoSuchAlgorithmException ex)
        {
            log.error(fatal, "process: no crypto algorythm found, snm={}", snm, ex);
        }
    }

    private void processCreateUser(final ServerNetworkMessage snm) throws FileNotFoundException,
            IllegalRequestParameter, NoSuchAlgorithmException
    {
        log.trace("processCreateUser snm={}", snm);
        try
        {
            // Получаем логин, пароль из сообщения
            final NetworkMessage netMsg = snm.getNetMsg();
            final String name = netMsg.getParam(NetworkMessage.NAME, String.class);
            final String passwordHash = netMsg.getParam(NetworkMessage.PASSWORD_HASH, String.class);
        
            final User user = connectionManager.createUser(name, passwordHash, snm.getChannel());
            userLogined(user);
        } catch (final UserCreationException ex)
        {
            log.trace("{}, snm={}", ex.getLocalizedMessage(), snm);
            // Отправляем ошибку
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.CreateUserError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendAndClose(snm.getChannel(), errMsg);
        }
    }

    private void processLoginUser(final ServerNetworkMessage snm) throws IllegalRequestParameter,
            NoSuchAlgorithmException
    {
        log.trace("processLoginUser snm={}", snm);
        try
        {
            // Получаем логин, пароль из сообщения
            final NetworkMessage netMsg = snm.getNetMsg();
            final String name = netMsg.getParam(NetworkMessage.NAME, String.class);
            final String passwordHash = netMsg.getParam(NetworkMessage.PASSWORD_HASH, String.class);

            final User user = connectionManager.loginUser(name, passwordHash, snm.getChannel());
            userLogined(user);
        } catch (final UserLoginException ex)
        {
            log.trace("{}, snm={}", ex.getLocalizedMessage(), snm);
            // Отправляем ошибку
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.LoginUserError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendAndClose(snm.getChannel(), errMsg);
        }
    }
    
    private void userLogined(final User user)
    {
        log.trace("userLogined user={}", user);
        // Отправляем пользователю информацию о нём
        sendToUserAllInfo(user, true);
    }

    private void processChatSend(final ServerNetworkMessage snm)
            throws IllegalRequestParameter
    {
        log.trace("processChatSend snm={}", snm);
        
        // Получаем текст из сообщения
        final String receivedMessage = snm.getNetMsg().getParam(NetworkMessage.CHAT_TEXT, String.class);

        // Рассылаем сообщение всем пользователям on-line
        chat.userChatted(snm.getSender(), receivedMessage);
    }
    
    /**
     * Проверяет авторизацию установленного подключения.
     * Если всё ок - добавляет найденного пользователя в  экзмепляр ServerNetworkMessage,
     * иначе выбрасывает исключение.
     * Не проверяет авторизацию для некоторых типов запросов.
     */
    private void checkAuthConnection(final ServerNetworkMessage snm) throws AccessConnectedUserException
    {
        log.trace("checkAuthConnection snm={}", snm);
        final NetworkMessage.Type typeMsg = snm.getNetMsg().getType();
        // Исключения, для которых проводить проверку не надо. Для них польз-ль может быть null.
        if (typeMsg == NetworkMessage.Type.CreateUser ||
            typeMsg == NetworkMessage.Type.LoginUser ||
            typeMsg == NetworkMessage.Type.ConnectionOpened ||
            typeMsg == NetworkMessage.Type.ConnectionClosed)
        {
            log.trace("checkAuthConnection: cancel auth (typeMsg={}), snm={}", typeMsg, snm);
            return;
        } 

        snm.setSender(connectionManager.accessConnectedUser(snm.getChannel()));
    }

    private void processConnectionClosed(final ServerNetworkMessage snm)
    {
        log.trace("processConnectionClosed snm={}", snm);
        connectionManager.connectionClosed(snm.getChannel());
        
    }
    
    private void processConnectionOpened(final ServerNetworkMessage snm)
    {
        log.trace("processConnectionOpened snm={}", snm);
        try
        {
            connectionManager.connectionOpened(snm.getChannel(), snm.getTimeReceived());
        } catch (final IPAddressIsBanException ex)
        {
            log.trace("processConnectionOpened: {}, snm={}", ex.getLocalizedMessage(), snm);
            // Отправляем ошибку
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.SomeError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendAndClose(snm.getChannel(), errMsg);
        }
    }
    
    private void processGetOnlineUsers(final ServerNetworkMessage snm)
    {
        log.trace("processGetOnlineUsers snm={}", snm);
        final User sender = snm.getSender();
        connectionManager.sendToUser(sender, getGetOnlineUsersMsg(sender));
    }

    private void processInviteToPlay(final ServerNetworkMessage snm) 
            throws IllegalRequestParameter
    {
        log.trace("processInviteToPlay snm={}", snm);
        try
        {
            final String targetUserName = snm.getNetMsg().getParam(NetworkMessage.INVITE_NAME, String.class);
            final String type = snm.getNetMsg().getParam(NetworkMessage.INVITE_TYPE, String.class);
            final boolean isAccept = type.equals(NetworkMessage.INVITE_TYPE_YES);
        
            final User targetUser = accountManager.getUser(targetUserName);
            final User sender = snm.getSender();
            
            // Обновляем приглашения
            if (isAccept)
            {
                inviteManager.invite(sender, targetUser);
            } else
            {
                inviteManager.cancelInvite(sender, targetUser);
            }
        } catch (final NoSuchUserException ex)
        {
            // В случае ненахождения такого пользователя ничего не делаем
            log.warn("processInviteToPlay: NoSuchUserException snm={}", snm);
        }
    }

    private void processGetIncomingInviters(final ServerNetworkMessage snm)
    {
        log.trace("processGetIncomingInviters snm={}", snm);
        final User sender = snm.getSender();
        connectionManager.sendToUser(sender, getIncomingInvitersMsg(sender));
    }

    private void processInviteToPlayResponse(final ServerNetworkMessage snm)
            throws IllegalRequestParameter
    {
        log.trace("processInviteToPlayResponse snm={}", snm);
        try
        {
            final String offerrerUserName = snm.getNetMsg().getParam(NetworkMessage.INVITE_NAME, String.class);
            final String type = snm.getNetMsg().getParam(NetworkMessage.INVITE_TYPE, String.class);
            final boolean isAccept = type.equals(NetworkMessage.INVITE_TYPE_YES);
        
            final User offerrerUser = accountManager.getUser(offerrerUserName);
            final User sender = snm.getSender();
            
            // Обновляем приглашения
            if (isAccept)
            {
                inviteManager.acceptIncomingInvite(offerrerUser, sender);
            } else
            {
                inviteManager.rejectIncomingInvite(offerrerUser, sender);
            }
        } catch (final NoSuchUserException ex)
        {
            // В случае ненахождения такого пользователя ничего не делаем
            log.warn("processInviteToPlayResponse: NoSuchUserException snm={}", snm);
        }
    }

    private void processGetCurrentGame(final ServerNetworkMessage snm)
    {
        log.trace("processGetCurrentGame snm={}", snm);
        final User sender = snm.getSender();
        connectionManager.sendToUser(sender, getCurrentGameMsg(sender));
    }
    
    private void processDoAction(final ServerNetworkMessage snm)
            throws IllegalRequestParameter
    {
        log.trace("processDoAction snm={}", snm);
        final User sender = snm.getSender();
        try
        {
            // Достаём объект игрового действия
            final ChessAction chessAction = snm.getNetMsg().getParam(NetworkMessage.GAME_ACTION, ChessAction.class);
            
            gameController.playerDoAction(sender, chessAction);
        } catch (final GameMoveException ex)
        {
            log.trace("processDoAction: {}, snm={}", ex.getLocalizedMessage(), snm);
            // Отправляем ошибку на клиент
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.GameActionError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendToUser(sender, errMsg);
        }
    }
    
    // Периодически отправляет онлайн пользователям обновления информации
    private void manageSendInfoForOnlineUsers()
    {
        // Запускаем раз в n секунд
        if (nextLaunch.before(Calendar.getInstance().getTime()))
        {
            log.trace("manageSendInfoForOnlineUsers starts process by time");
            
            // Устанавливаем время следующей рассылки
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, Options.USERS_INFO_SEND_UPDATES_SECONDS);
            nextLaunch = calendar.getTime();
            
            //  Рассылаем каждому пользователю инфу
            for (final User user : connectionManager.getOnlineUsers())
                sendToUserAllInfo(user, false);
        }
    }
    
    // Отправляет пользователю информацию о нём: профайл, лобби, приглашения, игру
    private void sendToUserAllInfo(final User user, final boolean isSendCurrentGame)
    {
        log.trace("sendToUserAllInfo user={}, isSendCurrentGame={}", user, isSendCurrentGame);
        final Set<NetworkMessage> messages = new HashSet<>();
        
        // Получаем нужные сообщения
        messages.add(getUserProfileMsg(user));
        messages.add(getGetOnlineUsersMsg(user));
        messages.add(getIncomingInvitersMsg(user));
        if (isSendCurrentGame) messages.add(getCurrentGameMsg(user));
        
        // Упаковываем сообщения в одно
        final NetworkMessage multiMsg = new NetworkMessage(NetworkMessage.Type.MultipleMessage);
        multiMsg.put(NetworkMessage.MULTI_MESSAGES, messages);
        connectionManager.sendToUser(user, multiMsg);
    }
    
    // Возвращает сообщение - профиль пользователя
    private NetworkMessage getUserProfileMsg(final User user)
    {
        log.trace("getUserProfileMsg user={}", user);
        final NetworkMessage msgUserProfile = new NetworkMessage(NetworkMessage.Type.LoginUserSuccess);
        msgUserProfile.put(NetworkMessage.USER, userProfile(user));
        return msgUserProfile;
    }
    
    // Возвращает сообщение - список онлайн пользователей
    private NetworkMessage getGetOnlineUsersMsg(final User sender)
    {
        log.trace("getGetOnlineUsersMsg sender={}", sender);
        // Получаем всех онлайн пользователей
        final Set<User> onlineUsers = connectionManager.getOnlineUsers();
        
        // Переводим объекты User в UserProfile. Также нет смысла отправлять себя.
        final Set<UserProfile> onlineUserProfiles = new HashSet<>();
        for (final User user : onlineUsers)
        {
            if (!sender.equals(user))
            {
                onlineUserProfiles.add(userProfile(
                        user,
                        inviteManager.isInvited(sender, user),
                        gameManager.isUserPlaying(user)
                ));
            }
        }
        
        // Отправляем сообщение на клиент
        final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.SendOnlineUsers);
        netMsg.put(NetworkMessage.USERS, onlineUserProfiles);
        return netMsg;
    }
    
    // Возвращает сообщение - множество пригласивших игроков
    private NetworkMessage getIncomingInvitersMsg(final User sender)
    {
        log.trace("getIncomingInvitersMsg sender={}", sender);
        final Set<User> inviters = inviteManager.getIncomingInviters(sender);
        final Set<UserProfile> userProfilesInvites = new HashSet<>();
        
        // Переводим объекты User в объекты UserProfile
        for (final User user : inviters)
        {
            userProfilesInvites.add(userProfile(
                        user,
                        inviteManager.isInvited(sender, user),
                        gameManager.isUserPlaying(user))
            );
        }
        
        // Отправляем сообщение на клиент
        final NetworkMessage response = new NetworkMessage(NetworkMessage.Type.SendIncomingInvites);
        response.put(NetworkMessage.INVITES, userProfilesInvites);
        return response;
    }
    
    // Возвращает сообщение - текущую игру пользователя
    private NetworkMessage getCurrentGameMsg(final User user)
    {
        log.trace("getCurrentGameMsg user={}", user);
        final ChessGame game = gameManager.getCurrentGame(user);
        
        final NetworkMessage gameMsg = new NetworkMessage(NetworkMessage.Type.SendCurrentGame);
        gameMsg.put(NetworkMessage.CURRENT_GAME, game);
        return gameMsg;
    }
    
    // Создаёт объект UserProfile из объекта User
    private UserProfile userProfile(final User user, final boolean isInvited, final boolean isPlaying)
    {
        return new UserProfile(user.getName(), user.getRank(),
                user.getWins(), user.getDefeats(), user.getDraws(),
                user.getTotalTimeOnServer() + connectionManager.getUserCurrentTimeOnServer(user),
                isInvited, isPlaying
        );
    }
    
    // Создаёт объект UserProfile из объекта User
    private UserProfile userProfile(final User user)
    {
        return userProfile(user, false, false);
    }

}
