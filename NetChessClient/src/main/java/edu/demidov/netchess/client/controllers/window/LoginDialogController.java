package edu.demidov.netchess.client.controllers.window;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginDialogController implements Initializable
{
    
    private boolean isApproved;             // Пользователь нажал кнопку "Ок"
    private boolean isCreateNewAccount;     // Пользователь выбрал создать новый аккаунт
    private String loginName;
    private String loginPassword;
    private Stage loginDialogStage;
    private final static Logger log = LoggerFactory.getLogger(LoginDialogController.class);

    @FXML
    private ToggleGroup selectLoginGroup;
    
    @FXML
    private RadioButton radCreateAccount;
    
    @FXML
    private TextField txtLoginName;
    
    @FXML
    private PasswordField txtLoginPswd;
    
    @Override
    public void initialize(final URL url, final ResourceBundle rb) {}
    
    /**
     * Показывает модальное диалоговое окно
     */
    public void showModal()
    {
        log.debug("showModal");
        isApproved = false;
        
        txtLoginName.requestFocus();    // Устанавливаем фокус в поле с логином
        loginDialogStage.showAndWait();
    }
    
    public void setLoginDialogStage(final Stage loginDialogStage)
    {
        this.loginDialogStage = loginDialogStage;
    }
    
    public boolean isApproved()
    {
        return isApproved;
    }

    public boolean isCreateNewAccount()
    {
        return isCreateNewAccount;
    }

    public String getLoginName()
    {
        return loginName;
    }

    public String getLoginPassword()
    {
        return loginPassword;
    }
    
    @FXML
    private void actionOk(final ActionEvent event)
    {
        log.debug("actionOk event={}", event);
        isApproved = true;
        isCreateNewAccount = selectLoginGroup.getSelectedToggle().equals(radCreateAccount);
        loginName = txtLoginName.getText();
        loginPassword = txtLoginPswd.getText();
        
        closeWindow(event);
    }
    
    @FXML
    private void actionCancel(final ActionEvent event)
    {
        log.debug("actionCancel event={}", event);
        isApproved = false;
        closeWindow(event);
    }
    
    // Скрывает текущее окно
    private void closeWindow(final ActionEvent event)
    {
        log.debug("closeWindow event={}", event);
        final Node source = (Node) event.getSource();
        final Stage stage = (Stage) source.getScene().getWindow();
        stage.hide();
    }

}
