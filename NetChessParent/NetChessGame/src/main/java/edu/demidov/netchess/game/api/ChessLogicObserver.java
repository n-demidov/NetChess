package edu.demidov.netchess.game.api;

import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;

public interface ChessLogicObserver
{
    void gameChanged(ChessGame game);

    void gameEnded(ChessGame game, ChessPlayer winner);
}
