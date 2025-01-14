package pt4.flotsblancs.scenes;

import pt4.flotsblancs.database.Database;
import pt4.flotsblancs.database.model.Client;
import pt4.flotsblancs.database.model.ConstraintException;
import pt4.flotsblancs.database.model.Reservation;
import pt4.flotsblancs.router.Router;
import pt4.flotsblancs.router.Router.Routes;
import pt4.flotsblancs.scenes.breakpoints.BreakPointManager;
import pt4.flotsblancs.scenes.breakpoints.HBreakPoint;
import pt4.flotsblancs.scenes.components.HBoxSpacer;
import pt4.flotsblancs.scenes.components.ProblemsListCard;
import pt4.flotsblancs.scenes.components.PromptedTextField;
import pt4.flotsblancs.scenes.components.ReservationCard;
import pt4.flotsblancs.scenes.components.VBoxSpacer;
import pt4.flotsblancs.scenes.items.ItemScene;
import pt4.flotsblancs.scenes.utils.ExceptionHandler;
import pt4.flotsblancs.scenes.utils.ToastType;
import pt4.flotsblancs.utils.DateUtils;

import java.util.List;
import java.sql.SQLException;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.control.Label;

import io.github.palexdev.materialfx.controls.MFXButton;

public class ClientsScene extends ItemScene<Client> {

    private final int INNER_PADDING = 10;
    private final int CONTENT_SPACING = 20;

    private Client client;

    private Label title;

    private PromptedTextField name;
    private PromptedTextField firstName;
    private PromptedTextField adresse;
    private PromptedTextField phone;
    private PromptedTextField preferences;
    private PromptedTextField email;

    private MFXButton saveButton;
    private MFXButton addReservationButton;

    private ChangeListener<? super Object> changeListener = (obs, oldVal, newVal) -> {
        if (oldVal == null || newVal == null || oldVal == newVal)
            return;
        saveButton.setDisable(false);
    };

    @Override
    public String getName() {
        return "Client";
    }

    @Override
    protected String addButtonText() {
        return "Ajouter un client";
    }

    @Override
    protected void onAddButtonClicked() {
        try {
            Router.goToScreen(Routes.CLIENTS, new Client("Jean"));
        } catch (SQLException e) {
            ExceptionHandler.loadIssue(e);
        }
    }

    @Override
    protected Region createContainer(Client client) {
        this.client = client;

        var container = new VBox(10);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(50));

        container.getChildren().add(createHeader());
        container.getChildren().add(new VBoxSpacer());
        container.getChildren().add(createTopSLot());
        container.getChildren().add(new VBoxSpacer());
        container.getChildren().add(createBottomSlot());
        container.getChildren().add(new VBoxSpacer());
        container.getChildren().add(createActionsButtonsSlot());

        refreshPage();

