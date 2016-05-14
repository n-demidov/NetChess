package edu.demidov.netchess.server.model.network;

import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.model.BroadcastChat;
import edu.demidov.netchess.server.model.Options;
import edu.demidov.netchess.server.model.exceptions.AccessConnectedUserException;
import edu.demidov.netchess.server.model.exceptions.IPAddressIsBanException;
import edu.demidov.netchess.server.model.exceptions.UserCreationException;
import edu.demidov.netchess.server.model.exceptions.UserLoginException;
import edu.demidov.netchess.server.model.users.AccountManager;
import edu.demidov.netchess.server.model.users.User;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Отвечает за контроль соединений для аккаунтов, прошедших логин.
 * Добавляет функционал к классу AccountManager (декоратор): методы createUser и loginUser.
 */
public class ConnectionManager
{

    private static final String CONNECTION_WILL_CLOSE
            = "Соединение будет закрыто, так как было открыто новое с адреса %s";
    private static final String ACCESS_CONNECTED_USER_EXCEPTION
            = "Соединение не установлено или было потеряно. Попробуйте перезайти.";
    private static final String CONNECTION_TTL_EXPIRED
            = "Соединение будет закрыто, так как не была произведена авторизация продолжительное время";
    private static final String IP_BAN_LIST_COMMENT_SYMBOL = "//";
    
    // Бан по ip
    private static final String BANED_IPS_FILE = "data/banned_ips.txt",
            ERROR_WHILE_PARSING_FILE_EXCEPTION
            = String.format("Возникла ошибка при считывании файла %s", BANED_IPS_FILE);
    private static final String IP_BANNED_EXCEPTION = "IP-адрес внесён в чёрный список";

    /* Все соединения храним в Map:
    Map<Channel, Connection> - хранит все каналы, включая те, с которых не был сделан логин
    (для них Connection.user = null).
    Map<User, Connection> - содержит только прошедших логин пользователей. Т.о. это все пользователи, которые сейчас онлайн.
    Это сделано для повышения скорости обработки в ущерб памяти.
    */
    private final Map<Channel, Connection> allConnections;
    private final Map<User, Connection> onlineUsers;
    private Date nextLaunch = Calendar.getInstance().getTime();
    
    private final AccountManager accountManager;    // Экземпляр AccountManager для управления аккаунтами
    private final BroadcastChat chat;
    private static ConnectionManager instance;
    private final static Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    
    public static synchronized ConnectionManager getInstance()
    {
        if (instance == null) instance = new ConnectionManager();
        return instance;
    }
    
    private ConnectionManager()
    {
        allConnections = new HashMap<>();
        onlineUsers = new HashMap<>();
        accountManager = AccountManager.getInstance();
        chat = BroadcastChat.getInstance();
    }
    
    /**
     * Вызывает создание нового аккаунта.
     * В случае успешного создания - пользователь сразу регистрируется онлайн.
     * @param userName
     * @param password
     * @param channel
     * @return 
     * @throws FileNotFoundException
     * @throws UserCreationException 
     * @throws java.security.NoSuchAlgorithmException 
     */
    public User createUser(final String userName, final String password, final Channel channel)
            throws FileNotFoundException, UserCreationException, NoSuchAlgorithmException
    {
        log.trace("createUser, userName={}, channel={}", userName, channel);
        try
        {
            final User logginedUser = accountManager.createUser(userName, password);
            
            // В случае успеха - обновляем содинение для этого пользователя
            updateChannel(logginedUser, channel);
            
            return logginedUser;
        } catch (final IPAddressIsBanException ex)
        {
            throw new UserCreationException(ex.getLocalizedMessage());
        }
    }
    
    /**
     * Устанавливает соединение.
     * Правильность логина обеспечивается IAccountManager.
     * После чего для этого пользователя установится\обновится соединение.
     * @param userName
     * @param password
     * @param channel
     * @return
     * @throws UserLoginException 
     * @throws java.security.NoSuchAlgorithmException 
     */
    public User loginUser(final String userName, final String password,
            final Channel channel) throws UserLoginException, NoSuchAlgorithmException
    {
        log.trace("loginUser, userName={}, channel={}", userName, channel);
        try
        {
            // Проходим процедуру логина
            final User logginedUser = accountManager.loginUser(userName, password);
            
            // В случае успеха - обновляем содинение для этого пользователя
            updateChannel(logginedUser, channel);
            
            return logginedUser;
        } catch (final IPAddressIsBanException ex)
        {
            throw new UserLoginException(ex.getLocalizedMessage());
        }
    }
    
