package pt4.flotsblancs.scenes;

import pt4.flotsblancs.database.model.User;
import pt4.flotsblancs.router.IScene;
import pt4.flotsblancs.router.Router;
import pt4.flotsblancs.router.Router.Routes;
import pt4.flotsblancs.scenes.components.FlotsBlancsLogo;
import pt4.flotsblancs.scenes.utils.ToastType;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.controls.MFXButton;

public class LoginScene extends BorderPane implements IScene {

    TextField tFId;
    PasswordField pFMdp;

    @Override
    public String getName() {
        return "Connexion";
    }

    @Override
    public boolean showNavBar() {
        return false;
    }

    @Override
    public void onFocus() {
        // On clear les input de login et mdp a l'ouverture de la page
        tFId.setText("");
        pFMdp.setText("");
    }

    @Override
    public void start() {
        setId("login-page");

        setPadding(new Insets(60));
        setTop(new FlotsBlancsLogo(true, true, 100));
        setCenter(loginForm());
    }

    private VBox loginForm() {
        VBox loginForm = new VBox();

        loginForm.setAlignment(Pos.CENTER);
        loginForm.setSpacing(30);

        Label label = new Label("Se connecter");

        tFId = new TextField();
        tFId.setPromptText("Identifiant");
        tFId.setMaxWidth(200);

        pFMdp = new PasswordField();
        pFMdp.setPromptText("Mot de passe");
        pFMdp.setMaxWidth(200);

        MFXButton bValider = new MFXButton("Valider", 150, 30);
        bValider.setButtonType(ButtonType.RAISED);

        bValider.setOnAction(e -> handleLoginAction());
        setOnKeyPressed(e -> handleLoginAction());

        loginForm.getChildren().addAll(label, tFId, pFMdp, bValider);
        return loginForm;
    }

    private void handleLoginAction() {
        if (User.logIn(tFId.getText(), pFMdp.getText())) {
            Router.goToScreen(Routes.HOME);
            Router.showToast(ToastType.SUCCESS,
                    "Connecté.e en tant que " + User.getConnected().toString());
        } else {
            Router.showToast(ToastType.ERROR, "Identifiant ou mot de passe incorrect.");
        }
    }
}
