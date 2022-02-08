package pt4.flotsblancs;

import pt4.flotsblancs.orm.*;
import pt4.flotsblancs.router.*;
import java.sql.SQLException;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

	public static void main(String[] args) throws SQLException {
		Database.getInstance(); // Initialisation connexion BD
		launch(args); // Lancement JFX
	}

	@Override
	public void start(Stage primaryStage) throws SQLException {
		// Création routeur
		new Router(
			Routes.HOME,
			primaryStage // Route par défaut
		);
		// Affichage de la fenêtre
		primaryStage.show();
	}
}
