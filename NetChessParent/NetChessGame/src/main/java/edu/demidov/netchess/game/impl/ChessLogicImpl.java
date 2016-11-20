package edu.demidov.netchess.game.impl;

import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidBoardSizeException;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidPointException;
import edu.demidov.netchess.game.exceptions.NoKingOnFieldException;
import edu.demidov.netchess.common.model.exceptions.game.chess.NoNextPlayerFoundException;
import edu.demidov.netchess.common.model.game.chess.ChessField;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import edu.demidov.netchess.game.rules.ChessRules;
import edu.demidov.netchess.game.rules.impl.ChessRulesImpl;
import edu.demidov.netchess.game.api.ChessLogic;
import edu.demidov.netchess.game.api.ChessLogicObserver;
import edu.demidov.netchess.utils.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class ChessLogicImpl implements ChessLogic
{
    private static final String UNKNOWN_FIGURE_TYPE_EXCEPTION = "Неизвестный тип фигуры. Выберите ферзя, ладью, слона или коня.";
    private static final String MATE = "Мат";
    private static final String TIME_IS_UP = "Время истекло";
    private static final String OPPONENT_SURRENDER = "Оппонент сдался";
    private static final String STALEMATE  = "Пат";
    private static final String PLAYERS_AGREED_TO_DRAW = "Оппоненты согласились на ничью";
    private static final String GAME_ALREADY_FINISHED = "Партия уже завершена";
    private static final String CURRENT_PLAYER_MUST_CHOOSE_FIGURE_EXCEPTION = "Не удалось найти пешку на доске";
    private static final String NO_NEXT_PLAYER_FOUND_EXCEPTION = "Не удалось найти следующего игрока";
    private final static String MOVE_SEQUENCE_EXCEPTION = "Дождитесь своего хода. Сейчас ходит игрок '%s'.";

    private static final Random RANDOM = new Random();
    private static final int RANDOM_ID_MAX = 999_999_999;
    private static final int CHESS_SIZE = 8;

    private static final Logger log = LoggerFactory.getLogger(ChessLogicImpl.class);
    private static ChessLogicImpl instance;
    private final List<ChessLogicObserver> listeners;
    private final ChessRules chessRules = new ChessRulesImpl();

    public static synchronized ChessLogicImpl getInstance()
    {
        if (instance == null)
        {
            instance = new ChessLogicImpl();
        }
        return instance;
    }

    private ChessLogicImpl()
    {
        listeners = new ArrayList<>();
    }

    @Override
    public void addListener(final ChessLogicObserver listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final ChessLogicObserver listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void notifyListenersThatGameChanged(final ChessGame game)
    {
        log.trace("notifyListenersThatGameChanged game={}", game);

        for (final ChessLogicObserver listener : listeners)
        {
            listener.gameChanged(game);
        }
    }

    @Override
    public void notifyListenersThatGameEnded(final ChessGame game, final ChessPlayer winner)
    {
        for (final ChessLogicObserver listener : listeners)
        {
            listener.gameEnded(game, winner);
        }
    }

    @Override
    public ChessGame startGame(final List<ChessPlayer> chessPlayers)
    {
        log.debug("startGame chessPlayers={}", chessPlayers);

        ChessGame game = null;

        try
        {
            final ChessField field = new ChessField(CHESS_SIZE);
            game = new ChessGame(
                    RANDOM.nextInt(RANDOM_ID_MAX),
                    field,
                    chessPlayers);
        } catch (final InvalidBoardSizeException | NoNextPlayerFoundException | InvalidPointException e)
        {
            log.error("Exception, ", e);
        }

        assert game != null;
        return game;
    }

    @Override
    public void playerMoveFigure(final ChessPlayer player, final ChessGame game,
                                 final Point fromPoint, final Point toPoint) throws GameMoveException
    {
        log.debug("playerMoveFigure player={}, game={}, fromPoint={}, toPoint={}", player, game, fromPoint, toPoint);
        try
        {
            final long currentMoveTime = countCurrentMoveMilliseconds(game);  // Подсчитываем время текущего хода

            checkThatGameNotFinished(game);
            checkThatPlayerIsCurrent(player, game);

            // Если текущий игрок должен выбрать новую фигуру вместо пешки - выходим
            if (game.isCurrentPlayerChoosingFigure())
                throw new GameMoveException(CURRENT_PLAYER_MUST_CHOOSE_FIGURE_EXCEPTION);

            // Если ход не корректный (и почему-то не выкинулось исключение с описанием) - выходим
            if (!chessRules.isMoveCorrect(player.getColor(), game.getField(), fromPoint, toPoint))
                return;

            // Делаем сам ход
            game.moveFigure(fromPoint, toPoint);

            processIfCurrentPlayerChanged(game, player, currentMoveTime);
        } catch (final NoKingOnFieldException | NoNextPlayerFoundException | InvalidPointException ex)
        {
            log.warn("Exception: player={}, game={}, fromPoint={}, toPoint={}", player, game, fromPoint, toPoint, ex);
            throw new GameMoveException(ex.toString());
        }

        notifyListenersThatGameChanged(game);
    }

    @Override
    public void playerChooseFigureInsteadPawn(final ChessPlayer player,
                         final ChessGame game, final ChessFigure.Type chosenFigureType) throws GameMoveException
    {
        log.trace("playerChooseFigureInsteadPawn player={}, game={}, chosenFigureType={}", player, game, chosenFigureType);

        try
        {
            final long currentMoveTime = countCurrentMoveMilliseconds(game);  // Подсчитываем время текущего хода

            checkThatGameNotFinished(game);
            checkThatPlayerIsCurrent(player, game);

            // Проверяем может ли текущий игрок выбрать фигуру
            if (!game.isCurrentPlayerChoosingFigure())
            {
                return;
            }

            checkChooseFigureInsteadPawn(chosenFigureType);

            // Заменяем пешку игрока на выбранную фигуру
            game.transformPawn(player, chosenFigureType);

            processIfCurrentPlayerChanged(game, player, currentMoveTime);
        } catch (final InvalidPointException | NoKingOnFieldException | NoNextPlayerFoundException ex)
        {
            log.error("Exception: game={}, player={}, chosenFigureType={}", game, player, chosenFigureType, ex);
            throw new GameMoveException(ex.toString());
        }

        notifyListenersThatGameChanged(game);
    }

    @Override
    public void playerSurrender(final ChessPlayer player, final ChessGame game)
            throws GameMoveException
    {
        log.trace("playerSurrender, game={}, player={}", game, player);

        try
        {
            endGame(game, game.getNextPlayer(player), OPPONENT_SURRENDER);
        } catch (final NoNextPlayerFoundException ex)
        {
            log.error("Exception: game={}, player={}", game, player, ex);
            throw new GameMoveException(NO_NEXT_PLAYER_FOUND_EXCEPTION);
        }
    }

    @Override
    public void playerOfferedDraw(final ChessPlayer offeredDrawPlayer, final ChessGame game)
    {
        log.trace("playerOfferedDraw, offeredDrawPlayer={}, game={}", offeredDrawPlayer, game);

        if (offeredDrawPlayer.isOfferedDraw())
        {
            return;
        }

        offeredDrawPlayer.setOfferedDraw(true);

        // Подсчитываем все ли игроки согласны на ничью
        boolean isAllUsersWantDraw = true;
        for (final ChessPlayer otherPlayer : game.getPlayers())
        {
            if (!otherPlayer.isOfferedDraw())
            {
                isAllUsersWantDraw = false;
                break;
            }
        }

        if (isAllUsersWantDraw)
        {
            // Объявляем ничью
            endGame(game, null, PLAYERS_AGREED_TO_DRAW);
        } else
        {
            // Уведомляем других, что один из игроков предложил ничью
            notifyListenersThatGameChanged(game);
        }
    }

    /**
     * Проверяет на завершение партии по времени
     * @throws NoNextPlayerFoundException
     */
    @Override
    public void checkGameForEndByTime(final ChessGame game) throws NoNextPlayerFoundException
    {
        log.trace("checkGameForEndByTime game={}", game);

        if (game.isFinished()) return;

        final ChessPlayer currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        // Время текущего хода
        final long currentMoveTime = countCurrentMoveMilliseconds(game);

        // Оставшееся время текущего игрока
        final long playerLeftTime = currentPlayer.getTimeLeft() - currentMoveTime;

        if (playerLeftTime < 0)
        {
            endGame(game, game.getNextPlayer(currentPlayer), TIME_IS_UP);
        }
    }

    /* Проверяет не закончилась ли игра, либо не истекло ли время.
       В случае ошибки - выбрасывает исключение.
       */
    private void checkThatGameNotFinished(final ChessGame game) throws GameMoveException
    {
        log.trace("checkThatGameNotFinished game={}", game);
        try
        {
            // Проверяем не истекло ли время для текущей партии
            checkGameForEndByTime(game);

            // Проверяем не закончилась ли игра
            if (game.isFinished())
            {
                throw new GameMoveException(GAME_ALREADY_FINISHED);
            }
        } catch (final NoNextPlayerFoundException ex)
        {
            log.error("Exception: game={}", game, ex);
            throw new GameMoveException(NO_NEXT_PLAYER_FOUND_EXCEPTION);
        }
    }

    // Проверяет ходит ли сейчас этот игрок
    private void checkThatPlayerIsCurrent(final ChessPlayer player, final ChessGame game) throws GameMoveException
    {
        log.trace("checkThatPlayerIsCurrent player={}, game={}", player, game);
        final ChessPlayer currentPlayer = game.getCurrentPlayer();

        if (!player.equals(currentPlayer))
        {
            throw new GameMoveException(String.format(
                    MOVE_SEQUENCE_EXCEPTION,
                    currentPlayer == null ? "" : currentPlayer.getName()));
        }
    }

    /* Если ход передался новому игроку:
    - проверяем нет ли мата;
    - уменьшаем время хода
    */
    private void processIfCurrentPlayerChanged(final ChessGame game,
                                               final ChessPlayer prevPlayer, final long currentMoveTime)
            throws NoKingOnFieldException, NoNextPlayerFoundException
    {
        if (!prevPlayer.equals(game.getCurrentPlayer()))
        {
            // Проверяем нет ли мата/пата
            checkGameForEndByPosition(game);

            // Уменьшаем оставшееся время у игрока, сделавшего ход
            prevPlayer.setTimeLeft(prevPlayer.getTimeLeft() - currentMoveTime);
        }
    }

    // Проверяет игру на окончание партии по структуре доски
    private void checkGameForEndByPosition(final ChessGame game)
            throws NoKingOnFieldException, NoNextPlayerFoundException
    {
        log.trace("checkGameForEndByPosition game={}", game);
        final ChessPlayer currentPlayer = game.getCurrentPlayer();
        final ChessField field = game.getField();

        /* Если не осталось ни одного допустимого хода для текущего игрока,
        то это либо мат (если сейчас шах), либо пат (если шаха нет).
        */
        if (chessRules.isNoMoreMoves(currentPlayer.getColor(), field))
        {
            if (chessRules.isCheckForPlayer(currentPlayer.getColor(), field))
            {
                endGame(game, game.getNextPlayer(currentPlayer), MATE);     // мат
            } else
            {
                endGame(game, null, STALEMATE);     // пат
            }
        }
    }

    // Завершает партию
    private void endGame(final ChessGame game, final ChessPlayer winner, final String reasonResult)
    {
        log.debug("endGame game={}, winner={}, reasonResult={}", game, winner, reasonResult);

        try
        {
            game.end(winner, reasonResult);

            notifyListenersThatGameEnded(game, winner);
        } catch (final NoNextPlayerFoundException ex)
        {
            log.error("Exception: game={}, winner={}, reasonResult={}", game, winner, reasonResult, ex);
        }
    }

    private long countCurrentMoveMilliseconds(final ChessGame game)
    {
        return Calendar.getInstance().getTime().getTime() - game.getCurrentMoveStarted().getTime();
    }

    private void checkChooseFigureInsteadPawn(ChessFigure.Type chosenFigureType) throws GameMoveException {
        if (chosenFigureType == null || chosenFigureType == ChessFigure.Type.King || chosenFigureType == ChessFigure.Type.Pawn)
        {
            throw new GameMoveException(UNKNOWN_FIGURE_TYPE_EXCEPTION);
        }
    }
}