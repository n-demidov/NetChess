package edu.demidov.netchess.server.controllers;

import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.common.model.users.UserProfile;
import edu.demidov.netchess.server.model.Options;
import edu.demidov.netchess.server.model.game.GameChangedObserver;
import edu.demidov.netchess.server.model.game.GameManager;
import edu.demidov.netchess.server.model.invitations.InvitationManager;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.users.User;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс отправляет обновления на клиент
 */
public class ClientUpdater implements GameChangedObserver
{
    
    private Date nextLaunch = Calendar.getInstance().getTime();
    
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final InvitationManager inviteManager = InvitationManager.getInstance();
    private final GameManager gameManager = GameManager.getInstance();
    
    private static ClientUpdater instance;
    private final static Logger log = LoggerFactory.getLogger(ClientUpdater.class);
    
    public static synchronized ClientUpdater getInstance()
    {
        if (instance == null)
        {
            instance = new ClientUpdater();
        }
        return instance;
    }
    
    private ClientUpdater() {}

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
    
    /**
     * Периодически рассылает онлайн-пользователям обновления информации.
     * Класс ClientUpdater сам учитывает время, метод можно вызывать кажду игровую итерацию.
     */
    public void manageSendInfoForOnlineUsers()
    {
        if (nextLaunch.before(Calendar.getInstance().getTime()))
        {
            log.trace("manageSendInfoForOnlineUsers starts process by time");
            
            // Устанавливаем время следующей рассылки
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, Options.USERS_INFO_SEND_UPDATES_SECONDS);
            nextLaunch = calendar.getTime();
            
            //  Рассылаем каждому пользователю инфу
            for (final User user : connectionManager.getOnlineUsers())
                sendAllInfoToUser(user, false);
        }
    }
    
    /**
     * Отправляет пользователю всю информацию: профайл, лобби, приглашения, текущую игру
     * @param user
     * @param isSendCurrentGame 
     */
    public void sendAllInfoToUser(final User user, final boolean isSendCurrentGame)
    {
        log.trace("sendToUserAllInfo user={}, isSendCurrentGame={}", user, isSendCurrentGame);
        final Set<NetworkMessage> messages = new HashSet<>();
        
        // Получаем нужные сообщения
        messages.add(getUserProfileMsg(user));
        messages.add(getOnlineUsersMsg(user));
        messages.add(getIncomingInvitersMsg(user));
        if (isSendCurrentGame) messages.add(getCurrentGameMsg(user));
        
        // Упаковываем сообщения в одно
        final NetworkMessage multiMsg = new NetworkMessage(NetworkMessage.Type.MultipleMessage);
        multiMsg.put(NetworkMessage.MULTI_MESSAGES, messages);
        connectionManager.sendToUser(user, multiMsg);
    }
    
    /**
     * Возвращает сообщение - список онлайн пользователей
     * @param forUser
     * @return 
     */
    public NetworkMessage getOnlineUsersMsg(final User forUser)
    {
        log.trace("getGetOnlineUsersMsg sender={}", forUser);
        // Получаем всех онлайн пользователей
        final Set<User> onlineUsers = connectionManager.getOnlineUsers();
        
        // Переводим объекты User в UserProfile. Также нет смысла отправлять себя.
        final Set<UserProfile> onlineUserProfiles = new HashSet<>();
        for (final User user : onlineUsers)
        {
            if (!forUser.equals(user))
            {
                onlineUserProfiles.add(userProfile(
                        user,
                        inviteManager.isInvited(forUser, user),
                        gameManager.isUserPlaying(user)
                ));
            }
        }
        
        // Отправляем сообщение на клиент
        final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.SendOnlineUsers);
        netMsg.put(NetworkMessage.USERS, onlineUserProfiles);
        return netMsg;
    }
    
    /**
     * Возвращает сообщение - множество пригласивших игроков
     * @param user
     * @return 
     */
    public NetworkMessage getIncomingInvitersMsg(final User user)
    {
        log.trace("getIncomingInvitersMsg sender={}", user);
        final Set<User> inviters = inviteManager.getIncomingInviters(user);
        final Set<UserProfile> userProfilesInvites = new HashSet<>();
        
        // Переводим объекты User в объекты UserProfile
        for (final User inviter : inviters)
        {
            userProfilesInvites.add(userProfile(
                        inviter,
                        inviteManager.isInvited(user, inviter),
                        gameManager.isUserPlaying(inviter))
            );
        }
        
        // Отправляем сообщение на клиент
        final NetworkMessage response = new NetworkMessage(NetworkMessage.Type.SendIncomingInvites);
        response.put(NetworkMessage.INVITES, userProfilesInvites);
        return response;
    }
    
    /**
     * Возвращает сообщение - текущую игру пользователя
     * @param user
     * @return 
     */
    public NetworkMessage getCurrentGameMsg(final User user)
    {
        log.trace("getCurrentGameMsg user={}", user);
        final ChessGame game = gameManager.getCurrentGame(user);
        
        final NetworkMessage gameMsg = new NetworkMessage(NetworkMessage.Type.SendCurrentGame);
        gameMsg.put(NetworkMessage.CURRENT_GAME, game);
        return gameMsg;
    }
    
    // Возвращает сообщение - профиль пользователя
    private NetworkMessage getUserProfileMsg(final User user)
    {
        log.trace("getUserProfileMsg user={}", user);
        final NetworkMessage msgUserProfile = new NetworkMessage(NetworkMessage.Type.LoginUserSuccess);
        msgUserProfile.put(NetworkMessage.USER, userProfile(user));
        return msgUserProfile;
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
