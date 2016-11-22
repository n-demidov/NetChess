package edu.demidov.netchess.game.rules.impl;

import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidBoardSizeException;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidPointException;
import edu.demidov.netchess.common.model.game.chess.ChessColor;
import edu.demidov.netchess.common.model.game.chess.ChessField;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.game.exceptions.NoKingOnFieldException;
import edu.demidov.netchess.game.rules.ChessRules;
import edu.demidov.netchess.utils.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Класс отвечает за проверку шахматных правил
 */
public class ChessRulesImpl implements ChessRules {

    private static final Point[] DELTA_KNIGHT_MOVES;                        // Смещения возможных ходов коня
    private static final Point[] FORWARD_VECTOR_ATTACK_OFFSETS;             // Смещения для прямых линий
    private static final Point[] DIAGONAL_VECTOR_ATTACK_OFFSETS;            // Смещения для диагональных линий
    private static final int WHITE_PAWN_LINE = 6, BLACK_PAWN_LINE = 1;      // С каких полей пешка может пройти вперёд на 2 клетки

    private static final String ILLEGAL_MOVE_EXCEPTION = "Неверный ход";
    private static final String SAME_CELL_EXCEPTION = "Нельзя ходить в ту же клетку";
    private static final String NOT_YOUR_FIGURE_EXCEPTION = "Вы ходите не своей фигурой";
    private static final String COORDINATES_EXCEPTION = "Ошибка в координатах";
    private static final String CHECK_WARNING_EXCEPTION = "Ход невозможен, так как приведет к шаху";

    private static final Logger log = LoggerFactory.getLogger(ChessRulesImpl.class);

    static {
        DELTA_KNIGHT_MOVES = new Point[8];
        DELTA_KNIGHT_MOVES[0] = new Point(1, -2);
        DELTA_KNIGHT_MOVES[1] = new Point(2, -1);
        DELTA_KNIGHT_MOVES[2] = new Point(1, 2);
        DELTA_KNIGHT_MOVES[3] = new Point(2, 1);

        DELTA_KNIGHT_MOVES[4] = new Point(-1, 2);
        DELTA_KNIGHT_MOVES[5] = new Point(-2, 1);
        DELTA_KNIGHT_MOVES[6] = new Point(-2, -1);
        DELTA_KNIGHT_MOVES[7] = new Point(-1, -2);

        FORWARD_VECTOR_ATTACK_OFFSETS = new Point[4];
        FORWARD_VECTOR_ATTACK_OFFSETS[0] = new Point(0, -1);
        FORWARD_VECTOR_ATTACK_OFFSETS[1] = new Point(0, 1);
        FORWARD_VECTOR_ATTACK_OFFSETS[2] = new Point(-1, 0);
        FORWARD_VECTOR_ATTACK_OFFSETS[3] = new Point(1, 0);

        DIAGONAL_VECTOR_ATTACK_OFFSETS = new Point[4];
        DIAGONAL_VECTOR_ATTACK_OFFSETS[0] = new Point(1, 1);
        DIAGONAL_VECTOR_ATTACK_OFFSETS[1] = new Point(1, -1);
        DIAGONAL_VECTOR_ATTACK_OFFSETS[2] = new Point(-1, 1);
        DIAGONAL_VECTOR_ATTACK_OFFSETS[3] = new Point(-1, -1);
    }

    @Override
    public boolean isMoveCorrect(final ChessColor color, final ChessField field,
                                 final Point fromPoint, final Point toPoint) throws GameMoveException, NoKingOnFieldException {
        log.debug("isMoveCorrect color={}, field={}, fromPoint={}, toPoint={}", color, field, fromPoint, toPoint);
        try {
            final ChessFigure figure = field.getFigure(fromPoint);

            // Проверяем своей ли фигурой ходит игрок
            if (figure == null || !figure.getColor().equals(color)) {
                log.trace(NOT_YOUR_FIGURE_EXCEPTION);
                throw new GameMoveException(NOT_YOUR_FIGURE_EXCEPTION);
            }

            // Нельзя ходить в ту же клетку
            if (fromPoint.equals(toPoint)) {
                log.trace(SAME_CELL_EXCEPTION);
                throw new GameMoveException(SAME_CELL_EXCEPTION);
            }
            
            /* Проверяем корректность хода для конкретной фигуры
            Если ход не совпадает ни с одним из возможных ходов, то выбрасываем ошибку
            */
            boolean isCorrectMove = false;
            for (final Point moveCell : getFigureMoveCells(figure, fromPoint, field)) {
                if (toPoint.equals(moveCell)) {
                    isCorrectMove = true;
                    break;
                }
            }
            if (!isCorrectMove) {
                log.trace("can't find cell in the list of possible cells, toPoint={}", toPoint);
                throw new GameMoveException(ILLEGAL_MOVE_EXCEPTION);
            }
            
            /* Передвигаем фигуру на тестовой доске и смотрим - не приведёт ли ход к шаху.
            Если игрок своим ходом подставляется под шах, то выбрасываем исключение
            */
            final ChessField clonedField = new ChessField(field);
            clonedField.setFigure(fromPoint, null);
            clonedField.setFigure(toPoint, figure);
            if (isCheckForPlayer(color, clonedField)) {
                log.trace(CHECK_WARNING_EXCEPTION);
                throw new GameMoveException(CHECK_WARNING_EXCEPTION);
            }

            return true;
        } catch (final InvalidPointException ex) {
            log.trace(COORDINATES_EXCEPTION);
            throw new GameMoveException(COORDINATES_EXCEPTION);
        } catch (final InvalidBoardSizeException ex) {
            log.error("Exception:", ex);
        }

        return false;
    }