        return container;
    }

    private void refreshPage() {
        title.setText(client.getDisplayName());
        addReservationButton.setDisable(client.getOpenReservation() != null);
    }

    /**
     * @return Header de la page (Numéro de réservations + Label avec dates)
     */
    private BorderPane createHeader() {
        BorderPane container = new BorderPane();

        title = new Label();
        title.setFont(new Font(24));
        title.setTextFill(Color.rgb(51, 59, 97));

        var clientId = new Label("Client #" + client.getId());
        clientId.setFont(new Font(13));
        clientId.setTextFill(Color.DARKGREY);

        container.setLeft(title);
        container.setRight(clientId);
        return container;
    }

    private HBox createTopSLot() {
        HBox container = new HBox();
        container.setPadding(new Insets(INNER_PADDING));
        container.setAlignment(Pos.TOP_CENTER);

        container.getChildren().add(createCardsContainer());
        container.getChildren().add(new HBoxSpacer());
        container.getChildren().add(createNameFirstNameContainer());
        return container;
    }

    private BorderPane createBottomSlot() {
        BorderPane container = new BorderPane();
        container.setPadding(new Insets(INNER_PADDING));

        container.setLeft(createInfosContainer());
        container.setRight(new ProblemsListCard(client));
        return container;
    }

    private VBox createCardsContainer() {
        VBox container = new VBox(CONTENT_SPACING);
        container.setAlignment(Pos.TOP_LEFT);

        var card = new ReservationCard(client.getOpenReservation(), 250);

        var clientSince = new Label("Client depuis : " + DateUtils.toFormattedString(this.client.getCreationDate()));
        clientSince.setFont(new Font(15));
        clientSince.setTextFill(Color.GRAY);

        var nbReservations = new Label("Nombre de réservations : " + client.getReservations().size());
        nbReservations.setFont(new Font(15));
        nbReservations.setTextFill(Color.GRAY);
        container.getChildren().addAll(card, clientSince, nbReservations);
        return container;
    }

    private VBox createNameFirstNameContainer() {
        VBox container = new VBox(CONTENT_SPACING);
        container.setAlignment(Pos.CENTER);

        name = new PromptedTextField(client.getName(), "Nom");
        name.textProperty().addListener(changeListener);
        firstName = new PromptedTextField(client.getFirstName(), "Prénom");
        firstName.textProperty().addListener(changeListener);
        email = new PromptedTextField(client.getEmail(), "E-mail");
        email.textProperty().addListener(changeListener);

        container.getChildren().addAll(name, firstName, email);
        return container;
    }

    private VBox createInfosContainer() {
        VBox container = new VBox(CONTENT_SPACING);
        container.setAlignment(Pos.BASELINE_LEFT);

        boolean isReduced = isReducedSize(BreakPointManager.getCurrentHorizontalBreakPoint());

        phone = new PromptedTextField(client.getPhone(), "Téléphone");
        phone.setMinWidth(isReduced ? 180 : 350);
        phone.textProperty().addListener(changeListener);

        adresse = new PromptedTextField(client.getAddresse(), "Adresse");
        adresse.setMinWidth(isReduced ? 180 : 350);
        adresse.textProperty().addListener(changeListener);

        preferences = new PromptedTextField(client.getPreferences(), "Préférences");
        preferences.setMinWidth(isReduced ? 180 : 350);
        preferences.textProperty().addListener(changeListener);

        container.getChildren().addAll(phone, adresse, preferences);
        return container;
    }

    private HBox createActionsButtonsSlot() {
        var container = new HBox(10);

        saveButton = new MFXButton("Sauvegarder");
        saveButton.getStyleClass().add("action-button");
        saveButton.setDisable(true);

        addReservationButton = new MFXButton("Créer une réservation");
        addReservationButton.getStyleClass().add("action-button");

        container.setAlignment(Pos.CENTER_RIGHT);
        container.getChildren().addAll(saveButton, addReservationButton);

        saveButton.setOnAction(e -> {
            updateDatabase(client);
            refreshPage();
            saveButton.setDisable(true);
        });

        addReservationButton.setOnAction(e -> {
            try {
                Router.goToScreen(Routes.RESERVATIONS, new Reservation(client));
                Router.showToast(ToastType.SUCCESS, "Réservation ajoutée");
            } catch (ConstraintException e1) {
                // Si il y a eu un soucis sur les contraintes de la réservation, on l'indique à
                // l'utilisateur
                Router.showToast(ToastType.WARNING, e1.getMessage());
                e1.printStackTrace();
            } catch (SQLException e1) {
                ExceptionHandler.loadIssue(e1);
            }
        });

        return container;
    }

    private void updateDatabase(Client client) {
        if (client == null)
            return;
        boolean update = false;
        try {
            if (!client.getFirstName().equals(firstName.getTextSafely())) {
                client.setFirstName(firstName.getTextSafely());
                update = true;
            }
            if (!client.getName().equals(name.getTextSafely())) {
                client.setName(name.getTextSafely());
                update = true;
            }

            if (!client.getAddresse().equals(adresse.getTextSafely())) {
                try {
                    client.setAddresse(adresse.getTextSafely());
                    update = true;
                } catch (ConstraintException e) {
                    adresse.setText(client.getAddresse());
                    Router.showToast(ToastType.ERROR, e.getMessage());
                }
            }

            if (!client.getPhone().equals(phone.getTextSafely())) {
                try {
                    client.setPhone(phone.getTextSafely());
                    update = true;
                } catch (ConstraintException e) {
                    phone.setText(client.getPhone());
                    Router.showToast(ToastType.ERROR, e.getMessage());
                }
            }

            if (!client.getEmail().equals(email.getTextSafely())) {
                try {
                    client.setEmail(email.getTextSafely());
                    update = true;
                } catch (ConstraintException e) {
                    email.setText(client.getEmail());
                    Router.showToast(ToastType.ERROR, e.getMessage());
                }
            }

            if (!preferences.getTextSafely().equals(client.getPreferences())) {
                client.setPreferences(preferences.getTextSafely());
                update = true;
            }

            Database.getInstance().getClientsDao().update(client);
            if (update) {
                Router.showToast(ToastType.SUCCESS, "Client mis à jour");
                updateItemList(client);
            }
        } catch (SQLException e) {
            ExceptionHandler.loadIssue(e);
        }
    }

    @Override
    public void onUnfocus() {
        onContainerUnfocus();
    }

    @Override
    public void onContainerUnfocus() {
        if (this.saveButton != null)
            if (!saveButton.isDisabled())
                updateDatabase(client);
    }

    @Override
    protected List<Client> queryAll() throws SQLException {
        return Database.getInstance().getClientsDao().queryBuilder().orderBy("name", true).query();
    }

    private boolean isReducedSize(HBreakPoint currentBp) {
        return currentBp.getWidth() <= HBreakPoint.LARGE.getWidth();
    }

    @Override
    public void onHorizontalBreak(HBreakPoint oldBp, HBreakPoint newBp) {
        super.onHorizontalBreak(oldBp, newBp); // Implémentation de ItemScene

        if (adresse == null || preferences == null)
            return;

        if (isReducedSize(newBp)) {
            adresse.setMinWidth(180);
            preferences.setMinWidth(180);
        } else {
            adresse.setMinWidth(350);
            preferences.setMinWidth(350);
        }
    }
}
