package pt4.flotsblancs.scenes;

import pt4.flotsblancs.database.Database;
import pt4.flotsblancs.database.model.Reservation;
import pt4.flotsblancs.database.model.types.CashBack;
import pt4.flotsblancs.database.model.types.Equipment;
import pt4.flotsblancs.router.Router;
import pt4.flotsblancs.router.Router.Routes;
import pt4.flotsblancs.scenes.breakpoints.BreakPointManager;
import pt4.flotsblancs.scenes.breakpoints.HBreakPoint;
import pt4.flotsblancs.scenes.components.CampgroundCard;
import pt4.flotsblancs.scenes.components.ClientCard;
import pt4.flotsblancs.scenes.components.ConfirmButton;
import pt4.flotsblancs.scenes.components.HBoxSpacer;
import pt4.flotsblancs.scenes.components.ProblemsListCard;
import pt4.flotsblancs.scenes.components.ReservationDatePicker;
import pt4.flotsblancs.scenes.components.VBoxSpacer;
import pt4.flotsblancs.scenes.components.ComboBoxes.*;
import pt4.flotsblancs.scenes.items.ItemScene;
import pt4.flotsblancs.scenes.utils.ExceptionHandler;
import pt4.flotsblancs.scenes.utils.PriceUtils;
import pt4.flotsblancs.scenes.utils.ToastType;
import pt4.flotsblancs.utils.MailSender;
import pt4.flotsblancs.utils.PDFGenerator;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.text.Font;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.OverrunStyle;

import org.kordamp.ikonli.javafx.FontIcon;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.enums.FloatMode;



public class ReservationsScene extends ItemScene<Reservation> {

    private final int INNER_PADDING = 10;
    private final int CONTENT_SPACING = 20;

    private Reservation reservation;

    private Label title;
    private Label dayCount;

    private HBox topSlot;
    private VBox cardsContainer;
    private CampgroundCard campCard;
    private HBoxSpacer topSlotFirstSpacer;

    private HBox bottomSlot;

    private Label depositPrice;
    private Label toPayPrice;
    private Label totalPrice;

    private Label datesLabel;

    private MFXComboBox<String> depositComboBox;
    private MFXComboBox<String> paymentComboBox;
    private MFXComboBox<CashBack> cashBackComboBox;

    private ReservationDatePicker startDatePicker;
    private ReservationDatePicker endDatePicker;

    private CampGroundComboBox campComboBox;
    private ServiceComboBox serviceComboBox;
    private PersonCountComboBox personCountComboBox;
    private EquipmentComboBox equipmentsComboBox;

    private MFXButton openBillBtn;
    private MFXButton sendBillBtn;
    private ConfirmButton cancelBtn;

    private ProblemsListCard problemsContainer;

    private ChangeListener<? super Object> changeListener = (obs, oldValue, newValue) -> {
        // Check si on a vraiment besoin de refresh la page et la bd
        if (oldValue == newValue || oldValue == null)
            return;
        refreshPage();
        updateDatabase();
    };

    @Override
    public String getName() {
        return "Réservations";
    }

    @Override
    protected String addButtonText() {
        return null; // Dire explicitement de ne pas afficher le bouton d'ajout de l'ItemList pour
                     // cette page
    }

    @Override
    protected void onAddButtonClicked() {
        Router.showToast(ToastType.INFO, "Selectionner un client");
        Router.goToScreen(Routes.CLIENTS);
    }

    @Override
    protected Region createContainer(Reservation reservation) {
        this.reservation = reservation;

        VBox container = new VBox();
        container.setPadding(new Insets(50));

        topSlot = createTopSlot();
        bottomSlot = createBottomSlot();

        container.getChildren().add(createHeader());
        container.getChildren().add(new VBoxSpacer());
        container.getChildren().add(topSlot);
        container.getChildren().add(new VBoxSpacer());
        container.getChildren().add(bottomSlot);
        container.getChildren().add(new VBoxSpacer());
        container.getChildren().add(createActionsButtonsSlot());

        refreshPage();
        return container;
    }