    /**
     * Проверяет поставлен ли игроку шах
     *
     * @param color
     * @param field
     * @return
     * @throws NoKingOnFieldException
     */
    @Override
    public boolean isCheckForPlayer(final ChessColor color, final ChessField field) throws NoKingOnFieldException {
        log.trace("isCheckForPlayer color={}, field={}", color, field);

        final Point playerKingCell = findKingPosition(color, field);
        return isCellUnderAttack(playerKingCell, color, field);
    }

    /**
     * Проверяет остался ли у игрока хотя бы один допустимый ход.
     * Проверяется для игрока указанного цвета.
     *
     * @param color
     * @param field ception
     * @return
     * @throws NoKingOnFieldException
     */
    @Override
    public boolean isNoMoreMoves(final ChessColor color, final ChessField field) throws NoKingOnFieldException {
        log.debug("isNoMoreMoves color={}, field={}", color, field);
        // Для каждой фигуры
        for (int x = 0; x < field.getFieldSize(); x++)
            for (int y = 0; y < field.getField()[x].length; y++) {
                try {
                    final Point figurePos = new Point(x, y);
                    final ChessFigure figure = field.getFigure(figurePos);
                    // Фигура должна быть указанного цвета
                    if (figure == null || figure.getColor() != color) continue;

                    // Для всех возможных ходов этой фигуры
                    final Collection<Point> moves = getFigureMoveCells(figure, figurePos, field);
                    for (final Point move : moves) {
                        // Если ход валидный - сразу возвращаем результат
                        try {
                            if (isMoveCorrect(color, field, figurePos, move))
                                return false;
                        } catch (final GameMoveException ex) {
                        }
                    }
                } catch (final InvalidPointException ex) {
                    log.error("Critical error while iterate in 2 cycles, color={}, field={}, x={}, y={}", color, field, x, y, ex);
                }
            }

        return true;
    }

    /**
     * Возвращает коллекцию предполагаемо возможных ходов фигуры
     *
     * @param figure
     * @param figurePos
     * @param field
     * @return
     */
    private Collection<Point> getFigureMoveCells(final ChessFigure figure,
                                                 final Point figurePos, final ChessField field) {
        log.trace("getFigureMoveCells figure={}, figurePos={}, field={}", figure, figurePos, field);

        switch (figure.getType()) {
            case Castle:
            case Bishop:
            case Queen:
            case Knight:
                return getFigureMoveOrAttackCells(figure, figurePos, field);
            case King:
                return findKingMoveCells(figure, figurePos, field);
            case Pawn:
                return findPawnMoveCells(figure, figurePos, field);
            default:
                log.error("FATAL IllegalArgumentException");
                throw new IllegalArgumentException();
        }
    }

