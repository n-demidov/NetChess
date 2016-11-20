package edu.demidov.netchess.server.controllers.handlers;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.game.chess.ChessAction;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.game.api.ChessLogic;
import edu.demidov.netchess.game.impl.ChessLogicImpl;
import edu.demidov.netchess.server.model.exceptions.NoSuchUserInGameException;
import edu.demidov.netchess.server.model.game.ChessGames;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.network.ServerNetworkMessage;
import edu.demidov.netchess.server.model.users.User;
import edu.demidov.netchess.utils.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameActionHandler implements NetworkMessageHandler
{
    private static final String NO_SUCH_USER_IN_GAME_EXCEPTION = "В игре нет игрока с именем '%s'";
    private static final String INCORRECT_MOVE_COORDINATES_EXCEPTION = "Получены некорректные координаты хода";
    private final static Logger log = LoggerFactory.getLogger(GameActionHandler.class);
    private static GameActionHandler instance;

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final ChessGames chessGames = ChessGames.getInstance();
    private final ChessLogic gameLogic = ChessLogicImpl.getInstance();

    public static synchronized GameActionHandler getInstance()
    {
        if (instance == null)
        {
            instance = new GameActionHandler();
        }
        return instance;
    }

    private GameActionHandler() {}

    @Override
    public void process(final ServerNetworkMessage snm) throws IllegalRequestParameter
    {
        log.trace("process snm={}", snm);
        final User user = snm.getSender();
        try
        {
            // Достаём объект игрового действия
            final ChessAction chessGameAction = snm.getNetMsg().getParam(NetworkMessage.GAME_ACTION, ChessAction.class);

            final ChessGame game = chessGames.getCurrentGame(user);
            if (game == null)
            {
                return;
            }

            final ChessPlayer player = chessGames.getPlayer(user, game);
            
            playerDoAction(player, chessGameAction, game);
        } catch (final GameMoveException ex)
        {
            log.trace("process: {}, snm={}", ex.getLocalizedMessage(), snm);

            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.GameActionError);
            errMsg.put(NetworkMessage.TEXT, ex.getLocalizedMessage());
            connectionManager.sendToUser(user, errMsg);
        } catch (final NoSuchUserInGameException e)
        {
            log.trace("NoSuchUserInGameException");

            final NetworkMessage errMsg = new NetworkMessage(NetworkMessage.Type.GameActionError);
            errMsg.put(
                    NetworkMessage.TEXT,
                    String.format(NO_SUCH_USER_IN_GAME_EXCEPTION, user.getName()));
            connectionManager.sendToUser(user, errMsg);
        }
    }

    /**
     * Обрабатывает сделанное игроком действие.
     * Возвращает true, если произошло игровое изменение и его надо отобразить игрокам
     */
    public void playerDoAction(final ChessPlayer player, final ChessAction chessAction, final ChessGame game)
            throws GameMoveException, IllegalRequestParameter
    {
        log.debug("playerDoAction player={}, chessAction={}, game={}", player, chessAction, game);

        assert player != null;
        assert game != null;

        switch (chessAction.getType())
        {
            case Move:
                processMove(player, game, chessAction);
                break;
            case ChooseFigureInsteadPawn:
                processChooseFigureInsteadPawn(player, game, chessAction);
                break;
            case Surrender:
                processSurrender(player, game);
                break;
            case OfferedDraw:
                processOfferedDraw(player, game);
                break;
            default:
                log.error("Critical error: IllegalArgumentException, playerDoAction player={}, chessAction={}", player, chessAction);
                throw new IllegalArgumentException();
        }
    }

    private void processMove(final ChessPlayer player, final ChessGame game,
                             final ChessAction chessAction) throws IllegalRequestParameter, GameMoveException
    {
        log.trace("processMove player={}, game={}, chessAction={}", player, game, chessAction);

        final Point[] points = chessAction.getPoints();
        if (points == null || points.length < 2)
        {
            throw new IllegalRequestParameter(INCORRECT_MOVE_COORDINATES_EXCEPTION);
        }

        final Point fromPoint = chessAction.getPoints()[0];
        final Point toPoint = chessAction.getPoints()[1];
        if (fromPoint == null || toPoint == null)
        {
            throw new IllegalRequestParameter(INCORRECT_MOVE_COORDINATES_EXCEPTION);
        }

        gameLogic.playerMoveFigure(player, game, fromPoint, toPoint);
    }

    private void processChooseFigureInsteadPawn(final ChessPlayer player,
                                                final ChessGame game, final ChessAction chessAction) throws GameMoveException, IllegalRequestParameter
    {
        log.trace("processChooseFigureInsteadPawn player={}, game={}, chessAction={}", player, game, chessAction);

        final ChessFigure.Type chosenFigureType = chessAction.getChooseFigureType();
        gameLogic.playerChooseFigureInsteadPawn(player, game, chosenFigureType);
    }

    private void processSurrender(final ChessPlayer player, final ChessGame game)
            throws GameMoveException
    {
        log.trace("processSurrender player={}, game={}", player, game);

        gameLogic.playerSurrender(player, game);
    }

    private void processOfferedDraw(final ChessPlayer offeredDrawPlayer, final ChessGame game)
    {
        log.trace("processOfferedDraw offeredDrawPlayer={}, game={}", offeredDrawPlayer, game);

        gameLogic.playerOfferedDraw(offeredDrawPlayer, game);
    }
}
