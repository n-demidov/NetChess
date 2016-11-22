package edu.demidov.netchess.game.rules.impl;

import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidPointException;
import edu.demidov.netchess.common.model.game.chess.ChessColor;
import edu.demidov.netchess.common.model.game.chess.ChessField;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.game.exceptions.NoKingOnFieldException;
import edu.demidov.netchess.utils.Point;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

public class ChessRulesImplTest {

    private static final int FIELD_SIZE = 8;
    private static final int WHITE_KING_X = 0, WHITE_KING_Y = 0;

    private ChessRulesImpl chessRules = new ChessRulesImpl();
    private final ChessColor white = ChessColor.White;
    private final ChessColor black = ChessColor.Black;
    private ChessField field;
    private ChessFigure[][] figures;
    private ChessFigure whiteKing;

    @Before
    public void before() throws Exception {
        figures = new ChessFigure[FIELD_SIZE][FIELD_SIZE];

        field = mock(ChessField.class);
        when(field.getFieldSize()).thenReturn(FIELD_SIZE);
        when(field.getField()).thenReturn(figures);

        when(field.getFigure(eq(FIELD_SIZE), anyInt())).thenThrow(new InvalidPointException());
        when(field.getFigure(anyInt(), eq(FIELD_SIZE))).thenThrow(new InvalidPointException());
        when(field.getFigure(eq(-1), anyInt())).thenThrow(new InvalidPointException());
        when(field.getFigure(anyInt(), eq(-1))).thenThrow(new InvalidPointException());


        whiteKing = mock(ChessFigure.class);
        when(whiteKing.getType()).thenReturn(ChessFigure.Type.King);
        when(whiteKing.getColor()).thenReturn(ChessColor.White);

        when(field.getFigure(WHITE_KING_X, WHITE_KING_Y)).thenReturn(whiteKing);

        Point whiteKingPosition = new Point(0, 0);

        when(field.getFigure(whiteKingPosition)).thenReturn(whiteKing);
    }

    @Test(expected = NoKingOnFieldException.class)
    public void testIsCheckForPlayer_WhenNoKing() throws Exception {
        field = mock(ChessField.class);

        chessRules.isCheckForPlayer(white, field);

        fail();
    }

    @Test
    public void testIsCheckForPlayer_NoCheck() throws Exception {
        final boolean actual = chessRules.isCheckForPlayer(white, field);

        assertFalse(actual);
    }

    @Test
    public void testIsCheckForPlayer_WhenCheck() throws Exception {
        final int BLACK_CASTLE_X = 1, BLACK_CASTLE_Y = 0;
        final ChessFigure blackCastle = mock(ChessFigure.class);
        when(blackCastle.getType()).thenReturn(ChessFigure.Type.Castle);
        when(blackCastle.getColor()).thenReturn(ChessColor.Black);

        when(field.getFigure(BLACK_CASTLE_X, BLACK_CASTLE_Y)).thenReturn(blackCastle);

        final boolean actual = chessRules.isCheckForPlayer(white, field);

        assertTrue(actual);
    }

}