    // Проверяет находится ли указанная клетка (на шахматном поле) под атакой одной из фигур оппонента
    private boolean isCellUnderAttack(final Point pointUnderAttack, final ChessColor color,
                                      final ChessField field) {
        log.trace("isCellUnderAttack pointUnderAttack={}, color={}, field={}", pointUnderAttack, color, field);

        try {
            for (int i = 0; i < field.getFieldSize(); i++) {
                for (int j = 0; j < field.getField()[i].length; j++) {
                    final Point opponentCell = new Point(i, j);
                    final ChessFigure opponentFigure = field.getFigure(opponentCell);

                    // Если это фигура оппонента
                    if (opponentFigure != null &&
                            opponentFigure.getColor() != color) {
                        final Collection<Point> attackedCells
                                = getFigureAttackCells(opponentFigure, field, opponentCell);

                        // Проверяем не находится ли указанная клетка в одной из клеток, которые атакует вражеская фигура
                        for (final Point attackedCell : attackedCells) {
                            if (pointUnderAttack.equals(attackedCell)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (final InvalidPointException ex) {
            log.error("Critical error while iterate in 2 cycles, color={}, field={}", color, field, ex);
        }

        return false;
    }

    // Находит короля на доске
    private Point findKingPosition(final ChessColor color, final ChessField field) throws NoKingOnFieldException {
        log.trace("findKingPosition color={}, field={}", color, field);

        try {
            for (int i = 0; i < field.getFieldSize(); i++) {
                for (int j = 0; j < field.getField()[i].length; j++) {
                    final Point p = new Point(i, j);
                    final ChessFigure figure = field.getFigure(p);

                    if (figure != null && figure.getColor() == color &&
                            figure.getType() == ChessFigure.Type.King ) {
                        return p;
                    }
                }
            }
        } catch (final InvalidPointException ex) {
            log.error("Critical error while iterate in 2 cycles, color={}, field={}", color, field, ex);
        }

        throw new NoKingOnFieldException();
    }

    // Возвращает клетки, атакованные фигурой
    private Collection<Point> getFigureAttackCells(final ChessFigure figure,
                                                   final ChessField field, final Point figurePos) {
        switch (figure.getType()) {
            case Castle:
            case Bishop:
            case Queen:
            case Knight:
                return getFigureMoveOrAttackCells(figure, figurePos, field);
            case King:
                return findKingAttackCells(figure, figurePos, field);
            case Pawn:
                return findPawnAttackCells(figure, figurePos);
            default:
                log.error("getFigureAttackCells FATAL IllegalArgumentException");
                throw new IllegalArgumentException();
        }
    }

    private Collection<Point> getFigureMoveOrAttackCells(final ChessFigure figure,
                                                         final Point figurePosition, final ChessField field) {
        final Collection<Point> findedCells = new ArrayList<>();

        switch (figure.getType()) {
            case Castle:
                // Вертикали + горизонтали
                for (final Point point : FORWARD_VECTOR_ATTACK_OFFSETS) {
                    // Все полученные атакованные клетки суммируем в 1 коллекцию
                    findedCells.addAll(findVectorCells(figure, field,
                            figurePosition, point.getX(), point.getY()));
                }
                break;
            case Bishop:
                // Диагонали
                for (final Point point : DIAGONAL_VECTOR_ATTACK_OFFSETS) {
                    // Все полученные атакованные клетки суммируем в 1 коллекцию
                    findedCells.addAll(findVectorCells(figure, field,
                            figurePosition, point.getX(), point.getY()));
                }
                break;
            case Queen:
                // Вертикали, горизонтали + диагонали. Все полученные атакованные клетки суммируем в 1 коллекцию
                for (final Point point : FORWARD_VECTOR_ATTACK_OFFSETS) {
                    findedCells.addAll(findVectorCells(figure, field,
                            figurePosition, point.getX(), point.getY()));
                }
                for (final Point point : DIAGONAL_VECTOR_ATTACK_OFFSETS) {
                    findedCells.addAll(findVectorCells(figure, field,
                            figurePosition, point.getX(), point.getY()));
                }
                break;
            case Knight:
                findedCells.addAll(findKnightMoveOrAttackCells(figure, figurePosition, field));
                break;
            case King:
            case Pawn:
            default:
                log.error("getFigureMoveOrAttackCells FATAL IllegalArgumentException");
                throw new IllegalArgumentException();
        }

        return findedCells;
    }

    /* Находит клетки по вектору: вертикальному, горизонтальному или диагональному.
    Используется для нахождения клеток, в которые могут пойти или атаковать ладья, слон и ферзь.
    Клетки атаки и движения у них совпадают.
    */
    private Collection<Point> findVectorCells(final ChessFigure figure,
                                              final ChessField field, final Point figurePosition, final int deltaX, final int deltaY) {
        final Collection<Point> findedCells = new ArrayList<>();
        try {
            // Двигаемся до тех пор, пока не упрёмся в фигуру, или не выйдем за рамки поля
            for (int x = figurePosition.getX() + deltaX, y = figurePosition.getY() + deltaY;
                 x < field.getFieldSize();
                 x += deltaX, y += deltaY) {
                final Point point = new Point(x, y);

                // Сначала загрузим фигуру по этой точке - проверим клетку на корректность
                final ChessFigure otherFigure = field.getFigure(x, y);
                
                /* Если otherFigure своего цвета - то ни бить, ни ходить туда мы не можем
                Если же цвета оппонента - то бить и ходить туда мы можем
                */
                if (otherFigure == null || otherFigure.getColor() != figure.getColor()) {
                    findedCells.add(point);         // Теперь можно добавить найденную клетку в коллекцию
                }

                if (otherFigure != null) break;     // "Упёрлись" в фигуру - выходим
            }
            // Вышли за рамки поля/некорректная точка - заканчиваем итерацию.
        } catch (final InvalidPointException ex) {
        }

        return findedCells;
    }

    // Находит клетки, на которые может ходить король.
    private Collection<Point> findKingMoveCells(final ChessFigure figure,
                                                final Point figurePos, final ChessField field) {
        final Collection<Point> findedCells = new ArrayList<>();

        findedCells.addAll(findKingAttackCells(figure, figurePos, field));
        findedCells.addAll(findKingCastlingMoveCells(figure, figurePos, field));

        return findedCells;
    }

    // Находит клетки, на которые может атаковать король.
    private Collection<Point> findKingAttackCells(final ChessFigure figure,
                                                  final Point figurePosition, final ChessField field) {
        final Collection<Point> findedCells = new ArrayList<>();

        // Возвращаем клетки по периметру
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i == 0 && j == 0) continue;

                // Проверяем клетку на допустимость координат. А также нет ли там фигуры своего цвета.
                final Point newPoint = new Point(figurePosition.getX() + i, figurePosition.getY() + j);
                if (isCellNullOrOpponent(figure, newPoint, field))
                    findedCells.add(newPoint);
            }
        }

        return findedCells;
    }

    // Находит клетки, на которые король может сделать рокировку
    private Collection<Point> findKingCastlingMoveCells(final ChessFigure king,
                                                        final Point kingPos, final ChessField field) {
        final Collection<Point> findedCells = new ArrayList<>();

        try {
            // Находим вертикаль
            final int lineY = king.getColor().equals(ChessColor.White) ? 7 : 0;

            // Если король ещё ниразу не ходил
            if (king.getMovesCount() != 0) return findedCells;

            // Находим ладью
            for (int i = 0; i < field.getFieldSize(); i += 7) {
                final Point castlePoint = new Point(i, lineY);
                final int offsetX = (i == 0) ? -1 : 1;      // Смещение вправо или влево по горизонтали
                final ChessFigure figure = field.getFigure(castlePoint);

                // В клетке должна быть ладья нашего цвета, которая ниразу не ходила...
                if (!(figure != null && figure.getType().equals(ChessFigure.Type.Castle) &&
                        figure.getColor().equals(king.getColor()) && figure.getMovesCount() == 0))
                    continue;

                // Между королём и ладьёй не должно быть препятствий
                if (!isNoFiguresBetweenKingAndCastle(king, kingPos, field, offsetX)) continue;
                
                /* Рокировка временно невозможна, если поле:
                - на котором король стоит,
                - должен пересечь,
                - должен занять,
                атаковано одной или несколькими фигурами партнера
                */
                if (isCellUnderAttack(kingPos, king.getColor(), field)) continue;

                if (isCellUnderAttack(
                        new Point(kingPos.getX() + offsetX, kingPos.getY()),
                        king.getColor(),
                        field))
                    continue;

                if (isCellUnderAttack(
                        new Point(kingPos.getX() + offsetX * 2, kingPos.getY()),
                        king.getColor(),
                        field))
                    continue;

                findedCells.add(new Point(kingPos.getX() + offsetX * 2, lineY));
            }
        } catch (final InvalidPointException ex) {
            log.error("findKingCastlingMoveCells FATAL error while iterate field, king={}, kingPos={}, field={}", king, kingPos, field, ex);
        }

        return findedCells;
    }

    // Возвращает true, если между королём и ладьёй нет фигур
    private boolean isNoFiguresBetweenKingAndCastle(final ChessFigure king,
                                                    final Point kingPos, final ChessField field, final int offsetX) {
        try {
            // Находим вертикаль
            final int lineY = king.getColor().equals(ChessColor.White) ? 7 : 0;

            // Для каждой клетки между королём и ладьёй - в клетке не должно быть никакой фигуры
            for (int i = kingPos.getX() + offsetX;
                 i > 0 && i < field.getFieldSize() - 1; i += offsetX) {
                final Point betweenPoint = new Point(i, lineY);
                final ChessFigure figure = field.getFigure(betweenPoint);
                if (figure != null) return false;
            }

            return true;
        } catch (final InvalidPointException ex) {
            log.error("isNoFiguresBetweenKingAndCastle FATAL error while get cell from field, king={}, kingPos={}, field={}, offsetX={}", king, kingPos, field, offsetX, ex);
        }

        return false;
    }

    // Находит клетки, на которые может ходить или атаковать конь.
    private Collection<Point> findKnightMoveOrAttackCells(final ChessFigure figure,
                                                          final Point figurePosition, final ChessField field) {
        final Collection<Point> findedCells = new ArrayList<>();

        for (final Point delta : DELTA_KNIGHT_MOVES) {
            // Проверяем клетку на допустимость координат. А также нет ли там фигуры своего цвета.
            final Point newPoint = new Point(figurePosition.getX() + delta.getX(), figurePosition.getY() + delta.getY());
            if (isCellNullOrOpponent(figure, newPoint, field))
                findedCells.add(newPoint);
        }

        return findedCells;
    }

    // Находит клетки, на которые может ходить пешка.
    private Collection<Point> findPawnMoveCells(final ChessFigure figure,
                                                final Point figurePos, final ChessField field) {
        final Collection<Point> moveCells = new ArrayList<>();
        final int deltaY = figure.getColor() == ChessColor.White ? -1 : 1;
        boolean isForwardMoveGood = false;

        // Ход вперёд на одну клетку
        Point point = new Point(figurePos.getX(), figurePos.getY() + deltaY);
        if (isCellNull(point, field)) {
            moveCells.add(point);
            isForwardMoveGood = true;
        }

        // Пешка может пройти вперёд на две клетки?
        boolean isOnFirstLine = false;
        if (figure.getColor() == ChessColor.White && figurePos.getY() == WHITE_PAWN_LINE) isOnFirstLine = true;
        if (figure.getColor() == ChessColor.Black && figurePos.getY() == BLACK_PAWN_LINE) isOnFirstLine = true;

        // Ход вперёд на две клетки
        if (isForwardMoveGood && isOnFirstLine) {
            point = new Point(figurePos.getX(), figurePos.getY() + deltaY * 2);
            if (isCellNull(point, field))
                moveCells.add(point);
        }

        // Ход вперед наискосок
        for (final Point diagonalPoint : findPawnAttackCells(figure, figurePos)) {
            if (isCellOpponent(figure, diagonalPoint, field))
                moveCells.add(diagonalPoint);
        }

        return moveCells;
    }

    // Находит две передние диагональные клетки, которые атакует пешка.
    private Collection<Point> findPawnAttackCells(final ChessFigure figure,
                                                  final Point figurePos) {
        final int deltaY = figure.getColor() == ChessColor.White ? -1 : 1;

        // Находим передние диагональные клетки
        final Collection<Point> diagonalCells = new ArrayList<>();
        diagonalCells.add(new Point(figurePos.getX() - 1, figurePos.getY() + deltaY));
        diagonalCells.add(new Point(figurePos.getX() + 1, figurePos.getY() + deltaY));

        return diagonalCells;
    }

    /* Проверяем клетку на допустимость координат. А также нет ли там фигуры своего цвета.
    Надо для короля, коня
    */
    private boolean isCellNullOrOpponent(final ChessFigure selfFigure,
                                         final Point otherCell, final ChessField field) {
        try {
            final ChessFigure otherFigure = field.getFigure(otherCell);
            if (otherFigure == null || otherFigure.getColor() != selfFigure.getColor()) {
                return true;
            }
        } catch (final InvalidPointException ex) {
        }

        return false;
    }

    /* Проверяем клетку на допустимость координат. А также свободна ли клетка.
    Надо для пешки
    */
    private boolean isCellNull(final Point otherCell, final ChessField field) {
        try {
            final ChessFigure otherFigure = field.getFigure(otherCell);
            if (otherFigure == null) {
                return true;
            }
        } catch (final InvalidPointException ex) {
        }

        return false;
    }

    /* Проверяем клетку на допустимость координат. А также стоит ли там фигура оппонента.
    Надо для пешки
    */
    private boolean isCellOpponent(final ChessFigure selfFigure,
                                   final Point otherCell, final ChessField field) {
        try {
            final ChessFigure otherFigure = field.getFigure(otherCell);
            if (otherFigure != null && otherFigure.getColor() != selfFigure.getColor()) {
                return true;
            }
        } catch (final InvalidPointException ex) {
        }

        return false;
    }

}
