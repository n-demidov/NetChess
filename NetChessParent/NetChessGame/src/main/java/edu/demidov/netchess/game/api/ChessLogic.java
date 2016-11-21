package edu.demidov.netchess.game.api;

import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.exceptions.game.chess.NoNextPlayerFoundException;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import edu.demidov.netchess.game.rules.ChessRules;
import edu.demidov.netchess.utils.Point;

import java.util.List;

public interface ChessLogic extends ChessLogicObservable
{
    ChessGame startGame(List<ChessPlayer> chessPlayers);

    void playerMoveFigure(ChessPlayer player, ChessGame game, Point fromPoint, Point toPoint)
            throws GameMoveException;

    void playerTransformPawn(ChessPlayer player, ChessGame game, ChessFigure.Type chosenFigureType)
            throws GameMoveException;

    void playerSurrender(ChessPlayer player, ChessGame game) throws GameMoveException;

    void playerOfferedDraw(ChessPlayer offeredDrawPlayer, ChessGame game);

    void checkGameForEndByTime(ChessGame game) throws NoNextPlayerFoundException;

    void setChessRules(ChessRules chessRules);
}