    /**
     * Определяет было ли установлено соединение с переданным каналом.
     * И если да - то возвращает подключённого пользователя.
     * Метод должен вызываться только после проверки процедуры логина.
     * @param requestedChannel
     * @return
     * @throws AccessConnectedUserException 
     */
    public User accessConnectedUser(final Channel requestedChannel)
            throws AccessConnectedUserException
    {
        log.trace("accessConnectedUser requestedChannel={}", requestedChannel);
        
        final Connection connection = allConnections.getOrDefault(requestedChannel, null);

        // Проверяем совпадает ли канал sender'а с (реальным) открытым каналом для этого пользователя
        if (connection == null || connection.getUser() == null ||
                !requestedChannel.equals(connection.getChannel()))
        {
            log.trace("accessConnectedUser requestedChannel={}, {}", requestedChannel, ACCESS_CONNECTED_USER_EXCEPTION);
            throw new AccessConnectedUserException(ACCESS_CONNECTED_USER_EXCEPTION);
        }
        
        return connection.getUser();
    }

    /**
     * Отправляет пользователю сообщение
     * @param toUser
     * @param msg 
     */
    public void sendToUser(final User toUser, final NetworkMessage msg)
    {
        log.trace("sendToUser toUser={}, msg={}", toUser, msg);
        final Connection connection = onlineUsers.getOrDefault(toUser, null);
        if (connection == null) return;
        
        final Channel userChannel = connection.getChannel();
        
        if (userChannel.isActive()) userChannel.writeAndFlush(msg);
    }

    /**
     * Отправляет сообщение всем онлайн пользователям
     * @param msg 
     */
    public void sendToAllOnline(final NetworkMessage msg)
    {
        log.trace("sendToAllOnline msg={}", msg);
        
        for (Map.Entry<User, Connection> entry : onlineUsers.entrySet())
        {
            log.trace("sendToAllOnline channel.isActive={}, entry={}",
                      entry.getValue().getChannel().isActive(), entry);
            
            final Connection connection = entry.getValue();
            if (connection != null) sendToUser(connection.getUser(), msg);
        }
    }
    
    /**
     * Отправляет на канал сообщение и закрывает его.
     * Также удаляет это соединение из хеш-таблицы.
     * @param toChannel
     * @param msg 
     */
    public void sendAndClose(Channel toChannel, final Object msg)
    {
        log.trace("sendAndClose toChannel={}, msg={}", toChannel, msg);
        final ChannelFuture future = toChannel.writeAndFlush(msg);
        future.addListener(ChannelFutureListener.CLOSE);
        toChannel = null;
        
        final Connection connection = allConnections.getOrDefault(toChannel, null);
        if (connection != null) clearConnection(connection);
    }
    
    /**
     * Возвращает всех онлайн пользователей
     * @return 
     */
    public Set<User> getOnlineUsers()
    {
        return onlineUsers.keySet();
    }
    
    /** Возвращает true, если пользователь онлайн
     * @param user
     * @return 
     */
    public boolean isUserOnline(final User user)
    {
        return onlineUsers.getOrDefault(user, null) != null;
    }
    
    /**
     * Событие вызывается, когда удалённое соединение закрылось
     * @param channel 
     */
    public void connectionClosed(final Channel channel)
    {
        log.trace("connectionClosed channel={}", channel);
        // Если такой канал есть в хеш-таблице - вызываем метод disconnect для него
        final Connection connection = allConnections.getOrDefault(channel, null);
        if (connection != null) clearConnection(connection);
    }
    
    /**
     * Событие вызывается, когда удалённое соединение открылось
     * @param channel
     * @param timeReceived 
     * @throws edu.demidov.netchess.server.model.exceptions.IPAddressIsBanException 
     */
    public void connectionOpened(final Channel channel, final Date timeReceived) throws IPAddressIsBanException
    {
        log.trace("connectionOpened channel={}, timeReceived={}", channel, timeReceived);
        assert channel != null;
        
        checkBannedIP(channel);     // Проверяем ip-адрес на предмет бана
        
        allConnections.put(channel, new Connection(null, channel, timeReceived));
    }
    
