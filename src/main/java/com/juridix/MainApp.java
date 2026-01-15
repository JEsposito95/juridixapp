package com.juridix;

import com.juridix.controller.LoginController;
import com.juridix.db.Database;
import com.juridix.db.DatabaseInitializer;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Inicializar base de datos
            DatabaseInitializer.init();

            // Verificar conexión
            if (!Database.testConnection()) {
                System.err.println("❌ No se pudo conectar a la base de datos");
                System.exit(1);
            }

            System.out.println("✅ Aplicación iniciada correctamente");

            // Mostrar login
            LoginController loginController = new LoginController(primaryStage);
            loginController.mostrar();

        } catch (Exception e) {
            System.err.println("❌ Error fatal al iniciar la aplicación: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        System.out.println("👋 Cerrando aplicación...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}