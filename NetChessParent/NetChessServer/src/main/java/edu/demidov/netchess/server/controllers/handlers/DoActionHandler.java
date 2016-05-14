package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.game.chess.ChessAction;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.server.controllers.game.chess.GameController;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoActionHandler implements NetworkMessageHandler
{

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final GameController gameController = GameController.getInstance();
    
    private static DoActionHandler instance;
    private final static Logger log = LoggerFactory.getLogger(DoActionHandler.class);

    public static synchronized DoActionHandler getInstance()
    {
        if (instance == null)
        {
            instance = new DoActionHandler();
        }
        return instance;
    }

    private DoActionHandler() {}

    /**
     * Принимает и передаёт объект игрового действия в GameController.
     * В случае получения игрового исключения - отправит его на клиент.
     * @param snm
     * @throws IllegalRequestParameter 
     */
    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter
    {
        log.trace("process snm={}", snm);
        final User sender = snm.getSender();
        try
        {
            // Достаём объект игрового действия
            final ChessAction chessAction = snm.getNetMsg().getParam(NetworkMessage.GAME_ACTION, ChessAction.class);
            
            gameController.playerDoAction(sender, chessAction);
        } catch (final GameMoveException ex)
        {
            log.trace("process: {}, snm={}", ex.getLocalizedMessage(), snm);
            // Отправляем ошибку на клиент
            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.GameActionError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendToUser(sender, errMsg);
        }
    }

}
