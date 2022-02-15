package pt4.flotsblancs.components;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class FlotsBlancsLogo extends VBox {
    public FlotsBlancsLogo(boolean isDark, boolean showTitle, int size) {
        setSpacing(10);
        ImageView icon = new ImageView();

        try {
            FileInputStream stream = new FileInputStream(
                    "src/main/resources/" + (isDark ? "logo_dark.png" : "logo.png"));
            Image img = new Image(stream);
            img.heightProperty();
            img.widthProperty();
            icon.setFitHeight(size);
            icon.setFitWidth(size);
            icon.setImage(img);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        setAlignment(Pos.CENTER);
        setPadding(new Insets(30, 0, 0, 0));

        Label title = new javafx.scene.control.Label("Les Flots Blancs");
        title.setTextFill(isDark ? Color.BLACK : Color.WHITE);
        getChildren().addAll(icon, title);
    }
}
