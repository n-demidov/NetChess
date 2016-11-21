package edu.demidov.netchess.common.model.game.chess;

import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidBoardSizeException;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidPointException;
import edu.demidov.netchess.utils.Point;
import org.junit.Test;

import java.util.Random;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ChessFieldTest {

    private static final int FIELD_SIZE = 8;

    @Test
    public void testGetFieldSize() throws Exception {
        final ChessField field = new ChessField(FIELD_SIZE);

        assertEquals(String.format("The test case failed of a FIELD_SIZE = %s", FIELD_SIZE),
                FIELD_SIZE, field.getFieldSize());
    }

    @Test
    public void testSetFigure() throws Exception {
        final ChessField field = new ChessField(FIELD_SIZE);
        final Point inputPoint = new Point(0, 0);
        final ChessFigure inputFigure = new ChessFigure(ChessColor.White, ChessFigure.Type.Pawn);

        field.setFigure(inputPoint, inputFigure);
        final ChessFigure actualFigure = field.getFigure(inputPoint);

        assertEquals(inputFigure, actualFigure);
    }

    @Test
    public void testGetFigureWhenFigureIsNotSet() throws Exception {
        final ChessField field = new ChessField(FIELD_SIZE);
        final Point inputPoint = new Point(0, 0);

        final ChessFigure actualFigure = field.getFigure(inputPoint);

        assertNull(actualFigure);
    }

    @Test
    public void testGetFigureWhenXIsLessThenZero() throws Exception {
        final ChessField field = new ChessField(FIELD_SIZE);
        final Point inputPoint = new Point(-1, 0);

        try {
            field.getFigure(inputPoint);
            fail();
        } catch (final InvalidPointException e) {
        }
    }

    @Test
    public void testGetFigureWhenYIsLessThenZero() throws Exception {
        final ChessField field = new ChessField(FIELD_SIZE);
        final Point inputPoint = new Point(0, -1);

        try {
            field.getFigure(inputPoint);
            fail();
        } catch (final InvalidPointException e) {
        }
    }

    @Test
    public void testGetFigureWhenXIsMoreThenSize() throws Exception {
        final ChessField field = new ChessField(FIELD_SIZE);
        final Point inputPoint = new Point(field.getFieldSize() + 1, 0);

        try {
            field.getFigure(inputPoint);
            fail();
        } catch (final InvalidPointException e) {
        }
    }

    @Test
    public void testGetFigureWhenYIsMoreThenSize() throws Exception {
        final ChessField field = new ChessField(FIELD_SIZE);
        final Point inputPoint = new Point(0, field.getFieldSize() + 1);

        try {
            field.getFigure(inputPoint);
            fail();
        } catch (final InvalidPointException e) {
        }
    }

    @Test(expected = InvalidBoardSizeException.class)
    public void testCustomFieldError() throws Exception {
        final Random randomGenerator = new Random();
        for (int i = 100; i > 0; i--) {
            final int fieldSize = randomGenerator.nextInt(50) - 50;
            final ChessField field = new ChessField(fieldSize);
        }
    }

    /**
     * Проверяет копирование объекта: для поля field - глубокое копирование, для фигур на доске - неважно (можно любое)
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testChessField_1arg_OtherChessField() throws Exception {
        final Point inputPoint = new Point(0, 3);

        final ChessField field = new ChessField(FIELD_SIZE);
        final ChessFigure inputFigure = new ChessFigure(ChessColor.White, ChessFigure.Type.Pawn);
        field.setFigure(inputPoint, inputFigure);

        // Проверяем, что объекты field у обоих досок ссылаются на разные объекты, но одинаковы
        final ChessField clonedField = new ChessField(field);

        if (clonedField == field) fail("Ссылки на доски должны быть разными");
        assertEquals("Доски должны быть одинаковыми по equels()", clonedField, field);
        assertEquals("На 1 доске фигура должна была остаться без изменений", inputFigure, field.getFigure(inputPoint));
        assertEquals("На 2 доске фигура должна была остаться без изменений", inputFigure, clonedField.getFigure(inputPoint));
        assertNull("В этой клетке должно быть null", clonedField.getFigure(new Point(2, 2)));

        // Удалим фигуру на клонированной доске. Первая доска должна остаться без изменений
        final ChessFigure newFigure = null;
        clonedField.setFigure(inputPoint, newFigure);

        assertThat("Доски должны быть разными по equels()", clonedField, not(equalTo(field)));
        assertEquals("На первой доске фигура должна была остаться без изменений", inputFigure, field.getFigure(inputPoint));
        assertEquals("На второй доске фигура должна стать newFigure", newFigure, clonedField.getFigure(inputPoint));
    }

}
