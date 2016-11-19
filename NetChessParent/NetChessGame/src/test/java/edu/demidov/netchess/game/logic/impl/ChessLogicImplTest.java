package edu.demidov.netchess.game.logic.impl;

import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ChessLogicImplTest
{
    private final ChessLogicImpl chessLogic = ChessLogicImpl.getInstance();

    @Test
    public void testPlayerChooseFigureInsteadPawn_WhenGoodFigures() throws Exception
    {
        final List<ChessFigure.Type> figures = new ArrayList<>(Arrays.asList(
                ChessFigure.Type.Queen, ChessFigure.Type.Castle, ChessFigure.Type.Bishop, ChessFigure.Type.Knight));

        for (final ChessFigure.Type figure : figures)
        {
            final ChessGame game = getNotExpiredMockedGame();
            final ChessPlayer currentPlayer = mock(ChessPlayer.class);

            when(game.getCurrentPlayer()).thenReturn(currentPlayer);
            when(game.isCurrentPlayerChoosingFigure()).thenReturn(true);

            chessLogic.playerChooseFigureInsteadPawn(currentPlayer, game, figure);

            verify(game, times(1)).transformPawn(currentPlayer, figure);
        }
    }

    @Test
    public void testPlayerChooseFigureInsteadPawn_WhenWrongFigures()
    {
        final List<ChessFigure.Type> figures = new ArrayList<>(Arrays.asList(
                null, ChessFigure.Type.Pawn, ChessFigure.Type.King));

        for (final ChessFigure.Type figure : figures)
        {
            try {
                final ChessGame game = getNotExpiredMockedGame();

                when(game.isCurrentPlayerChoosingFigure()).thenReturn(true);

                chessLogic.playerChooseFigureInsteadPawn(game.getCurrentPlayer(), game, figure);

                fail();
            } catch (final GameMoveException e) {}
        }
    }

    @Test
    public void testPlayerSurrender() throws Exception
    {
        final ChessGame game = mock(ChessGame.class);

        chessLogic.playerSurrender(null, game);

        verify(game, times(1)).end(any(ChessPlayer.class), any(String.class));
    }

    @Test
    public void testCheckGameForEndByTime_WhenTimeoutNotExpired() throws Exception
    {
        final ChessGame game = getNotExpiredMockedGame();

        chessLogic.checkGameForEndByTime(game);

        verify(game, never()).end(any(ChessPlayer.class), any(String.class));
    }

    @Test
    public void testCheckGameForEndByTime_WhenTimeoutExpired() throws Exception
    {
        final ChessGame game = getExpiredMockedGame();

        chessLogic.checkGameForEndByTime(game);

        verify(game, times(1)).end(any(ChessPlayer.class), any(String.class));
    }

    private ChessGame getNotExpiredMockedGame()
    {
        final int MOVE_TIME_STARTED_AGO = 100;
        final long MOVE_TIME_LEFT = MOVE_TIME_STARTED_AGO * 2;

        return getMockedGameForTimoutTest(MOVE_TIME_STARTED_AGO, MOVE_TIME_LEFT);
    }

    private ChessGame getExpiredMockedGame()
    {
        final int MOVE_TIME_STARTED_AGO = 100;
        final long MOVE_TIME_LEFT = MOVE_TIME_STARTED_AGO / 2;

        return getMockedGameForTimoutTest(MOVE_TIME_STARTED_AGO, MOVE_TIME_LEFT);
    }

    private ChessGame getMockedGameForTimoutTest(int MOVE_TIME_STARTED_AGO, long MOVE_TIME_LEFT)
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MILLISECOND, -MOVE_TIME_STARTED_AGO);
        Date moveStarted = cal.getTime();

        final ChessPlayer currentPlayer = mock(ChessPlayer.class);
        when(currentPlayer.getTimeLeft()).thenReturn(MOVE_TIME_LEFT);

        final ChessGame game = mock(ChessGame.class);
        when(game.getCurrentPlayer()).thenReturn(currentPlayer);
        when(game.getCurrentMoveStarted()).thenReturn(moveStarted);
        return game;
    }
}