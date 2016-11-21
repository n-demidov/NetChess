package edu.demidov.netchess.common.model.game.chess;

import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidBoardSizeException;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidPointException;
import edu.demidov.netchess.utils.Point;

import java.io.Serializable;
import java.util.Arrays;

public class ChessField implements Serializable {

    private static final int MIN_COORDINATE = 0;
    private static final int MIN_SIZE = 8;

    private ChessFigure[][] field;
    private int fieldSize;

    public ChessField() {
    }

    public ChessField(final int fieldSize) throws InvalidBoardSizeException {
        if (fieldSize < MIN_SIZE) {
            throw new InvalidBoardSizeException();
        } else {
            this.fieldSize = fieldSize;
            field = new ChessFigure[fieldSize][fieldSize];
        }
    }

    /**
     * Делает копирование
     * Создаёт новый объект поля (field); а ссылки на фигуры будут общими.
     *
     * @param other
     * @throws InvalidBoardSizeException
     */
    public ChessField(final ChessField other) throws InvalidBoardSizeException {
        this(other.getFieldSize());

        // Копируем все ссылки на фигуры
        for (int i = MIN_COORDINATE; i < other.getFieldSize(); i++) {
            System.arraycopy(
                    other.getField()[i], MIN_COORDINATE,
                    this.field[i], MIN_COORDINATE,
                    other.getField()[i].length - MIN_COORDINATE);
        }
    }

    public ChessFigure[][] getField() {
        return field;
    }

    public void setField(final ChessFigure[][] field) {
        this.field = field;
    }

    public int getFieldSize() {
        return fieldSize;
    }

    public void setFieldSize(final int fieldSize) {
        this.fieldSize = fieldSize;
    }

    public ChessFigure getFigure(final Point point) throws InvalidPointException {
        if (!checkPoint(point)) throw new InvalidPointException();
        return field[point.getX()][point.getY()];
    }

    public void setFigure(final Point point, final ChessFigure figure)
            throws InvalidPointException {
        if (!checkPoint(point)) throw new InvalidPointException();
        field[point.getX()][point.getY()] = figure;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Arrays.deepHashCode(this.field);
        hash = 37 * hash + this.fieldSize;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChessField other = (ChessField) obj;
        if (this.fieldSize != other.fieldSize) {
            return false;
        }
        if (!Arrays.deepEquals(this.field, other.field)) {
            return false;
        }
        return true;
    }

    private boolean checkPoint(final Point point) {
        return checkCoordinate(point.getX(), field.length) && checkCoordinate(point.getY(), field[point.getX()].length);
    }

    private boolean checkCoordinate(final int coordinate, final int maxCoordinate) {
        return coordinate >= MIN_COORDINATE && coordinate < maxCoordinate;
    }

}
