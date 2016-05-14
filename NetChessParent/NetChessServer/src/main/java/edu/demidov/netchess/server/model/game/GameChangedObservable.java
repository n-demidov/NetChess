package edu.demidov.netchess.server.model.game;

import edu.demidov.netchess.common.model.game.chess.ChessGame;

public interface GameChangedObservable
{
    
    void addListener(GameChangedObserver listener);
    
    void removeListener(GameChangedObserver listener);
    
    void notifyListenersThatGameChanged(ChessGame game);
    
}
