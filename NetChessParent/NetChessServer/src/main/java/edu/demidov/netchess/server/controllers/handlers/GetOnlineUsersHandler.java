package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.server.controllers.ClientUpdater;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetOnlineUsersHandler implements NetworkMessageHandler
{

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final ClientUpdater clientUpdater = ClientUpdater.getInstance();
    
    private static GetOnlineUsersHandler instance;
    private final static Logger log = LoggerFactory.getLogger(GetOnlineUsersHandler.class);

    public static synchronized GetOnlineUsersHandler getInstance()
    {
        if (instance == null)
        {
            instance = new GetOnlineUsersHandler();
        }
        return instance;
    }

    private GetOnlineUsersHandler() {}

    /**
     * Отправляет пользователю список он-лайн игроков
     * @param snm
     * @throws IllegalRequestParameter 
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter
    {
        log.trace("process snm={}", snm);
        final User sender = snm.getSender();
        connectionManager.sendToUser(sender, clientUpdater.getOnlineUsersMsg(sender));
    }

}
