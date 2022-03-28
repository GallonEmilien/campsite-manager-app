package pt4.flotsblancs.scenes.components;

import java.util.HashMap;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import pt4.flotsblancs.Stats.Period;
import pt4.flotsblancs.database.model.CampGround;

public class InformationCard<T> extends BorderPane {

    private Label infoLabel;
    private Label subtitleLabel;

    public InformationCard(String mainTitle, Color color) {
        this.setPadding(new Insets(20));
        this.setBackground(new Background(new BackgroundFill(color, new CornerRadii(17), null)));
        this.setPrefSize(400D, 90D);

        this.setLeft(createTitlesContainer(mainTitle));

        this.setRight(createStaticInfoContainer());
    }

    private VBox createCampgroundListContainer(HashMap<CampGround, Integer> data, String suffix) {
        var container = new VBox();

        HashMap<CampGround, Integer> dataToDisplay = data.entrySet()
                .stream()
                .limit(3)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);

        dataToDisplay.forEach((k, v) -> {
            var pane = new BorderPane();
            pane.setMinWidth(100);

            var left = new Label("#" + k.getId());
            var right = new Label(v + "   " + suffix);

            left.setFont(new Font(12));
            right.setFont(new Font(12));

            pane.setLeft(left);
            pane.setRight(right);
            container.getChildren().add(pane);
        });

        return container;
    }

    private VBox createStaticInfoContainer() {
        infoLabel = new Label();
        infoLabel.setFont(new Font(20));

        VBox infoBox = new VBox();
        infoBox.setAlignment(Pos.CENTER);
        infoBox.getChildren().addAll(infoLabel);
        return infoBox;
    }

    private VBox createTitlesContainer(String title) {
        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label mainTitleLabel = new Label(title);
        subtitleLabel = new Label();

        mainTitleLabel.setFont(new Font(16));
        subtitleLabel.setFont(new Font(13));
        subtitleLabel.setTextFill(Color.GRAY);

        titleBox.getChildren().addAll(mainTitleLabel, subtitleLabel);
        return titleBox;
    }

    public void setData(T data, Period period) {
        setData(data, "", period);
    }

    public void setData(T data, String suffix, Period period) {
        if (data instanceof HashMap)
            this.setRight(createCampgroundListContainer((HashMap<CampGround, Integer>) data, suffix));
        else
            infoLabel.setText(data + " " + suffix);
        subtitleLabel.setText(period.toString());
    }
}
