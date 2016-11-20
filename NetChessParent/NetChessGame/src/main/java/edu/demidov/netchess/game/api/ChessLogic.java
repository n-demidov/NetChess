package edu.demidov.netchess.game.api;

import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.exceptions.game.chess.NoNextPlayerFoundException;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import edu.demidov.netchess.utils.Point;

import java.util.List;

public interface ChessLogic extends ChessLogicObservable
{
    ChessGame startGame(final List<ChessPlayer> chessPlayers);

    void playerMoveFigure(final ChessPlayer player, final ChessGame game,
                          final Point fromPoint, final Point toPoint) throws GameMoveException;

    void playerChooseFigureInsteadPawn(final ChessPlayer player,
                                       final ChessGame game, final ChessFigure.Type chosenFigureType) throws GameMoveException;

    void playerSurrender(final ChessPlayer player, final ChessGame game) throws GameMoveException;

    void playerOfferedDraw(final ChessPlayer offeredDrawPlayer, final ChessGame game);

    void checkGameForEndByTime(final ChessGame game) throws NoNextPlayerFoundException;
}
