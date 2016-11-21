package edu.demidov.netchess.game.rules;

import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.game.chess.ChessColor;
import edu.demidov.netchess.common.model.game.chess.ChessField;
import edu.demidov.netchess.game.exceptions.NoKingOnFieldException;
import edu.demidov.netchess.utils.Point;

public interface ChessRules {

    boolean isMoveCorrect(ChessColor color, ChessField field, Point fromPoint, Point toPoint)
            throws GameMoveException, NoKingOnFieldException;

    boolean isCheckForPlayer(ChessColor color, ChessField field) throws NoKingOnFieldException;

    boolean isNoMoreMoves(ChessColor color, ChessField field) throws NoKingOnFieldException;

}
