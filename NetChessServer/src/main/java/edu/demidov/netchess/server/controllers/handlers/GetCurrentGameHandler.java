package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.server.controllers.ClientUpdater;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCurrentGameHandler implements NetworkMessageHandler
{

    private final ClientUpdater clientUpdater = ClientUpdater.getInstance();
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    
    private static GetCurrentGameHandler instance;
    private final static Logger log = LoggerFactory.getLogger(GetCurrentGameHandler.class);

    public static synchronized GetCurrentGameHandler getInstance()
    {
        if (instance == null)
        {
            instance = new GetCurrentGameHandler();
        }
        return instance;
    }

    private GetCurrentGameHandler() {}

    /**
     * Отправляет пользователю его текущую игру, либо null, в случае её отсуствия
     * @param snm
     * @throws IllegalRequestParameter 
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter
    {
        log.trace("process snm={}", snm);
        final User sender = snm.getSender();
        connectionManager.sendToUser(sender, clientUpdater.getCurrentGameMsg(sender));
    }

}
