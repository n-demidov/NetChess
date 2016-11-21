package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.server.controllers.ClientUpdater;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetIncomingInvitersHandler implements NetworkMessageHandler {

    private final static Logger log = LoggerFactory.getLogger(GetIncomingInvitersHandler.class);
    private static GetIncomingInvitersHandler instance;
    private final ClientUpdater clientUpdater = ClientUpdater.getInstance();
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    private GetIncomingInvitersHandler() {
    }

    public static synchronized GetIncomingInvitersHandler getInstance() {
        if (instance == null) {
            instance = new GetIncomingInvitersHandler();
        }
        return instance;
    }

    /**
     * Отправляет пользователю список пригласивших его игроков
     *
     * @param snm
     * @throws IllegalRequestParameter
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter {
        log.trace("process snm={}", snm);
        final User sender = snm.getSender();
        connectionManager.sendToUser(sender, clientUpdater.getIncomingInvitersMsg(sender));
    }

}
