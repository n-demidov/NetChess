package edu.demidov.netchess.server.model;

public class Options {
    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 22_222;

    public static final int USER_RANK_DEFAULT = 1000;               // игровой ранг по умолчанию
    public static final int USERS_INFO_SEND_UPDATES_SECONDS = 7;    // как часто сервер будет рассылать пользователям инфу (приглашения, лобби и пр.)

    public static final int CONNECTIONS_FREQ_MANAGE_SECONDS = 60;   // как часто проверять соединения
    public static final int CONNECTION_UNAUTH_TTL_SECONDS = 60;     // минимальное TTL незалогиненного соединения

    public static final int INVITATIONS_TTL_MINUTES = 10;           // минимальное время жизни приглашений (TTL), в минутах
    public static final int INVITATIONS_FREQ_MANAGE_MINUTES = 3;    // как часто запускать процедуру проверки TTL, в минутах

    public static final int GAME_TIME_TO_PLAYER_MILLIS = 1000 * 60 * 30;    // время на партию для игрока, миллисекунды
    public static final int GAMES_FREQ_MANAGE_SECONDES = 3;                 // как часто запускается проверка истечения времени партий, секунды

    private Options() {
    }
}