    /**
     * Возвращает удалённый адрес пользователя (ip, port)
     * @param user
     * @return 
     */
    public SocketAddress getRemoteAddress(final User user)
    {
        final Connection connection = onlineUsers.getOrDefault(user, null);
        if (connection == null) return null;
        
        final Channel channel = connection.getChannel();
        return (channel != null) ? channel.remoteAddress() : null;
    }
    
    /**
     * Контроллирует канал.
     * Удаляем неактивные, или с истекшим TTL.
     * Метод запускает обработку раз в n секунд.
     */
    public void manageConnections()
    {
        if (nextLaunch.before(Calendar.getInstance().getTime()))
        {
            log.debug("manageConnections starts process by time");
            
            // Устанавливаем время следующей проверки TTL
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, Options.CONNECTIONS_FREQ_MANAGE_SECONDS);
            nextLaunch = calendar.getTime();
            
            // Находим разницу между текущим временем и INVITES_TTL_MIN
            calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, -Options.CONNECTION_UNAUTH_TTL_SECONDS);
            final Date curDeltaDate = calendar.getTime();
            
            final Iterator<Entry<Channel, Connection>> it = allConnections.entrySet().iterator();
            while (it.hasNext())
            {
                final Entry<Channel, Connection> entry = it.next();
                final Channel channel = entry.getKey();
                final Connection connection = entry.getValue();
               
                // Если канал неактивен - удаляем его
                if (!channel.isActive() || connection == null)
                {
                    log.trace("manageConnections: the channel not active or null - will remove, channel={}, connection={}", channel, connection);
                    it.remove();
                    if (connection != null) onlineUsers.remove(connection.getUser());
                    continue;
                }
                
                // Если канал не авторизован и висит дольше TTL - удаляем его
                if (connection.getUser() == null && connection.getOpenDate().before(curDeltaDate))
                {
                    // Закрываем канал, отправляем польз-лю сообщение, что старый канал закроется по причине TTL
                    log.trace("manageConnections: the channel not auth and TTL expired - will remove, channel={}, connection={}", channel, connection);
                    final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.SomeError);
                    netMsg.put(NetworkMessage.TEXT, CONNECTION_TTL_EXPIRED);
                    final ChannelFuture future = channel.writeAndFlush(netMsg);
                    future.addListener(ChannelFutureListener.CLOSE);
                    it.remove();
                }
            }
        }
    }
    
    /**
     * Возвращает кол-во секунд проведённое пользователям на сервере во время текущего соединения.
     * Если польз-ль не в сети - возвращает 0.
     * @param user
     * @return 
     */
    public int getUserCurrentTimeOnServer(final User user)
    {
        final Connection connection = onlineUsers.getOrDefault(user, null);
        if (connection == null) return 0;
        return (int) ((Calendar.getInstance().getTime().getTime() - connection.getOpenDate().getTime()) / 1000);
    }

    @Override
    public String toString()
    {
        return "ConnectionManager{" +
                "allConnections=" + allConnections +
                "onlineUsers=" + onlineUsers +
                '}';
    }
    
    /**
     * Обновляет текущий канал для пользователя.
     * Если пользователь уже был подключён - закрывает старый канал.
     * Метод должен вызываться только после процедуры проверки логина.
     * @param user
     * @param channel 
     */
    private void updateChannel(final User user, final Channel channel) throws IPAddressIsBanException
    {
        log.debug("updateChannel user={}, channel={}", user, channel);
        
        assert user != null;
        assert channel != null;
        
        checkBannedIP(channel);     // Проверяем ip-адрес на предмет бана
        
        /* Обрабатываем ситуации, если польз-ль залогинился не разрывая соединение:
        - с другого канала под тем же аккаунтом;
        - с этого же канала под др. аккаунтом
        */
        final Connection oldChannelConnection = allConnections.getOrDefault(channel, null);
        final Connection oldUserConnection = onlineUsers.getOrDefault(user, null);
        
        // Если для этого польз-я ранее был открыт другой канал - закрываем его
        if (oldUserConnection != null && !oldUserConnection.getChannel().equals(channel))
        {
            // Отправляем польз-лю сообщение, что старый канал закроется по причине логина с нового соединения
            final Channel oldChannel = oldUserConnection.getChannel();
            final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.SomeError);
            netMsg.put(
                    NetworkMessage.TEXT,
                    String.format(CONNECTION_WILL_CLOSE, channel.remoteAddress()));
            final ChannelFuture future = oldChannel.writeAndFlush(netMsg);
            future.addListener(ChannelFutureListener.CLOSE);
            
            clearConnection(oldUserConnection);
        }
        
        /* Если для этого канала уже был залогинен пользователь - удаляем его из хеш-таблицы/
        Также подсчитываем статистику для удалённого аккаунта
        */
        if (oldChannelConnection != null)
        {
            final User oldUser = oldChannelConnection.getUser();
            if (oldUser != null )
            {
                clearConnection(oldChannelConnection);
            }
        }

        // Обновляем канал для аккаунта
        final Connection newConnection = new Connection(user, channel, Calendar.getInstance().getTime());
        allConnections.put(channel, newConnection);
        onlineUsers.put(user, newConnection);
        
        // Вывод в чат сообщения о подключении - рассылаем всем пользователям on-line
        chat.userLoggedIn(user);
    }
    
    // Отсоединяет установленное соединение
    private void clearConnection(final Connection connection)
    {
        log.debug("clearConnection connection={}", connection);
        try
        {
            final Channel channel = connection.getChannel();
            final User user = connection.getUser();
            
            // Подсчитываем статистику проведенного пользователем времени на сервере
            if (user != null)
            {
                final long connectionSeconds
                    = (Calendar.getInstance().getTime().getTime() - connection.openDate.getTime()) / 1000;
                user.addTotalTimeOnServer(connectionSeconds);
                accountManager.updateUser(user);
            }

            // Удаляем из maps
            allConnections.remove(channel);
            onlineUsers.remove(user);

            // Вывод в чат сообщения об отключении - рассылаем всем пользователям on-line
            if (user != null) chat.userDisconnected(user, channel);
        } catch (final FileNotFoundException | UserLoginException ex) {
            log.error("clearConnection connection={}", connection, ex);
        }
    }

    // Проверяет не внесён ли ip в чёрный список
    private void checkBannedIP(final Channel channel) throws IPAddressIsBanException
    {
        log.debug("checkBannedIP channel={}", channel);
        
        final String channelIP = channel.localAddress().toString().substring(1);
        
        try (final BufferedReader br = new BufferedReader(new FileReader(BANED_IPS_FILE)))
        {
            // Считываем и обрабатываем строки из файла
            String s;
            while ((s = br.readLine()) != null)
            {
                s = s.trim();
                if (s.isEmpty() || s.startsWith(IP_BAN_LIST_COMMENT_SYMBOL)) continue;

                // Непосредственно проверяем логин в списке банов
                if (channelIP.startsWith(s))
                {
                    log.trace("checkBannedIP: ip in ban list, channel={}", channel);
                    throw new IPAddressIsBanException(IP_BANNED_EXCEPTION);
                }
            }
        } catch (final IOException ex)
        {
            log.error("checkBannedIP: channel={}, {}", channel, ERROR_WHILE_PARSING_FILE_EXCEPTION, ex);
        }
    }
    
    private static class Connection
    {
        
        private final User user;
        private final Channel channel;
        private final Date openDate;

        public Connection(final User user, final Channel channel, final Date openDate)
        {
            assert channel != null;
            assert openDate != null;
            
            this.user = user;
            this.channel = channel;
            this.openDate = openDate;
        }
        
        public User getUser()
        {
            return user;
        }

        public Channel getChannel()
        {
            return channel;
        }
        
        public Date getOpenDate()
        {
            return openDate;
        }

        @Override
        public String toString()
        {
            return "UserChannel{" +
                    "user=" + user +
                    ", channel=" + channel +
                    ", openDate=" + openDate
                    + '}';
        }

    }
    
}
