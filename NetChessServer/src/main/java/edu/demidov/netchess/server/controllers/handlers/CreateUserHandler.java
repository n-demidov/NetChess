package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.controllers.ClientUpdater;
import edu.demidov.netchess.server.model.exceptions.UserCreationException;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.users.User;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class CreateUserHandler implements NetworkMessageHandler
{

    private final ClientUpdater clientUpdater = ClientUpdater.getInstance();
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    private static CreateUserHandler instance;
    private final static Logger log = LoggerFactory.getLogger(CreateUserHandler.class);
    private final static Marker fatal = MarkerFactory.getMarker("FATAL");

    public static synchronized CreateUserHandler getInstance()
    {
        if (instance == null)
        {
            instance = new CreateUserHandler();
        }
        return instance;
    }

    private CreateUserHandler()
    {
    }

    /**
     * Принимает запрос о регистрации нового аккаунта
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

            final User user = connectionManager.createUser(name, passwordHash, snm.getChannel());
            // Отправляем пользователю информацию о нём
            clientUpdater.sendAllInfoToUser(user, true);
        } catch (final UserCreationException ex)
        {
            log.trace("{}, snm={}", ex.getLocalizedMessage(), snm);
            // Отправляем ошибку
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.CreateUserError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendAndClose(snm.getChannel(), errMsg);
        } catch (final FileNotFoundException ex)
        {
            log.error(fatal, "process: persistence file of users not found!, snm={}", snm, ex);
        } catch (final NoSuchAlgorithmException ex)
        {
            log.error(fatal, "process: no crypto algorythm found, snm={}", snm, ex);
        }
    }

}
