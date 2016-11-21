package edu.demidov.netchess.game.impl;

import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.exceptions.game.chess.NoNextPlayerFoundException;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import edu.demidov.netchess.game.api.ChessLogicObserver;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChessLogicImplTest {
    private final ChessLogicImpl chessLogic = ChessLogicImpl.getInstance();
    private ChessLogicObserver chessLogicObserver;

    private ChessGame game;
    private ChessPlayer currentPlayer;
    private ChessPlayer nextPlayer;

    @Before
    public void before() throws Exception {
        chessLogic.removeListener(chessLogicObserver);
        chessLogicObserver = mock(ChessLogicObserver.class);
        chessLogic.addListener(chessLogicObserver);

        nextPlayer = mock(ChessPlayer.class);
        currentPlayer = mock(ChessPlayer.class);
        game = mock(ChessGame.class);

        when(game.getCurrentPlayer()).thenReturn(currentPlayer);
        when(game.getNextPlayer(currentPlayer)).thenReturn(nextPlayer);
        when(game.getNextPlayer(nextPlayer)).thenReturn(currentPlayer);

        doGameNotExpired(game);

        assertEquals(game.getNextPlayer(currentPlayer), nextPlayer);
        assertEquals(game.getNextPlayer(nextPlayer), currentPlayer);
    }


    @Test
    public void testPlayerTransformPawn_WhenGoodFigures() throws Exception {
        final List<ChessFigure.Type> figures = new ArrayList<>(Arrays.asList(
                ChessFigure.Type.Queen, ChessFigure.Type.Castle, ChessFigure.Type.Bishop, ChessFigure.Type.Knight));

        for (final ChessFigure.Type figure : figures) {
            before();
            when(game.isCurrentPlayerChoosingFigure()).thenReturn(true);

            chessLogic.playerTransformPawn(currentPlayer, game, figure);

            verify(game, times(1)).transformPawn(currentPlayer, figure);
            verify(chessLogicObserver, never()).gameEnded(eq(game), eq(nextPlayer));
            verify(chessLogicObserver, times(1)).gameChanged(any(ChessGame.class));
        }
    }

    @Test
    public void testPlayerTransformPawn_WhenWrongFigures() throws Exception {
        final List<ChessFigure.Type> figures = new ArrayList<>(Arrays.asList(
                ChessFigure.Type.Pawn, ChessFigure.Type.King, null));

        for (final ChessFigure.Type figure : figures) {
            before();
            when(game.isCurrentPlayerChoosingFigure()).thenReturn(true);

            try {
                chessLogic.playerTransformPawn(game.getCurrentPlayer(), game, figure);
                fail();
            } catch (final GameMoveException e) {
            }

            verify(game, never()).transformPawn(any(ChessPlayer.class), any(ChessFigure.Type.class));
            verify(chessLogicObserver, never()).gameEnded(any(ChessGame.class), any(ChessPlayer.class));
            verify(chessLogicObserver, never()).gameChanged(any(ChessGame.class));
        }
    }

    @Test
    public void testPlayerTransformPawn_WhenGameEnded() throws Exception {
        final ChessFigure.Type figure = ChessFigure.Type.Queen;

        when(game.isFinished()).thenReturn(true);
        when(game.isCurrentPlayerChoosingFigure()).thenReturn(true);

        chessLogic.playerTransformPawn(currentPlayer, game, figure);

        verifyThatNoChanges();
    }

    @Test
    public void testCheckGameForEndByTime_WhenTimeoutNotExpired() throws Exception {
        chessLogic.checkGameForEndByTime(game);

        verifyThatNoChanges();
    }

    @Test
    public void testCheckGameForEndByTime_WhenTimeoutExpired() throws Exception {
        doGameExpired(game);

        chessLogic.checkGameForEndByTime(game);

        verify(game, times(1)).end(any(ChessPlayer.class), any(String.class));
        verify(chessLogicObserver, times(1)).gameEnded(eq(game), eq(nextPlayer));
        verify(chessLogicObserver, never()).gameChanged(any(ChessGame.class));
    }

    @Test
    public void testCheckGameForEndByTime_WhenGameEnded() throws Exception {
        doGameExpired(game);
        when(game.isFinished()).thenReturn(true);

        chessLogic.checkGameForEndByTime(game);

        verifyThatNoChanges();
    }

    @Test
    public void testPlayerSurrender_WhenCurrentPlayerSurrender() throws Exception {
        final ChessPlayer winner = nextPlayer;

        chessLogic.playerSurrender(currentPlayer, game);

        verifyWhenPlayerSurrender(winner);
    }

    @Test
    public void testPlayerSurrender_WhenNextPlayerSurrender() throws Exception {
        final ChessPlayer winner = currentPlayer;

        chessLogic.playerSurrender(nextPlayer, game);

        verifyWhenPlayerSurrender(winner);
    }

    @Test
    public void testPlayerSurrender_WhenGameEnded() throws Exception {
        when(game.isFinished()).thenReturn(true);

        chessLogic.playerSurrender(currentPlayer, game);

        verifyThatNoChanges();
    }

    private void verifyWhenPlayerSurrender(final ChessPlayer winner) throws NoNextPlayerFoundException {
        verify(game, times(1)).end(eq(winner), any(String.class));
        verify(chessLogicObserver, times(1)).gameEnded(eq(game), eq(winner));
        verify(chessLogicObserver, never()).gameChanged(any(ChessGame.class));
    }

    private void doGameNotExpired(final ChessGame game) {
        final int MOVE_TIME_STARTED_AGO = 100;
        final long MOVE_TIME_LEFT = MOVE_TIME_STARTED_AGO * 2;

        setCurrentPlayerTimeout(game, MOVE_TIME_STARTED_AGO, MOVE_TIME_LEFT);
    }

    private void doGameExpired(final ChessGame game) {
        final int MOVE_TIME_STARTED_AGO = 100;
        final long MOVE_TIME_LEFT = MOVE_TIME_STARTED_AGO / 2;

        setCurrentPlayerTimeout(game, MOVE_TIME_STARTED_AGO, MOVE_TIME_LEFT);
    }

    private void setCurrentPlayerTimeout(final ChessGame game, final int moveTimeStartedAgo, final long moveTimeLeft) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MILLISECOND, -moveTimeStartedAgo);
        Date moveStarted = cal.getTime();

        when(currentPlayer.getTimeLeft()).thenReturn(moveTimeLeft);
        when(game.getCurrentMoveStarted()).thenReturn(moveStarted);
    }

    private void verifyThatNoChanges() throws NoNextPlayerFoundException {
        verify(game, never()).end(any(ChessPlayer.class), any(String.class));
        verify(chessLogicObserver, never()).gameEnded(any(ChessGame.class), any(ChessPlayer.class));
        verify(chessLogicObserver, never()).gameChanged(any(ChessGame.class));
    }
}