    private void refreshPage() {
        equipmentsComboBox.refresh();
        campComboBox.refresh();
        serviceComboBox.refresh();
        cashBackComboBox.selectItem(reservation.getCashBack());


        // Rafraichit tous les labels de la page ayant une valeur calculé
        String cancelStr = reservation.getCanceled() ? " (Annulée)" : "";
        title.setText("Réservation  #" + reservation.getId() + cancelStr);

        dayCount.setText(reservation.getDayCount() + " jours");
        depositPrice.setText(
                "Prix acompte : " + PriceUtils.priceToString(reservation.getDepositPrice()) + "€");
        toPayPrice.setText(
                "Reste à payer : " + PriceUtils.priceToString(reservation.getToPayPrice()) + "€");
        totalPrice.setText(
                "Prix total : " + PriceUtils.priceToString(reservation.getTotalPrice()) + "€");
        sendBillBtn.setText(reservation.getBill() != null ? "Regénérer et envoyer facture"
                : "Générer et envoyer facture");
        campCard.refresh(reservation.getCampground());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E dd MMM", Locale.FRANCE);
        String startStr = simpleDateFormat.format(reservation.getStartDate());
        String endStr = simpleDateFormat.format(reservation.getEndDate());
        datesLabel.setText(startStr + " - " + endStr);

        // Active / Desactive les contrôle en fonction de l'état de la réservation
        boolean isDeposited = reservation.getDepositDate() != null;
        boolean isPaid = reservation.getPaymentDate() != null;
        boolean isCanceled = reservation.getCanceled();
        Equipment campEquipments = reservation.getCampground().getAllowedEquipments();
        boolean isMobilhome = campEquipments == Equipment.MOBILHOME;
        boolean isSingleEquipment =
                reservation.getCampground().getCompatiblesEquipments().size() == 1;
        boolean isSingleService = reservation.getCampground().getCompatiblesServices().size() == 1;
        startDatePicker.setDisable(isDeposited || isPaid || isCanceled);
        endDatePicker.setDisable(isDeposited || isPaid || isCanceled);
        campComboBox.setDisable(isDeposited || isPaid || isCanceled);
        serviceComboBox
                .setDisable(isDeposited || isPaid || isMobilhome || isSingleService || isCanceled);
        personCountComboBox.setDisable(isDeposited || isPaid || isCanceled);
        equipmentsComboBox.setDisable(
                isDeposited || isPaid || isMobilhome || isSingleEquipment || isCanceled);
        cashBackComboBox.setDisable(isPaid || isCanceled);
        depositComboBox.setDisable(isPaid || isCanceled);
        paymentComboBox.setDisable(!isDeposited || isCanceled);
        sendBillBtn.setDisable(!isPaid || isCanceled);
        cancelBtn.setDisable(isPaid || isCanceled);
        openBillBtn.setVisible(reservation.getBill() != null);
    }

    private void updateDatabase() {
        try {
            Database.getInstance().getReservationDao().update(reservation);
            Router.showToast(ToastType.SUCCESS, "Réservation mise à jour");
            updateItemList(reservation);
        } catch (SQLException e) {
            ExceptionHandler.updateIssue(e);
        }
    }

    /**
     * @return Conteneur avec les cartes, les equipements et services, les sélections de dates
     */
    private HBox createTopSlot() {
        HBox container = new HBox(10);
        container.setPadding(new Insets(INNER_PADDING));

        cardsContainer = cardsContainer();
        var gear = selectedEquipmentAndServicesContainer();
        var dates = datesContainer();

        topSlotFirstSpacer = new HBoxSpacer();

        if (!isReducedSize(BreakPointManager.getCurrentHorizontalBreakPoint())) {
            container.getChildren().add(cardsContainer);
            container.getChildren().add(topSlotFirstSpacer);
        }
        container.getChildren().add(gear);
        container.getChildren().add(new HBoxSpacer());
        container.getChildren().add(dates);

        return container;
    }

    /**
     * @return Conteneur avec problèmes et contrôles de paiement
     */
    private HBox createBottomSlot() {
        HBox container = new HBox(10);
        container.setPadding(new Insets(INNER_PADDING));
        container.setAlignment(Pos.BOTTOM_CENTER);

        problemsContainer = new ProblemsListCard(reservation);

        container.getChildren().add(createPaymentContainer());
        container.getChildren().add(new HBoxSpacer());
        if (!isReducedSize(BreakPointManager.getCurrentHorizontalBreakPoint()))
            container.getChildren().add(problemsContainer);
        return container;
    }

    private HBox createPaymentContainer() {
        HBox container = new HBox(CONTENT_SPACING);

        VBox btnContainer = new VBox(CONTENT_SPACING);
        depositComboBox = createPriceComboBox("Acompte");
        depositComboBox.selectIndex(reservation.getDepositDate() == null ? 1 : 0);
        createDepositListener(depositComboBox);

        paymentComboBox = createPriceComboBox("Règlement complet");
        paymentComboBox.selectIndex(reservation.getPaymentDate() == null ? 1 : 0);
        createPayementListener(paymentComboBox);

        cashBackComboBox = createCashbackComboBox();
        createCashbackListener(cashBackComboBox);

        btnContainer.getChildren().addAll(depositComboBox, paymentComboBox, cashBackComboBox);

        VBox labelsContainer = new VBox(CONTENT_SPACING);
        depositPrice = new Label();
        depositPrice.setMinWidth(110);
        depositPrice.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);

