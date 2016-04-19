package edu.demidov.netchess.server.model.game;

import edu.demidov.netchess.common.model.game.chess.ChessGame;

public interface GameChangedObserver
{
    
    void gameChanged(final ChessGame game);
    
}
