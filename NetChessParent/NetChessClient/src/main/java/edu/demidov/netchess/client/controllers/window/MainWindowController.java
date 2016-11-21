package edu.demidov.netchess.client.controllers.window;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;
import edu.demidov.netchess.client.model.Options;
import edu.demidov.netchess.client.model.network.netty.NettyClient;
import edu.demidov.netchess.common.model.exceptions.game.chess.InvalidPointException;
import edu.demidov.netchess.common.model.game.chess.ChessAction;
import edu.demidov.netchess.common.model.game.chess.ChessColor;
import edu.demidov.netchess.common.model.game.chess.ChessField;
import edu.demidov.netchess.common.model.game.chess.ChessFigure;
import edu.demidov.netchess.common.model.game.chess.ChessGame;
import edu.demidov.netchess.common.model.game.chess.ChessPlayer;
import edu.demidov.netchess.common.model.network.NetworkMessage;
import edu.demidov.netchess.common.model.users.UserProfile;
import edu.demidov.netchess.utils.EncryptAlgorithm;
import edu.demidov.netchess.utils.Point;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {

    private static final String CONNECT_WITH_SERVER_FAILED = "<Не удалось связаться с сервером>";
    private static final String CONNECTING_TO_SERVER = "Подключение к серверу...";
    private static final String USER_NOT_LOG_IN = "Вы не вошли",
            USER_NOT_LOGIN_DESCRIPTION = "Нажмите на текст выше, чтобы\nвойти под своим аккаунтом,\nлибо создать новый";
    private static final String YOU_ARE_NOT_LOGIN = "Вы не вошли";
    private static final String PASSWORD_MIN_LENGTH_EXCEPTION
            = String.format("<Пароль не может быть меньше %d символов>", Options.PASSWORD_MIN_LENGTH);

    private static final String USER_LOGIN_INFO = "ранг: %d\nпоб: %d пораж: %d нич: %d\nобщее время на сервере: %s";
    private static final String ONLINE_USERS = "Игроки онлайн: %d";
    private static final String INVITES = "Входящие приглашения: %d";
    private static final String PLAYER_IS_PLAYING = "Играет";
    private static final String PLAYER_IS_FREE = "Свободен";
    private static final String NO_INPUT_INVITATIONS = "Входящих приглашений нет";
    private static final String NO_ONLINE_USERS = "Игроков онлайн нет";

    private static final String BLACK_MOVING = "Ход чёрных", WHITE_MOVING = "Ход белых";
    private static final String BLACK_WINS = "Выиграли чёрные", WHITE_WINS = "Выиграли белые";
    private static final String DRAW = "Ничья";
    private static final String GAME_ALERT_TITLE = "Игровая информация";
    private static final String ACTION_CONFIRM = "Подтверждение действия", GAME_MOVE_ERR = "Ошибка хода";
    private static final String SURRENDER_CONFIRMATION = "Вы действительно хотите сдаться?";
    private static final String DRAW_CONFIRMATION = "Предложить ничью?";
    private static final String ACCEPT_DRAW_CONFIRMATION = "Согласиться на ничью?";

    // Константы для отрисовки игрового поля
    private static final int GAME_CANVAS_WIDTH = 425, CANVAS_HEIGHT = 460;
    private static final int CELL_COUNT = 8, CELL_MIN = 0, CELL_SIZE = 40;
    private static final int CHESSBOARD_X = 50, CHESSBOARD_Y = 90,
            CHESSBOARD_H = CELL_COUNT * CELL_SIZE, CHESSBOARD_W = CHESSBOARD_H,
            CHESSBOARD_CENTER_X = CHESSBOARD_X + CHESSBOARD_W / 2,
            CHESSBOARD_CENTER_Y = CHESSBOARD_Y + CHESSBOARD_H / 2;
    private static final double CHESSBOARD_BORDER_WIDTH = 0.2;
    private static final int NUMERATIONS_OFFSET = 10;
    private static final String[] NUMERATION_STR_HORIZONTAL = new String[]{"a", "b", "c", "d", "e", "f", "g", "h"};
    private static final Font NUMERATION_FONT = new Font(12);
    private static final int CURRENT_CELL_LINE_WIDTH = 1;
    private static final Color CURRENT_CELL_COLOR = Color.GREEN;
    private static final Color LAST_MOVE_CELL_COLOR = Color.grayRgb(125, 0.45);
    // Картинки шахматных фигур
    private static final int IMAGES_PLAYERS_COUNT = 2;
    private static final int FIGURE_IMG_SIZE = 30;
    private static final int COORD_FIGURE_IMAGE_TO_DRAW = (CELL_SIZE - FIGURE_IMG_SIZE) / 2;
    // Кнопки "Предложить ничью" и "сдаться"
    private static final int SURRENDER_BUTTON_X = CHESSBOARD_X + CHESSBOARD_W + 10, SURRENDER_BUTTON_Y = CHESSBOARD_Y + CHESSBOARD_H / 3;
    private static final int DRAW_BUTTON_X = SURRENDER_BUTTON_X, DRAW_BUTTON_Y = CHESSBOARD_Y + CHESSBOARD_H * 2 / 3;
    private static final int BUTTON_WIDTH = 30;
    // Панель с информацией об игроках
    private static final int GAME_INFO_DELTA_Y = 20, GAME_INFO_FIRSTLINE_Y = 15;
    private static final int PLAYER_IMAGE_FIRSTLINE_Y = 20;
    private static final int PLAYER_IMAGE_OFFSET_X = 10, PLAYER_SCORES_OFFSET_Y = 6;
    private final static Logger log = LoggerFactory.getLogger(MainWindowController.class);
    private final static Marker fatal = MarkerFactory.getMarker("FATAL");
    final Color LIGHT_CELL_COLOR = Color.BISQUE, DARK_CELL_COLOR = Color.BURLYWOOD;
    final String GAME_TIME_FORMAT_STRING = "%02d:%02d";
    final Font GAME_TIME_FONT = new Font(16);
    // Панель выбора фигуры (когда пешка прошла до конца доски)
    final String CHOOSE_PANE_TEXT = "Выберите фигуру:";
    final int CHOOSE_FIGURES_COUNT = 4;
    final int CHOOSE_FIGURES_X = CHESSBOARD_CENTER_X - FIGURE_IMG_SIZE / 2 - FIGURE_IMG_SIZE * CHOOSE_FIGURES_COUNT / 2;
    final int CHOOSE_FIGURES_Y = CHESSBOARD_CENTER_Y - FIGURE_IMG_SIZE / 2;
    private final ObservableList<UserProfile> onlineUserProfiles = FXCollections.observableArrayList();
    private final ObservableList<UserProfile> inviters = FXCollections.observableArrayList();
    private final Image[] imgPawn = new Image[IMAGES_PLAYERS_COUNT];
    private final Image[] imgBishop = new Image[IMAGES_PLAYERS_COUNT];
    private final Image[] imgKnight = new Image[IMAGES_PLAYERS_COUNT];
    private final Image[] imgCastle = new Image[IMAGES_PLAYERS_COUNT];
    private final Image[] imgQueen = new Image[IMAGES_PLAYERS_COUNT];
    private final Image[] imgKing = new Image[IMAGES_PLAYERS_COUNT];
    @FXML
    private Label lblLogin;
    @FXML
    private Label lblLoginInfo;
    @FXML
    private Label lblOnlineUsers;
    @FXML
    private Label lblInvites;
    @FXML
    private TextArea txtChat;
    @FXML
    private TextField txtChatAdding;
    // Таблица "Игроки онлайн"
    @FXML
    private TableView tableOnlineUsers;
    @FXML
    private TableColumn<UserProfile, String> columnName;
    @FXML
    private TableColumn<UserProfile, Integer> columnRank;
    @FXML
    private TableColumn<UserProfile, Integer> columnTimeOnServer;
    @FXML
    private TableColumn<UserProfile, Integer> columnWins;
    @FXML
    private TableColumn<UserProfile, Integer> columnDefeats;
    @FXML
    private TableColumn<UserProfile, Integer> columnDraws;
    @FXML
    private TableColumn<UserProfile, Boolean> columnInvited;
    @FXML
    private TableColumn<UserProfile, Boolean> columnPlaying;
    // Таблица "Приглашения"
    @FXML
    private TableView tableInvites;
    @FXML
    private TableColumn<UserProfile, String> invColumnName;
    @FXML
    private TableColumn<UserProfile, String> invColumnRank;
    @FXML
    private TableColumn<UserProfile, String> invColumnWins;
    @FXML
    private TableColumn<UserProfile, String> invColumnDefeats;
    @FXML
    private TableColumn<UserProfile, String> invColumnDraws;
    @FXML
    private Button btnAcceptInvite;
    @FXML
    private Button btnRejectInvite;
    @FXML
    private AnchorPane paneGame;
    @FXML
    private Tab tabGame;
    private Stage mainStage;
    private FXMLLoader fxmlLoader;
    private Parent fxmlLoginDialog;
    private LoginDialogController loginDialogCntrl;
    private Stage loginDialogStage;
    private Alert infoGameAlert;
    private NettyClient nettyClient;
    private UserProfile userProfile;
    // Текущая игра
    private GraphicsContext gameGC;
    private Point gameCurrentCell;      // Выделенная клетка на шахматной доске
    private ChessGame game;
    private Image imgSurrender, imgDraw1, imgDraw2;

    public void setMainStage(final Stage mainStage) {
        this.mainStage = mainStage;
    }

    @Override
    public void initialize(final URL url, final ResourceBundle rb) {
        log.debug("initialize url={}, rb={}", url, rb);
        initListeners();
        initOnlineUsersInfo();
        initInvitersInfo();
        updateUserInfo();
        initLoader();

        initGameGraphics();
    }

    /**
     * Событие вызывается по таймеру каждую секунду
     */
    public void everySecondTicks() {
        paintGameTime();
    }

    /**
     * Событие возникает при изменении списка онлайн пользователей
     *
     * @param onlineProfiles
     */
    public void onlineUserProfilesChanged(final Collection<UserProfile> onlineProfiles) {
        log.trace("onlineUserProfilesChanged onlineProfiles={}", onlineProfiles);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                onlineUserProfiles.setAll(onlineProfiles);
                tableOnlineUsers.sort();
            }
        });
    }

    /**
     * Событие возникает при приходе игровой ошибки от сервера
     *
     * @param errorText
     */
    public void gameError(final String errorText) {
        log.trace("gameError errorText={}", errorText);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (infoGameAlert == null) infoGameAlert = new Alert(AlertType.INFORMATION);

                infoGameAlert.setTitle(GAME_ALERT_TITLE);
                infoGameAlert.setHeaderText(GAME_MOVE_ERR);
                infoGameAlert.setContentText(errorText);
                infoGameAlert.showAndWait();
            }
        });
    }

    /**
     * Событие возникает при изменении списка полученных приглашений
     *
     * @param userProfilesInvites
     */
    public void incomingInvitersChanged(final Collection<UserProfile> userProfilesInvites) {
        log.trace("incomingInvitersChanged userProfilesInvites={}", userProfilesInvites);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                inviters.setAll(userProfilesInvites);
            }
        });
    }

    /**
     * Событие вызывается при успешном логине
     *
     * @param userProfile
     */
    public void loginInfoUpdated(final UserProfile userProfile) {
        log.trace("loginInfoUpdated userProfile={}", userProfile);
        this.userProfile = userProfile;

        // Обновляем инфу о пользователе
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                updateUserInfo();
            }
        });
    }

    /**
     * Событие вызывается при обновлении игры с сервера
     *
     * @param updatedGame
     */
    public void currentGameUpdated(final ChessGame updatedGame) {
        log.trace("currentGameUpdated updatedGame={}", updatedGame);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (updatedGame != null) {
                    game = updatedGame;
                    tabGame.setDisable(false);
                    paintGame();
                }
            }
        });
    }

    public void addToChat(final String s) {
        txtChat.appendText(s + "\n");
    }

    private void initListeners() {
        log.trace("initListeners");
        onlineUserProfiles.addListener(new ListChangeListener<UserProfile>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends UserProfile> c) {
                updateOnlineUsers();
            }
        });
        inviters.addListener(new ListChangeListener<UserProfile>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends UserProfile> c) {
                updateInvites();
            }
        });
    }

    // Инициилизация блока "онлайн игроки"
    private void initOnlineUsersInfo() {
        log.trace("initOnlineUsersInfo");
        updateOnlineUsers();

        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        columnWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        columnDefeats.setCellValueFactory(new PropertyValueFactory<>("defeats"));
        columnDraws.setCellValueFactory(new PropertyValueFactory<>("draws"));

        // Колонка "Провёл времени на сервере"
        columnTimeOnServer.setCellValueFactory(cellData
                -> new SimpleObjectProperty(cellData.getValue().getTotalTimeOnServer()));
        columnTimeOnServer.setCellFactory(column -> {
            return new TableCell<UserProfile, Integer>() {
                @Override
                protected void updateItem(final Integer item, final boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(formatSeconds(item));
                    }
                }
            };
        });

        // Колонка "Играет"
        columnPlaying.setCellValueFactory(cellData
                -> new SimpleBooleanProperty(cellData.getValue().isPlaying()));
        columnPlaying.setCellFactory(column -> {
            return new TableCell<UserProfile, Boolean>() {
                @Override
                protected void updateItem(final Boolean item, final boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setText(null);
                    } else {
                        if (item) {
                            setText(PLAYER_IS_PLAYING);
                            setTextFill(Color.BLACK);
                        } else {
                            setText(PLAYER_IS_FREE);
                            setTextFill(Color.GREEN);
                        }
                    }
                }
            };
        });

        // Колонка "Пригласить"
        columnInvited.setCellFactory(new Callback<TableColumn<UserProfile, Boolean>, TableCell<UserProfile, Boolean>>() {
            @Override
            public TableCell<UserProfile, Boolean> call(final TableColumn<UserProfile, Boolean> param) {
                final CheckBoxTableCell<UserProfile, Boolean> cell = new CheckBoxTableCell<>(index -> {
                    final BooleanProperty invitedProperty = new SimpleBooleanProperty(
                            ((UserProfile) tableOnlineUsers.getItems().get(index)).isInvited());
                    invitedProperty.addListener(new ChangeListener<Boolean>() {
                        @Override
                        public void changed(final ObservableValue<? extends Boolean> observable,
                                            final Boolean oldValue, final Boolean newValue) {
                            final UserProfile item = (UserProfile) tableOnlineUsers.getItems().get(index);
                            item.setInvited(newValue);
                            inviteUserToPlay(item);
                        }
                    });
                    return invitedProperty;
                });

                return cell;
            }
        });

        tableOnlineUsers.setPlaceholder(new Label(NO_ONLINE_USERS));
        tableOnlineUsers.setEditable(true);
        tableOnlineUsers.setItems(onlineUserProfiles);

        // Устанавливаем сортировку
        tableOnlineUsers.getSortOrder().add(columnPlaying);
        tableOnlineUsers.getSortOrder().add(columnName);
    }

    // Инициализация блока приглашений
    private void initInvitersInfo() {
        log.trace("initInvitersInfo");
        updateInvites();

        invColumnName.setCellValueFactory(new PropertyValueFactory<>("name"));

        invColumnRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        invColumnWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        invColumnDefeats.setCellValueFactory(new PropertyValueFactory<>("defeats"));
        invColumnDraws.setCellValueFactory(new PropertyValueFactory<>("draws"));

        tableInvites.setPlaceholder(new Label(NO_INPUT_INVITATIONS));
        tableInvites.setItems(inviters);
    }

    // Загружает загрузчик для модального окна "Логин"
    private void initLoader() {
        log.trace("initLoader");
        try {
            fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource(Options.LOGIN_DIALOG_FILE));
            fxmlLoginDialog = fxmlLoader.load();
            loginDialogCntrl = fxmlLoader.getController();
        } catch (final IOException ex) {
            log.error(fatal, "Exception:", ex);
        }
    }

    /**
     * Обновляет инфу о количестве пользователей
     */
    private void updateOnlineUsers() {
        log.trace("updateOnlineUsers");
        lblOnlineUsers.setText(String.format(ONLINE_USERS, onlineUserProfiles.size()));
    }

    /**
     * Обновляет инфу о приглашениях
     */
    private void updateInvites() {
        log.trace("updateInvites");
        // Обновляем кол-во приглашений
        lblInvites.setText(String.format(INVITES, inviters.size()));

        // Разрешаем\Запрещаем кнопки
        if (inviters.isEmpty()) {
            btnAcceptInvite.setDisable(true);
            btnRejectInvite.setDisable(true);
        } else {
            btnAcceptInvite.setDisable(false);
            btnRejectInvite.setDisable(false);
        }
    }

    @FXML
    private void sendToChat(final ActionEvent event) {
        log.debug("sendToChat event={}", event);
        // Проверяем введённое сообщение
        final String inputMsg = txtChatAdding.getText().trim();
        txtChatAdding.clear();
        if (inputMsg.isEmpty()) return;

        // Отправляем сообщение на сервер
        if (userProfile != null) {
            final NetworkMessage networkMessage;
            networkMessage = new NetworkMessage(NetworkMessage.Type.ChatSend);
            networkMessage.put(NetworkMessage.CHAT_TEXT, inputMsg);
            sendToServer(networkMessage);
        } else {
            addToChat(YOU_ARE_NOT_LOGIN);
        }
    }

    @FXML
    private void showLogin(final MouseEvent event) {
        log.debug("showLogin event={}", event);
        try {
            // Ленивая загрузка модального окна "Логин"
            if (loginDialogStage == null) {
                loginDialogStage = new Stage();
                loginDialogStage.setTitle(Options.LOGIN_DIALOG_TITLE);
                loginDialogStage.setResizable(false);
                loginDialogStage.setScene(new Scene(fxmlLoginDialog));
                loginDialogStage.initModality(Modality.WINDOW_MODAL);
                loginDialogStage.initOwner(mainStage);
                loginDialogCntrl.setLoginDialogStage(loginDialogStage);
            }

            // Показываем модальное окно
            loginDialogCntrl.showModal();

            // Если пользователь нажал кнопку "Ок"
            if (loginDialogCntrl.isApproved()) {
                final String userName = loginDialogCntrl.getLoginName();
                final String password = loginDialogCntrl.getLoginPassword();

                // Проверяем длину пароля
                if (loginDialogCntrl.isCreateNewAccount()) {
                    if (password.length() < Options.PASSWORD_MIN_LENGTH) {
                        addToChat(PASSWORD_MIN_LENGTH_EXCEPTION);
                        return;
                    }
                }

                // Создаём хеш пароля + добавим логин (логин уникальный для каждого пользователя)
                final String passwordHash = EncryptAlgorithm.getInstance().getHashCodeFromString(
                        EncryptAlgorithm.SHA512_ALGORITHM,
                        password + userName
                );

                // Создаём сообщение логина/создания нового аккаунта
                final NetworkMessage netMsg = new NetworkMessage(
                        loginDialogCntrl.isCreateNewAccount() ?
                                NetworkMessage.Type.CreateUser : NetworkMessage.Type.LoginUser);

                netMsg.put(NetworkMessage.NAME, userName);
                netMsg.put(NetworkMessage.PASSWORD_HASH, passwordHash);
                sendToServer(netMsg);
            }
        } catch (final NoSuchAlgorithmException ex) {
            addToChat(ex.toString());
            log.error(fatal, "Exception:", ex);
        }
    }

    private void inviteUserToPlay(final UserProfile selectedUserProfile) {
        log.debug("inviteUserToPlay selectedUserProfile={}", selectedUserProfile);
        if (userProfile == null) return;

        // Отсылаем сообщение на сервер о приглашении/отмене приглашения
        final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.InviteToPlay);
        netMsg.put(NetworkMessage.INVITE_NAME, selectedUserProfile.getName());
        netMsg.put(
                NetworkMessage.INVITE_TYPE,
                selectedUserProfile.isInvited() ?
                        NetworkMessage.INVITE_TYPE_YES : NetworkMessage.INVITE_TYPE_NO
        );

        sendToServer(netMsg);
    }

    /**
     * Событие нажатия кнопок "Принять приглашение\Отказаться"
     *
     * @param event
     */
    @FXML
    private void responseUserToPlay(final ActionEvent event) {
        log.debug("responseUserToPlay event={}", event);
        if (userProfile == null) return;

        // Ищем нажатую кнопку
        final Object source = event.getSource();
        if (!(source instanceof Button)) return;
        final Button clickedButton = (Button) source;

        final UserProfile selectedUserProfile = (UserProfile) tableInvites.getSelectionModel().getSelectedItem();
        if (selectedUserProfile == null) return;

        // Отсылаем сообщение на сервер о принятии/отказе приглашения
        final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.InviteToPlayResponse);
        netMsg.put(NetworkMessage.INVITE_NAME, selectedUserProfile.getName());
        netMsg.put(
                NetworkMessage.INVITE_TYPE,
                btnAcceptInvite.equals(clickedButton) ?
                        NetworkMessage.INVITE_TYPE_YES : NetworkMessage.INVITE_TYPE_NO
        );
        sendToServer(netMsg);

        // Удаляем пригласившего из списка
        inviters.remove(selectedUserProfile);
    }

    // Посылает сообщение на сервер
    private void sendToServer(final NetworkMessage netMsg) {
        log.trace("sendToServer netMsg={}", netMsg);
        try {
            checkNettyClient();
            nettyClient.sendToServer(netMsg);
        } catch (final InterruptedException ex) {
            addToChat(ex.toString());
        }
    }

    // Проверяет состояние nettyClient
    private void checkNettyClient() {
        log.trace("checkNettyClient");
        try {
            // Ленивая загрузка NettyClient
            if (nettyClient == null) nettyClient = NettyClient.getInstance();

            // Если соединение с сервером пропало - пробуем перезапустить
            if (!nettyClient.isActive()) {
                addToChat(CONNECTING_TO_SERVER);
                nettyClient.stop();
                nettyClient.run();

                if (!nettyClient.isActive()) connectToServerFailed();
            }
        } catch (final InterruptedException ex) {
            log.error("Exception:", ex);
            addToChat(ex.toString());
        } catch (final ConnectException ex) {
            log.error("Exception:", ex);
            connectToServerFailed();
        }
    }

    // Обновляет инфу о пользователе
    private void updateUserInfo() {
        log.trace("updateUserInfo");
        if (userProfile == null) {
            lblLogin.setText(USER_NOT_LOG_IN);
            lblLoginInfo.setText(USER_NOT_LOGIN_DESCRIPTION);
        } else {
            lblLogin.setUnderline(false);
            lblLogin.setTextFill(Color.BLACK);

            lblLogin.setText(userProfile.getName());
            lblLoginInfo.setText(String.format(
                    USER_LOGIN_INFO,
                    userProfile.getRank(),
                    userProfile.getWins(),
                    userProfile.getDefeats(),
                    userProfile.getDraws(),
                    formatSeconds(userProfile.getTotalTimeOnServer())
            ));
        }
    }

    // Вызывается при отсоединении от сервера
    private void connectToServerFailed() {
        log.trace("connectToServerFailed");
        // Выводим в чат сообщение
        addToChat(CONNECT_WITH_SERVER_FAILED);
    }

    private void initGameGraphics() {
        log.trace("initGameGraphics");
        // Инициилизация отрисовки игрового поля
        final Canvas canvas = new Canvas(GAME_CANVAS_WIDTH, CANVAS_HEIGHT);
        gameGC = canvas.getGraphicsContext2D();
        canvas.setOnMouseClicked(this::canvasMouseClicked);
        paneGame.getChildren().add(canvas);

        loadChessImages();

        // Рисуем игру
        paintGame();
    }

    // Ищет игрока в текущей игре. Может вернуть null
    private ChessPlayer findSelfInGame() {
        log.trace("findSelfInGame");
        // Находим игрока
        for (final ChessPlayer gamePlayer : game.getPlayers())
            if (gamePlayer.getName().equals(userProfile.getName())) return gamePlayer;
        return null;
    }

    // Событие клика по канвасу
    private void canvasMouseClicked(final MouseEvent event) {
        log.trace("canvasMouseClicked event={}", event);
        try {
            final int xMouse = (int) event.getX();
            final int yMouse = (int) event.getY();
            final ChessField field = game.getField();
            final ChessPlayer player = findSelfInGame();
            if (player == null) return;

            // Если кликнули по кнопке - вызываем их обработчики событий
            if (xMouse < SURRENDER_BUTTON_X + BUTTON_WIDTH && yMouse < SURRENDER_BUTTON_Y + BUTTON_WIDTH
                    && xMouse > SURRENDER_BUTTON_X && yMouse > SURRENDER_BUTTON_Y) {
                surrenderButtonClicked();
                return;
            } else if (xMouse < DRAW_BUTTON_X + BUTTON_WIDTH && yMouse < DRAW_BUTTON_Y + BUTTON_WIDTH
                    && xMouse > DRAW_BUTTON_X && yMouse > DRAW_BUTTON_Y) {
                drawButtonClicked();
                return;
            }

            // Если кликнули по панели выбора фигуры (пешка прошла до конца доски)
            if (game.isCurrentPlayerChoosingFigure()) {
                // Находим выбранную фигуру
                for (int i = 0; i < CHOOSE_FIGURES_COUNT; i++) {
                    if (xMouse > CHOOSE_FIGURES_X + CELL_SIZE * i &&
                            yMouse > CHOOSE_FIGURES_Y &&
                            xMouse < CHOOSE_FIGURES_X + CELL_SIZE * (i + 1) - COORD_FIGURE_IMAGE_TO_DRAW * 2 &&
                            yMouse < CHOOSE_FIGURES_Y + FIGURE_IMG_SIZE) {
                        ChessFigure.Type figureType = ChessFigure.Type.Queen;
                        if (i == 1) figureType = ChessFigure.Type.Castle;
                        if (i == 2) figureType = ChessFigure.Type.Bishop;
                        if (i == 3) figureType = ChessFigure.Type.Knight;

                        // Формируем сообщение на сервер...
                        final ChessAction chessAction = new ChessAction(ChessAction.Type.ChooseFigureInsteadPawn, figureType);
                        final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.DoAction);
                        netMsg.put(NetworkMessage.GAME_ACTION, chessAction);
                        sendToServer(netMsg);
                        break;
                    }
                }
                return;
            }

            // Проверяем клик мыши в рамках игрового поля
            if (xMouse < CHESSBOARD_X || yMouse < CHESSBOARD_Y
                    || xMouse > CHESSBOARD_X + CHESSBOARD_W || yMouse > CHESSBOARD_Y + CHESSBOARD_H) return;

            // Надо определить клетку по координатам
            final Point clickedCell = canvasCoordinateToFieldCell(xMouse, yMouse);

            /* Обрабатываем нажатие на клетку:
            - если выделения не было - выбираем нажатую клетку как текущую
            - если выделение было - отправляем на сервер сделанный ход
            */
            if (gameCurrentCell == null) {
                if (field.getFigure(clickedCell) == null) return;     // Если клетка пустая - ничего не делаем
                setGameCurrentCell(clickedCell);
            } else {
                // Если кликнули по своей фигуре - выбираем её как выделенную
                final ChessFigure clickedFigure = field.getFigure(clickedCell);
                if (clickedFigure != null && clickedFigure.getColor().equals(player.getColor())) {
                    setGameCurrentCell(clickedCell);
                    return;
                }

                // Формируем объект хода
                final Point[] movePoints = new Point[2];
                movePoints[0] = gameCurrentCell;
                movePoints[1] = clickedCell;
                final ChessAction chessAction = new ChessAction(ChessAction.Type.Move, movePoints);

                // Формируем сообщение на сервер...
                final NetworkMessage netMsg = new NetworkMessage(NetworkMessage.Type.DoAction);
                netMsg.put(NetworkMessage.GAME_ACTION, chessAction);
                sendToServer(netMsg);

                setGameCurrentCell(null);
            }
        } catch (final InvalidPointException ex) {
            log.error(fatal, "Exception while iterate field", ex);
        }
    }

    // Устанавливает текущую клетку
    private void setGameCurrentCell(final Point cell) {
        gameCurrentCell = cell;
        paintGame();
    }

    // Выводит игру на экран
    private void paintGame() {
        log.debug("paintGame");
        if (game == null) return;

        // Очищаем поле
        gameGC.clearRect(0, 0, GAME_CANVAS_WIDTH, CANVAS_HEIGHT);

        // Рисуем шахматную доску
        paintChessBoard();

        // Игровая информация
        paintGeneralInfo();
        paintPlayersInfo();
        paintGameButtons();
    }

    // Рисует кнопки "сдаться", "предложить ничью"
    private void paintGameButtons() {
        // Кнопка "сдаться"
        gameGC.drawImage(imgSurrender,
                SURRENDER_BUTTON_X, SURRENDER_BUTTON_Y,
                BUTTON_WIDTH, BUTTON_WIDTH);
        
        /* Кнопка "предложить ничью"
        Если один из игроков предложил ничью - рисуем другую картинку
        */
        boolean oneOfPlayerOfferedDraw = false;
        for (final ChessPlayer player : game.getPlayers()) {
            if (player.isOfferedDraw()) {
                oneOfPlayerOfferedDraw = true;
                break;
            }
        }

        gameGC.drawImage(oneOfPlayerOfferedDraw ? imgDraw2 : imgDraw1,
                DRAW_BUTTON_X, DRAW_BUTTON_Y,
                BUTTON_WIDTH, BUTTON_WIDTH);
    }

    // Рисует информацию об игроках
    private void paintPlayersInfo() {
        gameGC.setTextBaseline(VPos.TOP);   // Выравнивание текста

        // Белые
        gameGC.setTextAlign(TextAlignment.LEFT);
        paintPlayerInfo(game.getPlayers().get(0), imgKing[0], CHESSBOARD_X - FIGURE_IMG_SIZE - PLAYER_IMAGE_OFFSET_X, CHESSBOARD_X);

        // Чёрные
        gameGC.setTextAlign(TextAlignment.RIGHT);
        paintPlayerInfo(game.getPlayers().get(1), imgKing[1], CHESSBOARD_X + CHESSBOARD_W + PLAYER_IMAGE_OFFSET_X, CHESSBOARD_X + CHESSBOARD_W);
    }

    // Рисует информацию об игроке
    private void paintPlayerInfo(final ChessPlayer player, final Image img, final double imgX,
                                 final double textX) {
        // Рисуем фигуру короля
        gameGC.drawImage(img,
                imgX,
                PLAYER_IMAGE_FIRSTLINE_Y);

        // Рисуем информацию об игроке
        gameGC.fillText(player.getName(),
                textX,
                PLAYER_IMAGE_FIRSTLINE_Y);
        gameGC.fillText(Integer.toString(player.getRank()),
                textX,
                PLAYER_IMAGE_FIRSTLINE_Y + GAME_INFO_DELTA_Y);

        // Если игра закончилась - выводим начисленные очки
        gameGC.setTextAlign(TextAlignment.CENTER);
        if (game.isFinished()) {
            String scores = player.getAccruedScores() < 0 ?
                    Integer.toString(player.getAccruedScores()) :
                    "+" + Integer.toString(player.getAccruedScores());
            gameGC.fillText(scores,
                    imgX + FIGURE_IMG_SIZE / 2,
                    PLAYER_IMAGE_FIRSTLINE_Y + FIGURE_IMG_SIZE + PLAYER_SCORES_OFFSET_Y);
        }
    }

    // Рисует оставшееся игровое время
    private void paintGameTime() {
        if (game == null) return;
        final ChessPlayer currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        // Выравнивание текста
        gameGC.setTextAlign(TextAlignment.CENTER);
        gameGC.setTextBaseline(VPos.TOP);

        // Выводим оставшееся время на ход
        gameGC.setFill(Color.BLACK);
        gameGC.setFont(GAME_TIME_FONT);

        // Подсчитываем время текущего хода
        final Date currentMoveStarted = game.getCurrentMoveStarted();
        if (currentMoveStarted == null) return;
        Long currentMoveTime = null;
        if (!game.isFinished())
            currentMoveTime = Calendar.getInstance().getTime().getTime() - currentMoveStarted.getTime();
        if (game.isFinished() && game.getFinishedGameDate() != null)
            currentMoveTime = game.getFinishedGameDate().getTime() - currentMoveStarted.getTime();
        if (currentMoveTime == null) return;

        // Переводим в удобочитаемый формат
        final long playerLeftTime = currentPlayer.getTimeLeft() - currentMoveTime;
        long seconds = 0;
        long minutes = 0;
        if (playerLeftTime > 0) {
            seconds = playerLeftTime / 1000;
            minutes = seconds / 60;
            seconds -= minutes * 60;
        } else {
            gameGC.setFill(Color.ROSYBROWN);
        }
        final String time = String.format(GAME_TIME_FORMAT_STRING, minutes, seconds);

        // Очищаем поле под время
        final FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
        final float width = fontLoader.computeStringWidth(time, gameGC.getFont());
        final float height = fontLoader.getFontMetrics(gameGC.getFont()).getLineHeight();
        gameGC.clearRect(CHESSBOARD_CENTER_X - width / 2, GAME_INFO_FIRSTLINE_Y, width, height);

        // Выводим оставшееся время
        gameGC.fillText(time, CHESSBOARD_CENTER_X, GAME_INFO_FIRSTLINE_Y);
    }

    // Рисует общую информацию об игре
    private void paintGeneralInfo() {
        final int GAME_CURRENT_MOVE_Y = 43;
        final int GAME_CURRENT_MOVE_Y2 = GAME_CURRENT_MOVE_Y + GAME_INFO_DELTA_Y;

        final ChessPlayer currentPlayer = game.getCurrentPlayer();
        if (currentPlayer == null) return;

        paintGameTime();    // Рисуем оставшееся игровое время

        // Рисуем текущую информацию: кто сейчас ходит, либо результат игры
        gameGC.setFill(Color.BLACK);
        gameGC.setFont(NUMERATION_FONT);
        if (game.isFinished()) {
            // Рисуем результат игры
            if (game.getResult() == null) {
                // Ничья
                gameGC.fillText(DRAW, CHESSBOARD_CENTER_X, GAME_CURRENT_MOVE_Y);
            } else {
                // Выиграл один из игроков
                gameGC.fillText(game.getResult().getColor() == ChessColor.White ? WHITE_WINS : BLACK_WINS,
                        CHESSBOARD_CENTER_X,
                        GAME_CURRENT_MOVE_Y);
            }
            // Подробное описание результата игры (с сервера)
            gameGC.fillText(
                    game.getResultReasonDescription(),
                    CHESSBOARD_CENTER_X,
                    GAME_CURRENT_MOVE_Y2);
        } else {
            // Рисуем кто сейчас ходит
            gameGC.fillText(currentPlayer.getColor() == ChessColor.White ? WHITE_MOVING : BLACK_MOVING,
                    CHESSBOARD_CENTER_X,
                    GAME_CURRENT_MOVE_Y);
        }
    }

    // Рисует шахматную доску
    private void paintChessBoard() {
        log.trace("paintChessBoard");
        // Выводим рамку
        printChessFieldBoard();

        // Для каждой вертикали выводим горизонталь
        for (int i = CELL_MIN; i < CELL_COUNT; i++) paintCellLine(i);

        // Выводим нумерацию полей
        paintNumeration();

        // Выводим обводку выбранной клетки
        paintCurrentCellFrame();

        // Выводим выбор фигуры (если пешка дошла до конца доски)
        paintPaneChooseNewFigure();
    }

    // Выводит все клетки линии на шахматной доске по индексу
    private void paintCellLine(final int i) {
        log.trace("paintCellLine");
        for (int j = CELL_MIN; j < CELL_COUNT; j++) {
            // Задаём цвет для поля
            gameGC.setFill(getColorForCell(i, j));

            // Выводим клетку
            final int x = CHESSBOARD_X + i * CELL_SIZE;
            final int y = CHESSBOARD_Y + j * CELL_SIZE;
            gameGC.fillRect(x, y, CELL_SIZE, CELL_SIZE);

            // Если это был последний сделанный ход - помечаем эту клетку
            if (isItLastMove(i, j)) {
                gameGC.setFill(LAST_MOVE_CELL_COLOR);
                gameGC.fillRect(x, y, CELL_SIZE, CELL_SIZE);
            }

            // Выводим на этой клетке фигуру, если она есть
            try {
                final ChessFigure figure = game.getField().getFigure(
                        canvasCoordinateToFieldCell(x, y));
                if (figure != null) {
                    final Image img = getImage(figure);
                    gameGC.drawImage(img,
                            x + COORD_FIGURE_IMAGE_TO_DRAW,
                            y + COORD_FIGURE_IMAGE_TO_DRAW);
                }
            } catch (final InvalidPointException ex) {
                log.error(fatal, "Exception while iterate field's cells; i={}, j={}", i, j, ex);
            }
        }
    }

    // Выводит нумерацию полей
    private void paintNumeration() {
        // Выравнивание текста
        gameGC.setTextAlign(TextAlignment.CENTER);
        gameGC.setTextBaseline(VPos.CENTER);

        gameGC.setFont(NUMERATION_FONT);
        gameGC.setFill(Color.BLACK);

        for (int j = CELL_MIN; j < CELL_COUNT; j++) {
            // Вертикальные цифры
            gameGC.fillText(
                    Integer.toString(CELL_COUNT - j),
                    CHESSBOARD_X - NUMERATIONS_OFFSET,
                    CHESSBOARD_Y + j * CELL_SIZE + CELL_SIZE / 2);
            // Горизонтальные цифры
            gameGC.fillText(
                    NUMERATION_STR_HORIZONTAL[j],
                    CHESSBOARD_X + j * CELL_SIZE + CELL_SIZE / 2,
                    CHESSBOARD_Y + CHESSBOARD_H + NUMERATIONS_OFFSET);
        }
    }

    // Выводит обводку выбранной клетки
    private void paintCurrentCellFrame() {
        if (gameCurrentCell != null) {
            gameGC.setLineWidth(CURRENT_CELL_LINE_WIDTH);
            gameGC.setStroke(CURRENT_CELL_COLOR);
            gameGC.strokeRect(
                    CHESSBOARD_X + gameCurrentCell.getX() * CELL_SIZE,
                    CHESSBOARD_Y + gameCurrentCell.getY() * CELL_SIZE,
                    CELL_SIZE,
                    CELL_SIZE);
        }
    }

    // Выводит выбор фигуры (если пешка дошла до конца доски)
    private void paintPaneChooseNewFigure() {
        log.trace("paintPaneChooseNewFigure");
        final int figureColorIndex = game.getCurrentPlayer().getColor().equals(ChessColor.White) ? 0 : 1;

        if (!game.isCurrentPlayerChoosingFigure() ||
                !game.getCurrentPlayer().getName().equals(userProfile.getName())) return;

        final Color CHOOSE_PANE_BACK_COLOR = Color.grayRgb(0, 0.7), CHOOSE_PANE_COLOR = LIGHT_CELL_COLOR;
        final int CHOOSE_PANE_OFFSET = 6;
        final int CHOOSE_PANE_X = CHOOSE_FIGURES_X - CHOOSE_PANE_OFFSET + 1,
                CHOOSE_PANE_Y = CHOOSE_FIGURES_Y - CHOOSE_PANE_OFFSET,
                CHOOSE_PANE_W = CELL_SIZE * CHOOSE_FIGURES_COUNT,
                CHOOSE_PANE_H = FIGURE_IMG_SIZE + CHOOSE_PANE_OFFSET * 2 + 1;

        // Оттеняем доску
        gameGC.setFill(CHOOSE_PANE_BACK_COLOR);
        gameGC.fillRect(CHESSBOARD_X, CHESSBOARD_Y, CHESSBOARD_W, CHESSBOARD_H);

        // Рисуем панель
        gameGC.setFill(CHOOSE_PANE_COLOR);
        gameGC.fillRect(CHOOSE_PANE_X, CHOOSE_PANE_Y, CHOOSE_PANE_W, CHOOSE_PANE_H);

        // Рисуем рамку
        gameGC.setLineWidth(CHESSBOARD_BORDER_WIDTH);
        gameGC.setStroke(Color.WHITESMOKE);
        gameGC.strokeRect(CHOOSE_PANE_X, CHOOSE_PANE_Y, CELL_SIZE * CHOOSE_FIGURES_COUNT,
                FIGURE_IMG_SIZE + CHOOSE_PANE_OFFSET * 2 + 1);

        // Выводим текст
        gameGC.setTextAlign(TextAlignment.CENTER);
        gameGC.setTextBaseline(VPos.BOTTOM);
        gameGC.setFill(Color.WHITE);
        gameGC.fillText(CHOOSE_PANE_TEXT, CHESSBOARD_CENTER_X, CHOOSE_FIGURES_Y - CHOOSE_PANE_OFFSET - 2);

        // Рисуем фигуры
        final Image[] imgs = new Image[CHOOSE_FIGURES_COUNT];
        imgs[0] = imgQueen[figureColorIndex];
        imgs[1] = imgCastle[figureColorIndex];
        imgs[2] = imgBishop[figureColorIndex];
        imgs[3] = imgKnight[figureColorIndex];

        for (int i = 0; i < CHOOSE_FIGURES_COUNT; i++) {
            final Image img = imgs[i];
            gameGC.drawImage(img, CHOOSE_FIGURES_X + CELL_SIZE * i, CHOOSE_FIGURES_Y);
        }
    }

    // Задаёт цвет для шахматного поля
    private Color getColorForCell(final int i, final int j) {
        // В "шахматном порядке"
        return ((i + j) % 2 == 0) ? LIGHT_CELL_COLOR : DARK_CELL_COLOR;
    }

    // Возвращает true, если эта игровая клетка - последний сделанный ход
    private boolean isItLastMove(final int x, final int y) {
        if (game.getLastMovePoints() == null) return false;

        for (final Point point : game.getLastMovePoints()) {
            if (x == point.getX() && y == point.getY()) return true;
        }

        return false;
    }

    // Рисует рамку шахматной доски
    private void printChessFieldBoard() {
        gameGC.setLineWidth(CHESSBOARD_BORDER_WIDTH);
        gameGC.setStroke(Color.BLACK);
        gameGC.strokeRect(CHESSBOARD_X, CHESSBOARD_Y, CELL_COUNT * CELL_SIZE, CELL_COUNT * CELL_SIZE);
    }

    // Переводит координату в клетку на игровом поле
    private Point canvasCoordinateToFieldCell(int x, int y) {
        // Координата не должна выходить за игровую ячейку
        if (x == CHESSBOARD_X + CHESSBOARD_W) x--;
        if (y == CHESSBOARD_Y + CHESSBOARD_H) y--;

        // Надо определить клетку по координатам
        return new Point(
                (x - CHESSBOARD_X) / CELL_SIZE,
                (y - CHESSBOARD_Y) / CELL_SIZE
        );
    }

    private Image getImage(final ChessFigure figure) {
        final Image img;
        final int colorIndex = figure.getColor() == ChessColor.White ? 0 : 1;

        switch (figure.getType()) {
            case Pawn:
                img = imgPawn[colorIndex];
                break;
            case Bishop:
                img = imgBishop[colorIndex];
                break;
            case Knight:
                img = imgKnight[colorIndex];
                break;
            case Castle:
                img = imgCastle[colorIndex];
                break;
            case Queen:
                img = imgQueen[colorIndex];
                break;
            case King:
                img = imgKing[colorIndex];
                break;
            default:
                log.error(fatal, "getImage: unknown type; figure={}", figure);
                img = null;
                break;
        }
        return img;
    }

    private void loadChessImages() {
        log.trace("loadChessImages");
        imgPawn[0] = new Image(getClass().getResourceAsStream("/images/chess/wp.png"));
        imgPawn[1] = new Image(getClass().getResourceAsStream("/images/chess/bp.png"));
        imgBishop[0] = new Image(getClass().getResourceAsStream("/images/chess/wb.png"));
        imgBishop[1] = new Image(getClass().getResourceAsStream("/images/chess/bb.png"));
        imgKnight[0] = new Image(getClass().getResourceAsStream("/images/chess/wn.png"));
        imgKnight[1] = new Image(getClass().getResourceAsStream("/images/chess/bn.png"));
        imgCastle[0] = new Image(getClass().getResourceAsStream("/images/chess/wr.png"));
        imgCastle[1] = new Image(getClass().getResourceAsStream("/images/chess/br.png"));
        imgQueen[0] = new Image(getClass().getResourceAsStream("/images/chess/wq.png"));
        imgQueen[1] = new Image(getClass().getResourceAsStream("/images/chess/bq.png"));
        imgKing[0] = new Image(getClass().getResourceAsStream("/images/chess/wk.png"));
        imgKing[1] = new Image(getClass().getResourceAsStream("/images/chess/bk.png"));

        imgSurrender = new Image(getClass().getResourceAsStream("/images/chess/surrender.png"));
        imgDraw1 = new Image(getClass().getResourceAsStream("/images/chess/draw1.png"));
        imgDraw2 = new Image(getClass().getResourceAsStream("/images/chess/draw2.png"));
    }

    // Кликнули по кнопке "сдаться"
    private void surrenderButtonClicked() {
        log.debug("surrenderButtonClicked");
        // Уточняем в диалоговом окне действительно ли игрок хочет сдаться
        final Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle(GAME_ALERT_TITLE);
        confirmAlert.setHeaderText(ACTION_CONFIRM);
        confirmAlert.setContentText(SURRENDER_CONFIRMATION);
        final Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() != ButtonType.OK) return;

        // Формируем сообщение на сервер
        final NetworkMessage netMsg;
        netMsg = new NetworkMessage(NetworkMessage.Type.DoAction);
        netMsg.put(
                NetworkMessage.GAME_ACTION,
                new ChessAction(ChessAction.Type.Surrender));
        sendToServer(netMsg);
    }

    // Кликнули по кнопке "предложить ничью"
    private void drawButtonClicked() {
        log.debug("drawButtonClicked");
        if (userProfile == null) return;

        // Находим себя
        ChessPlayer player = null;
        ChessPlayer opponent = null;
        for (final ChessPlayer anyPlayer : game.getPlayers()) {
            if (userProfile.getName().equals(anyPlayer.getName())) {
                player = anyPlayer;
            } else {
                opponent = anyPlayer;
            }
        }
        if (player == null || opponent == null) return;

        // Если игрок уже предложил ничью - выходим
        if (player.isOfferedDraw()) return;

        // Уточняем в диалоговом окне действительно ли игрок хочет предложить ничью
        final Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle(GAME_ALERT_TITLE);
        confirmAlert.setHeaderText(ACTION_CONFIRM);
        confirmAlert.setContentText(opponent.isOfferedDraw() ? ACCEPT_DRAW_CONFIRMATION : DRAW_CONFIRMATION);
        final Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() != ButtonType.OK) return;

        // Формируем сообщение на сервер
        final NetworkMessage netMsg;
        netMsg = new NetworkMessage(NetworkMessage.Type.DoAction);
        netMsg.put(
                NetworkMessage.GAME_ACTION,
                new ChessAction(ChessAction.Type.OfferedDraw));
        sendToServer(netMsg);

        player.setOfferedDraw(true);
        paintGame();
    }

    // Переводит секунды в формат времени
    private String formatSeconds(long seconds) {
        final String separator = ":";
        final String FORMAT_STRING = "%02d";
        final StringBuilder sb = new StringBuilder();

        final long hours = seconds / 60 / 60;
        sb.append(hours).append(separator);
        seconds -= hours * 60 * 60;

        final long mins = seconds / 60;
        sb.append(String.format(FORMAT_STRING, mins)).append(separator);
        seconds -= mins * 60;

        sb.append(String.format(FORMAT_STRING, seconds));

        return sb.toString();
    }

}
