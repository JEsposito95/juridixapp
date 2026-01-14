package com.juridix;

import com.juridix.db.DatabaseInitializer;
import com.juridix.db.UserBootstrap;
import com.juridix.controller.LoginController;
import com.juridix.ui.views.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        DatabaseInitializer.init();


        LoginController loginController = new LoginController(primaryStage);
        loginController.mostrar();
    }


}
