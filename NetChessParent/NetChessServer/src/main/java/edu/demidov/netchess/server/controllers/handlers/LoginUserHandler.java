package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.controllers.ClientUpdater;
import edu.demidov.netchess.server.model.exceptions.UserLoginException;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.users.User;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class LoginUserHandler implements NetworkMessageHandler
{

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final ClientUpdater clientUpdater = ClientUpdater.getInstance();
    
    private static LoginUserHandler instance;
    private final static Logger log = LoggerFactory.getLogger(LoginUserHandler.class);
    private final static Marker fatal = MarkerFactory.getMarker("FATAL");

    public static synchronized LoginUserHandler getInstance()
    {
        if (instance == null)
        {
            instance = new LoginUserHandler();
        }
        return instance;
    }

    private LoginUserHandler() {}

    /**
     * Принимает запрос о логине.
     * Вызывает аутентификацию и отправляет результат проверки
     * @param snm
     * @throws IllegalRequestParameter 
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter
    {
        log.trace("process snm={}", snm);
        try
        {
            // Получаем логин, пароль из сообщения
            final NetworkMessage netMsg = snm.getNetMsg();
            final String name = netMsg.getParam(NetworkMessage.NAME, String.class);
            final String passwordHash = netMsg.getParam(NetworkMessage.PASSWORD_HASH, String.class);

            final User user = connectionManager.loginUser(name, passwordHash, snm.getChannel());
            // Отправляем пользователю информацию о нём
            clientUpdater.sendAllInfoToUser(user, true);
        } catch (final UserLoginException ex)
        {
            log.trace("{}, snm={}", ex.getLocalizedMessage(), snm);
            // Отправляем ошибку
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.LoginUserError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendAndClose(snm.getChannel(), errMsg);
        } catch (final NoSuchAlgorithmException ex)
        {
            log.error(fatal, "process: no crypto algorythm found, snm={}", snm, ex);
        }
    }

}
