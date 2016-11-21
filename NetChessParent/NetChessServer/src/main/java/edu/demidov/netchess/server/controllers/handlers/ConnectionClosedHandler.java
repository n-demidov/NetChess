package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionClosedHandler implements NetworkMessageHandler {

    private final static Logger log = LoggerFactory.getLogger(ConnectionClosedHandler.class);
    private static ConnectionClosedHandler instance;
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    private ConnectionClosedHandler() {
    }

    public static synchronized ConnectionClosedHandler getInstance() {
        if (instance == null) {
            instance = new ConnectionClosedHandler();
        }
        return instance;
    }

    /**
     * Обрабатывает сообщение о том, что соединение с клиентом было закрыто.
     * Вызывает соответствующее событие у экзмепляра ConnectionManager.
     *
     * @param snm
     * @throws IllegalRequestParameter
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter {
        log.trace("process snm={}", snm);
        connectionManager.connectionClosed(snm.getChannel());
    }

}
