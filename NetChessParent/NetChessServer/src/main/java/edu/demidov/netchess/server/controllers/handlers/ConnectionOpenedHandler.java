package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.model.exceptions.IPAddressIsBanException;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionOpenedHandler implements NetworkMessageHandler {

    private final static Logger log = LoggerFactory.getLogger(ConnectionOpenedHandler.class);
    private static ConnectionOpenedHandler instance;
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    private ConnectionOpenedHandler() {
    }

    public static synchronized ConnectionOpenedHandler getInstance() {
        if (instance == null) {
            instance = new ConnectionOpenedHandler();
        }
        return instance;
    }

    /**
     * Обрабатывает сообщение о том, что новый клиент подключился к серверу.
     * Вызывает соответствующее событие у экзмепляра ConnectionManager.
     *
     * @param snm
     * @throws IllegalRequestParameter
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter {
        log.trace("process snm={}", snm);
        try {
            connectionManager.connectionOpened(snm.getChannel(), snm.getTimeReceived());
        } catch (final IPAddressIsBanException ex) {
            log.trace("process: {}, snm={}", ex.getLocalizedMessage(), snm);
            // Отправляем ошибку
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.SomeError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendAndClose(snm.getChannel(), errMsg);
        }
    }

}
