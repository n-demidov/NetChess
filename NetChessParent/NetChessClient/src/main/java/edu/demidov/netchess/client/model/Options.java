package edu.demidov.netchess.client.model;

public class Options
{
    
    // Адрес сервера
    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 22_222;
    
    // Заголовки окон
    public static final String MAIN_WINDOW_TITLE = "NetChess";
    public static final String LOGIN_DIALOG_TITLE = "Логин/Создать аккаунт";
    
    // fxml-файлы
    public static final String MAIN_SCENE_FILE = "/fxml/MainScene.fxml";
    public static final String LOGIN_DIALOG_FILE = "/fxml/LoginDialogScene.fxml";
    
    public static final int PASSWORD_MIN_LENGTH = 5;
    
}
