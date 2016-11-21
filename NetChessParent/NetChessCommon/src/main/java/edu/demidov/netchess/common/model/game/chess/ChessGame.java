package edu.demidov.netchess.common.model.game.chess;

import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidPointException;
import edu.demidov.netchess.common.model.exceptions.game.chess.NoNextPlayerFoundException;
import edu.demidov.netchess.utils.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ChessGame implements Serializable {

    private static final double SCORES_COEFFICIENT = 0.008;
    private static final int WHITE_PAWN_TRANSFORM_LINE = 0, BLACK_PAWN_TRANSFORM_LINE = 7;
    private static final String NOT_FOUND_PAWN_ON_FIELD_EXCEPTION = "Не удалось найти пешку на доске";
    private final static Logger log = LoggerFactory.getLogger(ChessGame.class);
    private final static Marker fatal = MarkerFactory.getMarker("FATAL");
    private int id;
    private List<ChessPlayer> players;
    private ChessField field;
    private ChessPlayer currentPlayer;
    private Date currentMoveStarted;
    private boolean isCurrentPlayerChoosingFigure;  // Если текущий игрок должен выбрать фигуру (когда пешка дошла до конца доски)
    private Point[] lastMovePoints;
    private boolean isFinished;
    private ChessPlayer result;             // Победитель, null - если ничья
    private String resultReasonDescription; // Описание результата игры (для пользователя)
    private Date finishedGameDate;          // Время окончания партии

    public ChessGame() {
    }

    public ChessGame(final int id, final ChessField field, final List<ChessPlayer> players)
            throws NoNextPlayerFoundException, InvalidPointException {
        log.debug("ChessGame id={}, field={}, players={}", id, field, players);
        assert id != 0;
        assert field != null;
        assert players != null;

        this.id = id;
        this.field = field;
        this.players = players;

        initNewGame();
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public List<ChessPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(final List<ChessPlayer> players) {
        this.players = players;
    }

    public ChessField getField() {
        return field;
    }

    public void setField(final ChessField field) {
        this.field = field;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(final boolean isFinished) {
        if (!this.isFinished) finishedGameDate = Calendar.getInstance().getTime();
        this.isFinished = isFinished;
    }

    public ChessPlayer getResult() {
        return result;
    }

    public void setResult(final ChessPlayer result) {
        this.result = result;
    }

    public ChessPlayer getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(final ChessPlayer currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public Date getCurrentMoveStarted() {
        return currentMoveStarted;
    }

    public void setCurrentMoveStarted(final Date currentMoveStarted) {
        this.currentMoveStarted = currentMoveStarted;
    }

    public Point[] getLastMovePoints() {
        return lastMovePoints;
    }

    public void setLastMovePoints(final Point[] lastMovePoints) {
        this.lastMovePoints = lastMovePoints;
    }

    public String getResultReasonDescription() {
        return resultReasonDescription;
    }

    public void setResultReasonDescription(final String resultReasonDescription) {
        this.resultReasonDescription = resultReasonDescription;
    }

    public boolean isCurrentPlayerChoosingFigure() {
        return isCurrentPlayerChoosingFigure;
    }

    public void setCurrentPlayerChoosingFigure(final boolean isCurrentPlayerChoosingFigure) {
        this.isCurrentPlayerChoosingFigure = isCurrentPlayerChoosingFigure;
    }

    public Date getFinishedGameDate() {
        return finishedGameDate;
    }

    public void setFinishedGameDate(final Date finishedGameDate) {
        this.finishedGameDate = finishedGameDate;
    }

    /**
     * Возвращает игрока, следующего за указанным.
     * В случае отсутствия следующего игрока - выбрасывает исключение
     *
     * @param player
     * @return
     * @throws NoNextPlayerFoundException
     */
    public ChessPlayer getNextPlayer(final ChessPlayer player) throws NoNextPlayerFoundException {
        log.debug("getNextPlayer player={}", player);
        assert player != null;
        int playerIndex = players.indexOf(player);

        // Ищем следующего игрока
        for (int i = playerIndex + 1; i < players.size(); i++) {
            if (players.get(i) != null) {
                return players.get(i);
            }
        }

        for (int i = 0; i < playerIndex; i++) {
            if (players.get(i) != null) {
                return players.get(i);
            }
        }

        throw new NoNextPlayerFoundException();
    }

    /**
     * Передвигает фигуру
     * Также передаёт ход следующему игроку
     *
     * @param fromPoint
     * @param toPoint
     * @throws InvalidPointException
     * @throws NoNextPlayerFoundException
     */
    public void moveFigure(final Point fromPoint, final Point toPoint)
            throws InvalidPointException, NoNextPlayerFoundException {
        log.trace("moveFigure fromPoint={}, toPoint={}", fromPoint, toPoint);
        final ChessFigure figure = field.getFigure(fromPoint);
        assert figure != null;

        // Передвигаем фигуру
        moveFigure(figure, fromPoint, toPoint, field);

        // Устанавливаем предыдущий ход
        final Point[] points = new Point[2];
        points[0] = fromPoint;
        points[1] = toPoint;
        setLastMovePoints(points);

        // Если пешка дошла до конца доски (может стать любой фигурой)
        if (figure.getType() == ChessFigure.Type.Pawn) {
            if (figure.getColor() == ChessColor.White && toPoint.getY() == WHITE_PAWN_TRANSFORM_LINE ||
                    figure.getColor() == ChessColor.Black && toPoint.getY() == BLACK_PAWN_TRANSFORM_LINE) {
                // Оставляем ход текущему игроку. Помечаем, что текущий игрок должен выбрать фигуру.
                isCurrentPlayerChoosingFigure = true;
                return;
            }
        }

        // Передаём ход следующему игроку
        giveMoveToNextPlayer();
    }

    /**
     * Превращает пешку игрока (дошедшую до конца доски) в указанную фигуру
     *
     * @param player
     * @param chosenFigureType
     * @throws GameMoveException
     * @throws NoNextPlayerFoundException
     * @throws InvalidPointException
     */
    public void transformPawn(final ChessPlayer player, final ChessFigure.Type chosenFigureType)
            throws GameMoveException, NoNextPlayerFoundException, InvalidPointException {
        log.debug("transformPawn player={}, chosenFigureType={}", player, chosenFigureType);
        // Ищем пешку на поле оппонента
        final Point queenedPoint = findQueenedPawn(field, player.getColor());
        final ChessFigure pawn = field.getFigure(queenedPoint);

        // Заменяем найденную пешку на выбранную фигуру
        final ChessFigure newFigure = new ChessFigure(pawn.getColor(), chosenFigureType);
        newFigure.setMovesCount(pawn.getMovesCount());
        field.setFigure(queenedPoint, newFigure);

        isCurrentPlayerChoosingFigure = false;

        // Передаём ход следующему игроку
        giveMoveToNextPlayer();
    }

    /**
     * Завершает игру
     *
     * @param winner
     * @param resultDescription
     * @throws NoNextPlayerFoundException
     */
    public void end(final ChessPlayer winner, final String resultDescription) throws NoNextPlayerFoundException {
        log.debug("end winner={}, resultDescription={}", winner, resultDescription);

        setFinished(true);
        setResult(winner);
        setResultReasonDescription(resultDescription);

        // Начисляем очки за игру
        calculateScoresForPlayers(winner);
    }

    @Override
    public String toString() {
        return "ChessGame{" + "id=" + id + ", players=" + players + ", field=" + field + ", currentPlayer=" + currentPlayer + ", currentMoveStarted=" + currentMoveStarted + ", lastMovePoints=" + Arrays.toString(lastMovePoints) + ", isFinished=" + isFinished + ", result=" + result + ", resultReasonDescription=" + resultReasonDescription + '}';
    }

    // Ищет пешку, дошедшую до конца поля оппонента. Если не находит - выбрасывает исключение.
    private Point findQueenedPawn(final ChessField field, final ChessColor color)
            throws GameMoveException {
        log.trace("findQueenedPawn field={}, color={}", field, color);
        try {
            final int opponentHorizontal = color == ChessColor.White ? 0 : 7;

            for (int i = 0; i < field.getFieldSize(); i++) {
                final Point point = new Point(i, opponentHorizontal);
                final ChessFigure pawn = field.getFigure(point);

                if (pawn == null || pawn.getColor() != color ||
                        pawn.getType() != ChessFigure.Type.Pawn)
                    continue;

                return point;
            }
        } catch (final InvalidPointException ex) {
            log.error(fatal, "findQueenedPawn: error while iterate field; field={}, color={}", field, color, ex);
        }

        // Если не нашли пешку - выбрасываем исключение
        log.warn("findQueenedPawn: can't find queened pawn; field={}, color={}", field, color);
        throw new GameMoveException(NOT_FOUND_PAWN_ON_FIELD_EXCEPTION);
    }

    // Инициилизирует новую игру
    private void initNewGame() throws NoNextPlayerFoundException, InvalidPointException {
        // Передаём ход первому игроку
        giveMoveToNextPlayer();

        // Расставляем фигуры
        initChessFigures();
    }

    // Расставляет фигуры в начальное положение
    private void initChessFigures() throws InvalidPointException {
        // Пешки
        for (int i = 0; i < field.getFieldSize(); i++) {
            field.setFigure(new Point(i, 1), new ChessFigure(ChessColor.Black, ChessFigure.Type.Pawn));
            field.setFigure(new Point(i, 6), new ChessFigure(ChessColor.White, ChessFigure.Type.Pawn));
        }

        // Ладьи
        for (int i = 0; i < field.getFieldSize(); i += 7) {
            field.setFigure(new Point(i, 0), new ChessFigure(ChessColor.Black, ChessFigure.Type.Castle));
            field.setFigure(new Point(i, 7), new ChessFigure(ChessColor.White, ChessFigure.Type.Castle));
        }

        // Кони
        for (int i = 1; i < field.getFieldSize(); i += 5) {
            field.setFigure(new Point(i, 0), new ChessFigure(ChessColor.Black, ChessFigure.Type.Knight));
            field.setFigure(new Point(i, 7), new ChessFigure(ChessColor.White, ChessFigure.Type.Knight));
        }

        // Слоны
        for (int i = 2; i < field.getFieldSize(); i += 3) {
            field.setFigure(new Point(i, 0), new ChessFigure(ChessColor.Black, ChessFigure.Type.Bishop));
            field.setFigure(new Point(i, 7), new ChessFigure(ChessColor.White, ChessFigure.Type.Bishop));
        }

        // Ферзи
        field.setFigure(new Point(3, 0), new ChessFigure(ChessColor.Black, ChessFigure.Type.Queen));
        field.setFigure(new Point(3, 7), new ChessFigure(ChessColor.White, ChessFigure.Type.Queen));

        // Короли
        field.setFigure(new Point(4, 0), new ChessFigure(ChessColor.Black, ChessFigure.Type.King));
        field.setFigure(new Point(4, 7), new ChessFigure(ChessColor.White, ChessFigure.Type.King));
    }

    // Передвигает фигуру на поле
    private void moveFigure(ChessFigure figure, Point fromPoint, Point toPoint,
                            ChessField field) throws InvalidPointException {
        log.trace("moveFigure figure={}, fromPoint={}, toPoint={}, field={}", figure, fromPoint, toPoint, field);
        // Если король делает рокировку
        final int deltaX = Math.abs(fromPoint.getX() - toPoint.getX());
        if (figure.getType() == ChessFigure.Type.King && deltaX == 2) {
            // Ищем ладью
            final int offsetX = (toPoint.getX() < fromPoint.getX()) ? -1 : 1;
            final int castleX = (offsetX == -1) ? 0 : 7;
            final Point castleStartPoint = new Point(castleX, fromPoint.getY());
            final ChessFigure castle = field.getFigure(castleStartPoint);

            // Передвигаем короля
            field.setFigure(fromPoint, null);
            field.setFigure(toPoint, figure);

            // Передвигаем ладью
            field.setFigure(castleStartPoint, null);
            field.setFigure(new Point(fromPoint.getX() + offsetX, fromPoint.getY()), castle);

            // Плюсуем счётчик перемещений
            figure.setMovesCount(figure.getMovesCount() + 1);
            castle.setMovesCount(castle.getMovesCount() + 1);

            return;
        }

        // Передвигаем фигуру
        field.setFigure(fromPoint, null);
        field.setFigure(toPoint, figure);

        // Плюсуем счётчик перемещений
        figure.setMovesCount(figure.getMovesCount() + 1);
    }

    // Передаёт ход к следующему игроку
    private void giveMoveToNextPlayer() throws NoNextPlayerFoundException {
        log.trace("giveMoveToNextPlayer");
        if (currentPlayer == null) {
            currentPlayer = players.get(0);
        } else {
            currentPlayer = getNextPlayer(currentPlayer);
        }

        // Сбрасываем предложение ничьи у этого игрока
        currentPlayer.setOfferedDraw(false);

        // Устанавливаем время текущего хода
        currentMoveStarted = Calendar.getInstance().getTime();
    }

    // Рассчитывает игрокам очки за игру
    private void calculateScoresForPlayers(final ChessPlayer winner)
            throws NoNextPlayerFoundException {
        log.debug("calculateScoresForPlayers winner={}", winner);
        if (winner == null)     // если ничья
        {
            // При ничье "каждому игроку прибавляется количество очков оппонента, умноженное на k/2"
            addScoresToPlayer(getPlayers().get(0), getPlayers().get(1), 0.5);
            addScoresToPlayer(getPlayers().get(1), getPlayers().get(0), 0.5);
        } else {
            // Выигравшему начисляется кол-во очков проигравшего, умноженное на k
            addScoresToPlayer(winner, getNextPlayer(winner), 1);
        }
    }

    // Добавляет очки игроку
    private void addScoresToPlayer(final ChessPlayer player, final ChessPlayer opponent, final double k) {
        log.trace("addScoresToPlayer player={}, opponent={}, k={}", player, opponent, k);
        int scores = (int) (opponent.getRank() * SCORES_COEFFICIENT * k);
        if (scores < 1) scores = 1;
        player.setAccruedScores(scores);
    }

}
