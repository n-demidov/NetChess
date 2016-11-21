package edu.demidov.netchess.common.model.network;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class NetworkMessage implements Serializable {

    public static final String NAME = "name", PASSWORD_HASH = "passwordHash";
    public static final String TEXT = "text";
    public static final String USER = "user", USERS = "users";
    public static final String CHAT_TEXT = "text";
    public static final String INVITE_NAME = "inviteName", INVITES = "invites",
            INVITE_TYPE = "type", INVITE_TYPE_YES = "y", INVITE_TYPE_NO = "n";
    public static final String CURRENT_GAME = "game", GAME_ACTION = "action";
    public static final String MULTI_MESSAGES = "multi";
    private static final String NO_SUCH_KEY = "Некорректные параметры запроса (%s)";
    private static final String ILLEGAL_REQUEST_PARAMETERS = "Некорректные параметры запроса (%s:%s)";
    private final static Logger log = LoggerFactory.getLogger(NetworkMessage.class);
    private Type type;
    private Map<String, Object> map = new HashMap();

    public NetworkMessage() {
    }

    public NetworkMessage(final Type type) {
        assert type != null;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(final Map map) {
        this.map = map;
    }

    /**
     * Достаёт данные из сообщения.
     * В случае некорректных данных выбрасывает исключение:
     * - если в сообщении нет такого ключа;
     * - если значение null и isMayBeNull установлено в false;
     * - если значение не соответсвует ожидаемому типу
     *
     * @param <T>
     * @param key
     * @param clazz
     * @param isMayBeNull
     * @return
     * @throws edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter
     */
    public <T> T getParam(final String key, final Class<T> clazz, boolean isMayBeNull) throws IllegalRequestParameter {
        log.trace("getParam key={}, clazz={}, isMayBeNull={}", key, clazz, isMayBeNull);

        // Проверяем есть ли такой ключ
        if (!map.containsKey(key))
            throw new IllegalRequestParameter(String.format(NO_SUCH_KEY, key));
        final Object value = map.get(key);

        // Проверяем на null (по необходимости); а также на соответствие типа
        if (value == null && !isMayBeNull || value != null && value.getClass() != clazz) {
            log.trace("getParam IllegalRequestParameter key={}, clazz={}, isMayBeNull={}, value={}, map={}", key, clazz, isMayBeNull, value, map);
            throw new IllegalRequestParameter(String.format(ILLEGAL_REQUEST_PARAMETERS, key, value));
        }

        return (T) value;
    }

    public <T> T getParam(final String key, final Class<T> clazz) throws IllegalRequestParameter {
        return getParam(key, clazz, false);
    }

    /**
     * Записывает данные в сообщение
     *
     * @param key
     * @param object
     */
    public void put(final String key, final Object object) {
        map.put(key, object);
    }

    @Override
    public String toString() {
        return "NetworkMessage{"
                + "type=" + type
                + ", map=" + map
                + '}';
    }

    public enum Type {
        // For Server:
        CreateUser,
        LoginUser,

        ChatSend,

        GetOnlineUsers,

        InviteToPlay,
        GetIncomingInviters,
        InviteToPlayResponse,

        GetCurrentGame,
        DoAction,

        // For Client:
        SomeError,
        MultipleMessage,       // Несколько сообщений, упакованных в одно. MULTI_MESSAGE содержит Set сообщений.

        CreateUserError,
        LoginUserError,
        LoginUserSuccess,
        AuthError,

        ChatNewMessage,

        SendOnlineUsers,

        SendIncomingInvites,

        SendCurrentGame,
        GameActionError,

        // Other
        ConnectionClosed,
        ConnectionOpened,
    }

}