        totalPrice = new Label();
        totalPrice.setMinWidth(110);
        totalPrice.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);

        toPayPrice = new Label();
        toPayPrice.setMinWidth(110);
        toPayPrice.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);

        labelsContainer.getChildren().addAll(depositPrice, toPayPrice, totalPrice);

        container.getChildren().addAll(btnContainer, labelsContainer);
        return container;
    }

    /**
     * @return VBox contenant la carte du client et la carte de l'emplacement associés à cette
     *         réservation
     */
    private VBox cardsContainer() {
        VBox container = new VBox(CONTENT_SPACING);
        container.setPadding(new Insets(10, 0, 0, 0));
        container.setMinWidth(220);
        var clientCard = new ClientCard(reservation.getClient(), 220);
        campCard = new CampgroundCard(reservation.getCampground(), 220);
        container.getChildren().addAll(clientCard, campCard);
        return container;
    }

    /**
     * @return Conteneur contenant les ComboBox des dates de début de fin de la réservation
     */
    private VBox datesContainer() {
        VBox container = new VBox(CONTENT_SPACING);

        startDatePicker = new ReservationDatePicker(reservation, true);
        startDatePicker.addUserChangedValueListener(changeListener);

        endDatePicker = new ReservationDatePicker(reservation, false);
        endDatePicker.addUserChangedValueListener(changeListener);

        container.getChildren().addAll(startDatePicker, endDatePicker);

        try {
            campComboBox = new CampGroundComboBox(reservation);
            campComboBox.addUserChangedValueListener(changeListener);
            container.getChildren().add(campComboBox);
        } catch (SQLException e) {
            ExceptionHandler.loadIssue(e);
        }
        return container;
    }

    /**
     * @return conteneur contenant les ComboBox de sélection de l'equipement / services / nb
     *         personnes
     */
    private VBox selectedEquipmentAndServicesContainer() {
        VBox container = new VBox(CONTENT_SPACING);

        personCountComboBox = new PersonCountComboBox(reservation);
        personCountComboBox.addListener(changeListener);

        serviceComboBox = new ServiceComboBox(reservation);
        serviceComboBox.addUserChangedValueListener(changeListener);

        equipmentsComboBox = new EquipmentComboBox(reservation);
        equipmentsComboBox.addUserChangedValueListener(changeListener);

        container.getChildren().addAll(personCountComboBox, serviceComboBox, equipmentsComboBox);
        return container;
    }

    /**
     * @return Header de la page (Numéro de réservations + Label avec dates)
     */
    private BorderPane createHeader() {
        BorderPane container = new BorderPane();

        title = new Label();
        title.setFont(new Font(24));
        title.setTextFill(Color.rgb(51, 59, 97));

        VBox datesInfosContainer = new VBox(5);
        datesLabel = new Label();
        datesLabel.setFont(new Font(17));
        datesLabel.setTextFill(Color.GREY);

        dayCount = new Label();
        dayCount.setFont(new Font(13));
        dayCount.setTextFill(Color.DARKGREY);

        datesInfosContainer.getChildren().addAll(datesLabel, dayCount);

        container.setLeft(title);
        container.setRight(datesInfosContainer);
        return container;
    }

    /**
     * @return Boutons d'actions en bas de la page
     */
    private HBox createActionsButtonsSlot() {
        var container = new HBox(10);

        openBillBtn = new MFXButton("Voir facture");
        sendBillBtn = new MFXButton();
        cancelBtn = new ConfirmButton("Annuler la réservation");

        FontIcon icon = new FontIcon("fas-exclamation-triangle:10");
        icon.setIconColor(Color.WHITE);
        cancelBtn.setGraphic(icon);
        cancelBtn.setOnConfirmedAction(e -> {
            reservation.setCanceled(true);
            refreshPage();
            updateDatabase();
        });

        sendBillBtn.setOnAction(e -> {
            new Thread() {
                public void run() {
                    try {
                        var bill = PDFGenerator.generateReservationBillPDF(reservation);
                        reservation.setBill(bill.toByteArray());
                        Platform.runLater(new Runnable() {
                            public void run() {
                                updateDatabase();
                                refreshPage();
                            }
                        });
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        Platform.runLater(new Runnable() {
                            public void run() {
                                Router.showToast(ToastType.ERROR,
                                        "Une erreur est survenue durant la génération de la facture");
                            }
                        });
                    }

                    try {
                        MailSender.sendMail(reservation);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        Platform.runLater(new Runnable() {
                            public void run() {
                                Router.showToast(ToastType.ERROR,
                                        "Une erreur est survenue durant l'envoi de l'email au client");
                            }
                        });
                    }
                }
            }.start();

        });

        openBillBtn.setOnAction(e -> {
            Router.showToast(ToastType.INFO, "Ouverture du fichier...");
            try {
                PDFGenerator.openFile(reservation);

            } catch (Exception e1) {
                e1.printStackTrace();
                Router.showToast(ToastType.ERROR,
                        "Une erreur est survenue durant l'ouverture du fichier");
            }
        });

        sendBillBtn.getStyleClass().add("action-button");
        cancelBtn.getStyleClass().add("action-button");
        openBillBtn.getStyleClass().add("action-button");

        container.setAlignment(Pos.CENTER_RIGHT);
        container.getChildren().addAll(openBillBtn, sendBillBtn, cancelBtn);
        return container;
    }


    /**
     * @param typeName
     * @return ComboBox pour les état de paiement (Versé / Attente)
     */
    private MFXComboBox<String> createPriceComboBox(String typeName) {
        var combo = new MFXComboBox<String>();
        combo.setFloatingText(typeName);
        combo.setFloatMode(FloatMode.INLINE);
        combo.getItems().addAll("Versé", "En attente");
        combo.setMinWidth(180);
        combo.setAnimated(false);

        return combo;
    }

    /**
     * @param typeName
     * @return ComboBox pour choisir une remise
     */
    private MFXComboBox<CashBack> createCashbackComboBox() {
        var combo = new MFXComboBox<CashBack>();
        combo.setFloatingText("Remise");
        combo.setFloatMode(FloatMode.INLINE);
        combo.getItems().addAll(CashBack.values());
        combo.selectIndex(0);
        combo.setMinWidth(180);
        combo.setAnimated(false);
        return combo;
    }

    /**
     * Listener de la ComboBox de l'acompte
     * 
     * Lance la mise à jour de l'interface et de la BD si la valeur change
     * 
     * @param comboBox
     */
    private void createDepositListener(MFXComboBox<String> comboBox) {
        comboBox.valueProperty().addListener((obs, oldPrice, newPrice) -> {
            if (oldPrice == null)
                return;
            if (newPrice.equals("Versé"))
                reservation.setDepositDate(new Date());
            else
                reservation.setDepositDate(null);
            refreshPage();
            updateDatabase();
        });
    }

    /**
     * Listener de la ComboBox de paiement
     * 
     * Lance la mise à jour de l'interface et de la BD si la valeur change
     * 
     * @param comboBox
     */
    private void createPayementListener(MFXComboBox<String> comboBox) {
        comboBox.valueProperty().addListener((obs, oldPrice, newPrice) -> {
            if (oldPrice == null)
                return;
            if (newPrice.equals("Versé"))
                reservation.setPaymentDate(new Date());
            else
                reservation.setPaymentDate(null);
            refreshPage();
            updateDatabase();
        });
    }

    /**
     * Listener de la ComboBox de paiement
     * 
     * Lance la mise à jour de l'interface et de la BD si la valeur change
     * 
     * @param comboBox
     */
    private void createCashbackListener(MFXComboBox<CashBack> comboBox) {
        comboBox.valueProperty().addListener((obs, oldPrice, newPrice) -> {
            if (oldPrice == null)
                return;
            reservation.setCashBack(newPrice);
            refreshPage();
            updateDatabase();
        });
    }

    @Override
    protected List<Reservation> queryAll() throws SQLException {
        return Database.getInstance().getReservationDao().queryForAll().stream()
                .filter(r -> r.getClient() != null).collect(Collectors.toList());
    }

    private boolean isReducedSize(HBreakPoint currentBp) {
        return currentBp.getWidth() <= HBreakPoint.MEDIUM.getWidth();
    }

    @Override
    public void onHorizontalBreak(HBreakPoint oldBp, HBreakPoint newBp) {
        super.onHorizontalBreak(oldBp, newBp); // Implémentation de ItemScene

        if (cardsContainer == null || topSlot == null || problemsContainer == null)
            return;

        if (isReducedSize(newBp)) {
            topSlot.getChildren().remove(cardsContainer);
            topSlot.getChildren().remove(topSlotFirstSpacer);
            bottomSlot.getChildren().remove(problemsContainer);
        } else {
            if (!topSlot.getChildren().contains(cardsContainer))
                topSlot.getChildren().add(0, cardsContainer);
            if (!topSlot.getChildren().contains(topSlotFirstSpacer))
                topSlot.getChildren().add(1, topSlotFirstSpacer);
            if (!bottomSlot.getChildren().contains(problemsContainer))
                bottomSlot.getChildren().add(problemsContainer);
        }
    }

    @Override
    public void onUnfocus() {
        onContainerUnfocus();
    }
}
