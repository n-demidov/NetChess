package edu.demidov.netchess.server.model.game;

import edu.demidov.netchess.common.model.exceptions.game.chess.GameMoveException;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidBoardSizeException;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidPointException;
import edu.demidov.netchess.common.model.exceptions.game.chess.NoKingOnFieldException;
import edu.demidov.netchess.common.model.exceptions.game.chess.NoNextPlayerFoundException;
import edu.demidov.netchess.common.model.game.chess.ChessColor;
import edu.demidov.netchess.common.model.game.chess.ChessField;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import edu.demidov.netchess.server.model.Options;
import edu.demidov.netchess.server.model.exceptions.NoSuchUserException;
import edu.demidov.netchess.server.model.exceptions.NoSuchUserInGameException;
import edu.demidov.netchess.server.model.exceptions.OneOfUserIsPlayingException;
import edu.demidov.netchess.server.model.exceptions.UserLoginException;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.users.AccountManager;
import edu.demidov.netchess.server.model.users.User;
import edu.demidov.netchess.utils.Point;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameManager implements GameChangedObservable
{
    
    private static final String MATE = "Мат";
    private static final String TIME_IS_UP = "Время истекло";
    private static final String OPPONENT_SURRENDER = "Оппонент сдался";
    private static final String STALEMATE  = "Пат";
    private static final String PLAYERS_AGREED_TO_DRAW = "Оппоненты согласились на ничью";
    private static final String GAME_ALREADY_FINISHED = "Партия уже завершена";
    public static final String UNKNOWN_FIGURE_TYPE_EXCEPTION = "Неизвестный тип фигуры. Выберите ферзя, ладью, слона или коня.";
    private static final String CURRENT_PLAYER_MUST_CHOOSE_FIGURE_EXCEPTION = "Не удалось найти пешку на доске";
    private static final String NO_NEXT_PLAYER_FOUND_EXCEPTION = "Не удалось найти следующего игрока";
    private final static String MOVE_SEQUENCE_EXCEPTION = "Дождитесь своего хода. Сейчас ходит игрок '%s'.";
    
    private static final Random RANDOM = new Random();
    private static final int RANDOM_ID_MAX = 999_999_999;
    private static final int CHESS_SIZE = 8;
    
    private static GameManager instance;
    private final List<GameChangedObserver> listeners;
    
    private final Map<User, ChessGame> map;
    private final AccountManager accountManager;
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final ChessRuleChecker chessRuleChecker;
    private Date nextLaunch = Calendar.getInstance().getTime();
    private final static Logger log = LoggerFactory.getLogger(GameManager.class);
    
    public static synchronized GameManager getInstance()
    {
        if (instance == null)
        {
            instance = new GameManager();
        }
        return instance;
    }
    
    private GameManager()
    {
        map = new HashMap<>();
        chessRuleChecker = ChessRuleChecker.getInstance();
        accountManager = AccountManager.getInstance();
        listeners = new ArrayList<>();
    }
    
    @Override
    public void addListener(final GameChangedObserver listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final GameChangedObserver listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void notifyListenersThatGameChanged(final ChessGame game)
    {
        log.trace("notifyListenersThatGameChanged game={}", game);
        for (final GameChangedObserver listener : listeners)
            listener.gameChanged(game);
    }
    
    /**
     * Создаёт новую игру для игроков
     * @param user1
     * @param user2
     * @throws OneOfUserIsPlayingException 
     */
    public void startGame(final User user1, final User user2)
            throws OneOfUserIsPlayingException
    {
        log.debug("startGame user1={}, user2={}", user1, user2);
        try
        {
            // Проверяем что оба игрока не играют в данный момент
            checkThatUsersFree(user1, user2);

            // Создаём игроков
            final List<ChessPlayer> players = createPlayers(user1, user2);

            // Создаём игровую доску
            final ChessField field = new ChessField(CHESS_SIZE);

            final ChessGame game = new ChessGame(
                    RANDOM.nextInt(RANDOM_ID_MAX),
                    field,
                    players
            );
            
            // Обновляем текущую игру пользователей
            map.put(user1, game);
            map.put(user2, game);
            
            notifyListenersThatGameChanged(game);
        } catch (final NoNextPlayerFoundException | InvalidBoardSizeException | InvalidPointException ex)
        {
            log.error("Exception, user1={}, user2={}", user1, user2, ex);
        }
    }
    
    /**
     * Возвращает текущую игру для игрока
     * @param user
     * @return 
     */
    public ChessGame getCurrentGame(final User user)
    {
        log.trace("getCurrentGame user={}", user);
        return map.getOrDefault(user, null);
    }
    
    /**
     * Возвращает true, если пользователь в данный момент играет
     * @param user
     * @return 
     */
    public boolean isUserPlaying(final User user)
    {
        return getCurrentGame(user) != null;
    }
    
    /**
     * Проверяет окончание всех партий по времени. А также подчищает хеш-таблицу.
     * Метод запускает обработку раз в n секунд.
     */
    public void manageGamesTime()
    {
        if (nextLaunch.before(Calendar.getInstance().getTime()))
        {
            log.trace("manageGamesTime starts process by time");
            // Устанавливаем время следующей проверки TTL
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, Options.GAMES_FREQ_MANAGE_SECONDES);
            nextLaunch = calendar.getTime();
            
            // Для каждой игры
            final Iterator<Entry<User, ChessGame>> it = map.entrySet().iterator();
            while (it.hasNext())
            {
                final Entry<User, ChessGame> entry = it.next();
                final ChessGame game = entry.getValue();
                
                // Если игра = null или уже завершилась - удаляем
                if (game == null || game.isFinished())
                {
                    log.trace("manageGamesTime the game will remove: null or finished, game={}", game);
                    it.remove();
                    continue;
                }
                
                try
                {
                    // Подсчитываем - не истекло ли время у текущего игрока
                    checkGameForEndByTime(game);
                    if (game.isFinished())
                    {
                        log.trace("manageGamesTime the game will remove: finished, game={}", game);
                        it.remove();
                    }
                } catch (final NoNextPlayerFoundException ex)
                {
                    log.error("Error during manageGamesTime, game={}", game, ex);
                }
            }
        }
    }
    
    /**
     * Возвращает список пользователей, которые играют в игре game
     * @param game
     * @return 
     */
    public Set<User> getPlayingUsers(final ChessGame game)
    {
        final Set<User> playingUsers = new HashSet<>();
        for (final ChessPlayer player : game.getPlayers())
        {
            try
            {
                final User user = accountManager.getUser(player.getName());
                if (user != null) playingUsers.add(user);
            } catch (final NoSuchUserException ex)
            {
                log.error("Exception, game={}", game, ex);
            }
        }
        return playingUsers;
    }

    // Возвращает объект ChessPlayer по объекту User
    public ChessPlayer getPlayer(final User user, final ChessGame game)
            throws NoSuchUserInGameException
    {
        final String userName = user.getName();
        
        for (final ChessPlayer player : game.getPlayers())
        {
            if (userName.equals(player.getName()))
                return player;
        }
        
        log.error("getPlayer NoSuchUserInGameException, user={}, game={}", user, game);
        throw new NoSuchUserInGameException(userName);
    }

    /**
     * Игрок сделал ход
     * @param player
     * @param game
     * @param fromPoint
     * @param toPoint
     * @throws GameMoveException 
     */
    public void playerMoveFigure(final ChessPlayer player, final ChessGame game,
            final Point fromPoint, final Point toPoint) throws GameMoveException
    {
        log.debug("playerMoveFigure player={}, game={}, fromPoint={}, toPoint={}", player, game, fromPoint, toPoint);
        try
        {
            final long currentMoveTime = calculateCurrentMoveMillis(game);  // Подсчитываем время текущего хода
            
            checkThatGameNotFinished(game);
            checkThatPlayerIsCurrent(player, game);
            
            // Если текущий игрок должен выбрать новую фигуру вместо пешки - выходим
            if (game.isCurrentPlayerChoosingFigure())
                throw new GameMoveException(CURRENT_PLAYER_MUST_CHOOSE_FIGURE_EXCEPTION);
            
            // Если ход не корректный (и почему-то не выкинулось исключение с описанием) - выходим
            if (!chessRuleChecker.isMoveCorrect(player.getColor(), game.getField(), fromPoint, toPoint))
                return;
            
            // Делаем сам ход
            game.moveFigure(fromPoint, toPoint);

            processIfCurrentPlayerChanged(game, player, currentMoveTime);
        } catch (final NoKingOnFieldException | NoNextPlayerFoundException | InvalidPointException ex)
        {
            log.warn("Exception: player={}, game={}, fromPoint={}, toPoint={}", player, game, fromPoint, toPoint, ex);
            throw new GameMoveException(ex.toString());
        }
        
        notifyListenersThatGameChanged(game);
    }
    
    /**
     * Игрок выбрал фигуру (когда пешка дошла до конца доски)
     * @param player
     * @param game
     * @param chosenFigureType
     * @throws GameMoveException 
     */
    public void playerChooseFigureInsteadPawn(final ChessPlayer player,
            final ChessGame game, final ChessFigure.Type chosenFigureType) throws GameMoveException
    {
        log.trace("playerChooseFigureInsteadPawn player={}, game={}, chosenFigureType={}", player, game, chosenFigureType);
        try
        {
            final long currentMoveTime = calculateCurrentMoveMillis(game);  // Подсчитываем время текущего хода
            
            checkThatGameNotFinished(game);
            checkThatPlayerIsCurrent(player, game);

            // Проверяем может ли текущий игрок выбрать фигуру
            if (!game.isCurrentPlayerChoosingFigure()) return;
            
            // Проверяем тип фигуры
            if (chosenFigureType == null || chosenFigureType == ChessFigure.Type.King || chosenFigureType == ChessFigure.Type.Pawn)
                throw new GameMoveException(UNKNOWN_FIGURE_TYPE_EXCEPTION);

            // Заменяем пешку игрока на выбранную фигуру
            game.transformPawn(player, chosenFigureType);

            processIfCurrentPlayerChanged(game, player, currentMoveTime);
        } catch (final InvalidPointException | NoKingOnFieldException | NoNextPlayerFoundException ex)
        {
            log.error("Exception: game={}, player={}, chosenFigureType={}", game, player, chosenFigureType, ex);
            throw new GameMoveException(ex.toString());
        }
        
        notifyListenersThatGameChanged(game);
    }
    
    /**
     * Игрок сдался
     * @param player
     * @param game
     * @throws GameMoveException 
     */
    public void playerSurrender(final ChessPlayer player, final ChessGame game)
            throws GameMoveException
    {
        log.trace("playerSurrender, game={}, player={}", game, player);
        try
        {
            endGame(game, game.getNextPlayer(player), OPPONENT_SURRENDER);
        } catch (final NoNextPlayerFoundException ex)
        {
            log.error("Exception: game={}, player={}", game, player, ex);
            throw new GameMoveException(NO_NEXT_PLAYER_FOUND_EXCEPTION);
        }
    }
    
    /**
     * Игрок предложил ничью.
     * @param offeredDrawPlayer
     * @param game 
     */
    public void playerOfferedDraw(final ChessPlayer offeredDrawPlayer, final ChessGame game)
    {
        log.trace("playerOfferedDraw, offeredDrawPlayer={}, game={}", offeredDrawPlayer, game);
        if (!offeredDrawPlayer.isOfferedDraw())
        {
            offeredDrawPlayer.setOfferedDraw(true);
            
            // Подсчитываем все ли игроки согласны на ничью
            boolean isAllUsersWantDraw = true;
            for (final ChessPlayer otherPlayer : game.getPlayers())
            {
                if (!otherPlayer.isOfferedDraw())
                {
                    isAllUsersWantDraw = false;
                    break;
                }
            }
            
            if (isAllUsersWantDraw)
            {
                // Объявляем ничью
                endGame(game, null, PLAYERS_AGREED_TO_DRAW);
            } else
            {
                // Уведомляем других, что один из игроков предложил ничью
                notifyListenersThatGameChanged(game);
            }
        }
    }

    /* Проверяет не закончилась ли игра, либо не истекло ли время.
    В случае ошибки - выбрасывает исключение.
    */
    private void checkThatGameNotFinished(final ChessGame game) throws GameMoveException
    {
        log.trace("checkThatGameNotFinished game={}", game);
        try
        {
            // Проверяем не истекло ли время для текущей партии
            checkGameForEndByTime(game);
            
            // Проверяем не закончилась ли игра
            if (game.isFinished())
                throw new GameMoveException(GAME_ALREADY_FINISHED);
        } catch (final NoNextPlayerFoundException ex)
        {
            log.error("Exception: game={}", game, ex);
            throw new GameMoveException(NO_NEXT_PLAYER_FOUND_EXCEPTION);
        }
    }
    
    // Проверяет ходит ли сейчас этот игрок
    private void checkThatPlayerIsCurrent(final ChessPlayer player, final ChessGame game) throws GameMoveException
    {
        log.trace("checkThatPlayerIsCurrent player={}, game={}", player, game);
        final ChessPlayer currentPlayer = game.getCurrentPlayer();
        
        if (!player.equals(currentPlayer))
            {
                throw new GameMoveException(String.format(
                        MOVE_SEQUENCE_EXCEPTION,
                        currentPlayer == null ? "" : currentPlayer.getName()));
            }
    }
    
    /* Если ход передался новому игроку:
    - проверяем нет ли мата;
    - уменьшаем время хода
    */
    private void processIfCurrentPlayerChanged(final ChessGame game,
            final ChessPlayer prevPlayer, final long currentMoveTime)
            throws NoKingOnFieldException, NoNextPlayerFoundException
    {
        if (!prevPlayer.equals(game.getCurrentPlayer()))
        {
            // Проверяем нет ли мата/пата
            checkGameForEndByPosition(game);

            // Уменьшаем оставшееся время у игрока, сделавшего ход
            prevPlayer.setTimeLeft(prevPlayer.getTimeLeft() - currentMoveTime);
        }
    }
    
    // Создаёт игроков
    private List<ChessPlayer> createPlayers(final User user1, final User user2)
    {
        log.trace("createPlayers user1={}, user2={}", user1, user2);
        final List<ChessPlayer> players = new ArrayList<>();
        
        // Случайно распределяем очерёдность хода
        if (RANDOM.nextBoolean())
        {
            players.add(createPlayer(user1, ChessColor.White));
            players.add(createPlayer(user2, ChessColor.Black));
        } else
        {
            players.add(createPlayer(user2, ChessColor.White));
            players.add(createPlayer(user1, ChessColor.Black));
        }
        
        return players;
    }
    
    // Создаёт объект ChessPlayer из объекта User
    private ChessPlayer createPlayer(final User user, final ChessColor color)
    {
        return new ChessPlayer(
                color, Options.GAME_TIME_TO_PLAYER_MILLIS,
                user.getName(), user.getRank(),
                user.getWins(), user.getDefeats(), user.getDraws(),
                user.getTotalTimeOnServer() + connectionManager.getUserCurrentTimeOnServer(user));
    }
    
    // Проверяет игру на окончание партии по структуре доски
    private void checkGameForEndByPosition(final ChessGame game)
            throws NoKingOnFieldException, NoNextPlayerFoundException
    {
        log.trace("checkGameForEndByPosition game={}", game);
        final ChessPlayer currentPlayer = game.getCurrentPlayer();
        final ChessField field = game.getField();
        
        /* Если не осталось ни одного допустимого хода для текущего игрока,
        то это либо мат (если сейчас шах), либо пат (если шаха нет).
        */
        if (chessRuleChecker.isNoMoreMoves(currentPlayer.getColor(), field))
        {
            if (chessRuleChecker.isCheckForPlayer(currentPlayer.getColor(), field))
            {
                endGame(game, game.getNextPlayer(currentPlayer), MATE);     // мат
            } else
            {
                endGame(game, null, STALEMATE);     // пат
            }
        }
    }
    
    /**
     * Проверяет на завершение партии по времени
     * @param game 
     * @throws NoNextPlayerFoundException
     */
    private void checkGameForEndByTime(final ChessGame game) throws NoNextPlayerFoundException
    {
        log.trace("checkGameForEndByTime game={}", game);
        if (game.isFinished()) return;
        
        final ChessPlayer currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;
        
        // Время текущего хода
        final long currentMoveTime = Calendar.getInstance().getTime().getTime() - game.getCurrentMoveStarted().getTime();
        
        // Оставшееся время текущего игрока
        final long playerLeftTime = currentPlayer.getTimeLeft() - currentMoveTime;
        
        if (playerLeftTime < 0)
        {
            endGame(game, game.getNextPlayer(currentPlayer), TIME_IS_UP);
        }
    }
    
    // Завершает партию
    private void endGame(final ChessGame game, final ChessPlayer winner, final String reasonResult)
    {
        log.debug("endGame game={}, winner={}, reasonResult={}", game, winner, reasonResult);
        try
        {
            game.end(winner, reasonResult);
            
            // Начисляем очки, кол-во игр
            savePlayersChanges(game, winner);
            
            // Освобождаем игроков
            for (final User user : getPlayingUsers(game))
                map.put(user, null);
            
            notifyListenersThatGameChanged(game);
        } catch (final NoNextPlayerFoundException ex)
        {
            log.error("Exception: game={}, winner={}, reasonResult={}", game, winner, reasonResult, ex);
        }
    }
    
    // Сохраняет обновленную информацияю об игроках
    private void savePlayersChanges(final ChessGame game, final ChessPlayer winner)
    {
        log.trace("savePlayersChanges game={}, winner={}", game, winner);
        for (final ChessPlayer player : game.getPlayers())
        {
            try
            {
                final User user = accountManager.getUser(player.getName());
                
                // Очки
                user.setRank(user.getRank() + player.getAccruedScores());

                // Начисляем победы, поражения, ничьи
                if (winner == null)
                {
                    user.setDraws(user.getDraws() + 1);
                } else
                {
                    if(user.getName().equals(winner.getName()))
                    {
                        user.setWins(user.getWins() + 1);
                    } else
                    {
                        user.setDefeats(user.getDefeats() + 1);
                    }
                }

                // Сохраняем изменения
                accountManager.updateUser(user);
            } catch (final NoSuchUserException | FileNotFoundException | UserLoginException ex)
            {
                log.error("Exception game={}, winner={}", game, winner, ex);
            }
        }
    }
    
    // Проверяет, что оба игрока не играют в данный момент
    private void checkThatUsersFree(final User user1, final User user2)
            throws OneOfUserIsPlayingException
    {
        if (isUserPlaying(user1) || isUserPlaying(user2))
        {
            throw new OneOfUserIsPlayingException();
        }
    }
    
    // Подсчитывает время текущего хода
    private long calculateCurrentMoveMillis(final ChessGame game)
    {
        return Calendar.getInstance().getTime().getTime() - game.getCurrentMoveStarted().getTime();
    }

}
