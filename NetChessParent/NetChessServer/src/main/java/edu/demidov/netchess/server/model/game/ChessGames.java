package edu.demidov.netchess.server.model.game;

import edu.demidov.netchess.common.model.exceptions.game.chess.NoNextPlayerFoundException;
import edu.demidov.netchess.common.model.game.chess.ChessColor;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import edu.demidov.netchess.game.api.ChessLogic;
import edu.demidov.netchess.game.api.ChessLogicObserver;
import edu.demidov.netchess.game.impl.ChessLogicImpl;
import edu.demidov.netchess.server.controllers.ClientUpdater;
import edu.demidov.netchess.server.model.Options;
import edu.demidov.netchess.server.model.exceptions.NoSuchUserException;
import edu.demidov.netchess.server.model.exceptions.NoSuchUserInGameException;
import edu.demidov.netchess.server.model.exceptions.OneOfUserIsPlayingException;
import edu.demidov.netchess.server.model.exceptions.UserLoginException;
import edu.demidov.netchess.server.model.invitations.InvitationsObserver;
import edu.demidov.netchess.server.model.network.ConnectionManager;
import edu.demidov.netchess.server.model.users.AccountManager;
import edu.demidov.netchess.server.model.users.User;
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

public class ChessGames implements InvitationsObserver, ChessLogicObserver
{
    private static final Random RANDOM = new Random();
    private final static Logger log = LoggerFactory.getLogger(ChessGames.class);
    private static ChessGames instance;

    private final AccountManager accountManager;
    private final ConnectionManager connectionManager;
    private final ChessLogic gameLogic;
    private final ClientUpdater clientUpdater;

    private final Map<User, ChessGame> map;
    private Date nextLaunch = Calendar.getInstance().getTime();
    
    public static synchronized ChessGames getInstance()
    {
        if (instance == null)
        {
            instance = new ChessGames();
        }
        return instance;
    }
    
    private ChessGames()
    {
        map = new HashMap<>();

        accountManager = AccountManager.getInstance();
        gameLogic = ChessLogicImpl.getInstance();
        connectionManager = ConnectionManager.getInstance();
        clientUpdater = ClientUpdater.getInstance();
    }

    /**
     * Событие вызывается, когда два игрока договорились сыграть друг с другом
     */
    @Override
    public void usersAgreed(final User player1, final User player2)
    {
        log.debug("usersAgreed player1={}, player2={}", player1, player2);

        try
        {
            if (isAnyonePlayerOffline(player1, player2))
            {
                return;
            }

            checkThatUsersFree(player1, player2);

            final List<ChessPlayer> chessPlayers = createPlayers(player1, player2);
            final ChessGame game = gameLogic.startGame(chessPlayers);

            // Обновляем текущую игру пользователей
            map.put(player1, game);
            map.put(player2, game);

            gameChanged(game);
        } catch (final OneOfUserIsPlayingException ex)
        {
            // Если один из игроков играет - ничего не делаем
            log.trace("usersAgreed: one of user already playing - cancel new game,  player1={}, player2={}", player1, player2);
        }
    }

    @Override
    public void gameChanged(final ChessGame game)
    {
        clientUpdater.gameChanged(game);
    }

    @Override
    public void gameEnded(final ChessGame game, final ChessPlayer winner)
    {
        log.debug("gameEnded game={}", game);

        savePlayersChanges(game, winner);
        releasePlayers(game);

        gameChanged(game);
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
                    gameLogic.checkGameForEndByTime(game);
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

    private boolean isAnyonePlayerOffline(final User player1, final User player2) {
        return !connectionManager.isUserOnline(player1) || !connectionManager.isUserOnline(player2);
    }

    private void releasePlayers(final ChessGame game)
    {
        for (final User user : getPlayingUsers(game))
        {
            map.put(user, null);
        }
    }
}
