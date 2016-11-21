package edu.demidov.netchess.game.api;

import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;

public interface ChessLogicObservable {

    void addListener(ChessLogicObserver listener);

    void removeListener(ChessLogicObserver listener);

    void notifyListenersThatGameChanged(ChessGame game);

    void notifyListenersThatGameEnded(ChessGame game, ChessPlayer winner);

}
