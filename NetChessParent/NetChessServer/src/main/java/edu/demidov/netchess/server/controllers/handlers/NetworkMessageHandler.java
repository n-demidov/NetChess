package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;

/**
 * Интерфейс для обработчиков-контроллеров сообщений (ServerNetworkMessage)
 */
public interface NetworkMessageHandler {

    /**
     * Обрабатывает ответ, пришедший с клиента
     *
     * @param snm
     * @throws edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter
     */
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter;

}
