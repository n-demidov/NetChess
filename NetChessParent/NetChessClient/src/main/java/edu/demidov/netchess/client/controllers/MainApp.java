package edu.demidov.netchess.client.controllers;

import edu.demidov.netchess.client.controllers.network.ServerMessageController;
import edu.demidov.netchess.client.controllers.window.MainWindowController;
import edu.demidov.netchess.client.controllers.window.TimerTick;
import edu.demidov.netchess.client.model.Options;
import java.io.FileNotFoundException;
import java.io.IOException;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application
{
    
    private static final int TIMER_TICK_SECOND = 1000;
    
    private MainWindowController mainAppCntrl;
    private ServerMessageController mainLoopCntrl;
    private Timer timer;
    private final static Logger log = LoggerFactory.getLogger(MainApp.class);
    
    @Override
    public void start(final Stage primaryStage) throws IOException
    {
        log.info("start primaryStage={}", primaryStage);
        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource(Options.MAIN_SCENE_FILE));
        final Parent fxmlMain = fxmlLoader.load();
        mainAppCntrl = fxmlLoader.getController();
        mainAppCntrl.setMainStage(primaryStage);
        
        final Scene scene = new Scene(fxmlMain);
        primaryStage.setTitle(Options.MAIN_WINDOW_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();
        primaryStage.show();
        
        initThreads(mainAppCntrl);
    }
    
    @Override
    public void stop()
    {
        log.info("stop");
        // Останавливаем дополнительные потоки
        mainLoopCntrl.stop();
        timer.stop();
    }
    
    public static void main(final String[] args) throws InterruptedException,
            FileNotFoundException
    {
        launch(args);
        System.exit(0);
    }

    // Инициилизирует дополнительные потоки
    private void initThreads(final MainWindowController mainAppCntrl)
    {
        log.debug("initThreads mainAppCntrl={}", mainAppCntrl);
        // Запускаем обработчик сообщений от сервера
        mainLoopCntrl = new ServerMessageController(mainAppCntrl);
        new Thread(mainLoopCntrl).start();
        
        // Устанаваливаем таймер раз в секунду
        timer = new Timer(TIMER_TICK_SECOND, new TimerTick(mainAppCntrl));
        timer.start();
    }

}
