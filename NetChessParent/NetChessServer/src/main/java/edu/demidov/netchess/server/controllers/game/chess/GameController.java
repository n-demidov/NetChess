package edu.demidov.netchess.server.controllers.game.chess;

import edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter;
import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.server.model.game.GameManager;
import edu.demidov.netchess.common.model.game.chess.ChessAction;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import edu.demidov.netchess.server.model.exceptions.OneOfUserIsPlayingException;
import edu.demidov.netchess.server.model.exceptions.NoSuchUserInGameException;
import edu.demidov.netchess.server.model.users.User;
import edu.demidov.netchess.utils.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameController
{
    
    private static final String NO_SUCH_USER_IN_GAME_EXCEPTION = "В игре нет игрока с именем '%s'";
    private static final String INCORRECT_MOVE_COORDINATES_EXCEPTION = "Получены некорректные координаты хода";

    private static GameController instance;
    private final GameManager gameManager;
    private final static Logger log = LoggerFactory.getLogger(GameController.class);
    
    public static synchronized GameController getInstance()
    {
        if (instance == null) instance = new GameController();
        return instance;
    }
    
    private GameController()
    {
        gameManager = GameManager.getInstance();
    }
    
    /**
     * Создаёт новую игру для игроков
     * @param user1
     * @param user2
     * @throws OneOfUserIsPlayingException 
     */
    public void startGame(final User user1, final User user2)
            throws OneOfUserIsPlayingException
    {
        log.trace("startGame user1={}, user2={}", user1, user2);
        gameManager.startGame(user1, user2);
    }
    
    /**
     * Обрабатывает сделанное игроком действие.
     * Возвращает true, если произошло игровое изменение и его надо отобразить игрокам
     * @param user
     * @param chessAction 

     * @throws GameMoveException 
     * @throws edu.demidov.netchess.common.model.exceptions.IllegalRequestParameter 
     */
    public void playerDoAction(final User user, final ChessAction chessAction)
            throws GameMoveException, IllegalRequestParameter
    {
        log.debug("playerDoAction user={}, chessAction={}", user, chessAction);
        try
        {
            assert user != null;
            
            final ChessGame game = gameManager.getCurrentGame(user);
            if (game == null) return;
            final ChessPlayer player = gameManager.getPlayer(user, game);
            
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
                    log.error("Critical error: IllegalArgumentException, playerDoAction user={}, chessAction={}", user, chessAction);
                    throw new IllegalArgumentException();
            }
        } catch (final NoSuchUserInGameException ex)
        {
            log.debug("{}: playerDoAction user={}, chessAction={}", NO_SUCH_USER_IN_GAME_EXCEPTION, user, chessAction);
            throw new GameMoveException(String.format(NO_SUCH_USER_IN_GAME_EXCEPTION, user.getName()));
        }
    }

    private void processMove(final ChessPlayer player, final ChessGame game,
            final ChessAction chessAction) throws IllegalRequestParameter, GameMoveException
    {
        log.trace("processMove player={}, game={}, chessAction={}", player, game, chessAction);
        // Проверяем полученные координаты движения
        final Point[] points = chessAction.getPoints();
        if (points == null || points.length < 2)
            throw new IllegalRequestParameter(INCORRECT_MOVE_COORDINATES_EXCEPTION);
        
        final Point fromPoint = chessAction.getPoints()[0];
        final Point toPoint = chessAction.getPoints()[1];
        if (fromPoint == null || toPoint == null)
            throw new IllegalRequestParameter(INCORRECT_MOVE_COORDINATES_EXCEPTION);
        
        gameManager.playerMoveFigure(player, game, fromPoint, toPoint);
    }
    
    private void processChooseFigureInsteadPawn(final ChessPlayer player,
            final ChessGame game, final ChessAction chessAction) throws GameMoveException, IllegalRequestParameter
    {
        log.trace("processChooseFigureInsteadPawn player={}, game={}, chessAction={}", player, game, chessAction);
        // Проверяем полученный тип фигуры
        final ChessFigure.Type chosenFigureType = chessAction.getChooseFigureType();
        if (chosenFigureType == null)
                throw new IllegalRequestParameter(GameManager.UNKNOWN_FIGURE_TYPE_EXCEPTION);
        
        gameManager.playerChooseFigureInsteadPawn(player, game, chosenFigureType);
    }
    
    // Игрок сдался
    private void processSurrender(final ChessPlayer player, final ChessGame game)
            throws GameMoveException
    {
        log.trace("processSurrender player={}, game={}", player, game);
        gameManager.playerSurrender(player, game);
    }
    
    // Игрок предложил ничью. Возвращает true, если изменилось состояние игры
    private void processOfferedDraw(final ChessPlayer offeredDrawPlayer, final ChessGame game)
    {
        log.trace("processOfferedDraw offeredDrawPlayer={}, game={}", offeredDrawPlayer, game);
        gameManager.playerOfferedDraw(offeredDrawPlayer, game);
    }
    
}
