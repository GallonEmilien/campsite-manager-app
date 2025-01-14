package pt4.flotsblancs.database.model;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import javafx.scene.paint.Color;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import pt4.flotsblancs.database.Database;
import pt4.flotsblancs.database.model.types.CashBack;
import pt4.flotsblancs.database.model.types.Equipment;
import pt4.flotsblancs.database.model.types.LogType;
import pt4.flotsblancs.database.model.types.Service;
import pt4.flotsblancs.scenes.items.Item;
import pt4.flotsblancs.scenes.utils.StatusColors;
import pt4.flotsblancs.utils.DateUtils;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@DatabaseTable(tableName = "reservations")
public class Reservation implements Item {

    @Getter
    @ToString.Include
    @EqualsAndHashCode.Include
    @DatabaseField(generatedId = true)
    private int id;

    @Getter
    @EqualsAndHashCode.Include
    @DatabaseField(canBeNull = false, columnName = "nb_persons")
    private int nbPersons;

    @Getter
    @EqualsAndHashCode.Include
    @DatabaseField(canBeNull = true, columnName = "cash_back")
    private CashBack cashBack;

    @Getter
    @DatabaseField(columnName = "deposit_date")
    private Date depositDate;

    @Getter
    @DatabaseField(columnName = "payment_date")
    private Date paymentDate;

    @Getter
    @ToString.Include
    @DatabaseField(canBeNull = false, columnName = "start_date")
    private Date startDate;

    @Getter
    @ToString.Include
    @DatabaseField(canBeNull = false, columnName = "end_date")
    private Date endDate;

    @Getter
    @DatabaseField(canBeNull = false, defaultValue = "false")
    private Boolean canceled;

    @Getter
    @DatabaseField(canBeNull = false, defaultValue = "NONE", columnName = "selected_services")
    private Service selectedServices;

    @Getter
    @DatabaseField(canBeNull = false, columnName = "equipments")
    private Equipment equipments;

    @Getter
    @DatabaseField(foreign = true, canBeNull = false, foreignAutoRefresh = true)
    private Client client;

    @Getter
    @DatabaseField(foreign = true, canBeNull = false, foreignAutoRefresh = true)
    private CampGround campground;

    @Getter
    @ForeignCollectionField(eager = false)
    private ForeignCollection<Problem> problems;

    @Getter
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private byte[] bill;

    public Reservation() {
        this.canceled = false;
    }

    /**
     * Permet de créer une réservation dite "vide" à partir d'un client
     * 
     * Des données valides et cohérente seront données à la réservation.
     * 
     * @param client client a assigner à cette réservation
     * @throws SQLException erreur technique de création
     * @throws ConstraintException la création à subit des modfications par effet de bords à cause
     *         des ses contraintes entre équipements / services / emplacement / dates
     */
    public Reservation(Client client) throws SQLException, ConstraintException {
        this.client = client;

        int plusDay = 0;
        List<CampGround> availablesCamps = new ArrayList<CampGround>();

        do {
            // Date par défaut : de aujourd'hui à ajd + 5jours
            this.startDate = DateUtils.plusDays(new Date(), plusDay);
            this.endDate = DateUtils.plusDays(startDate, 5);

            availablesCamps = Database.getInstance().getCampgroundDao()
                    .getAvailablesCampgrounds(startDate, endDate, -1);
            plusDay++;
        } while (availablesCamps.size() == 0);


        this.nbPersons = 1;
        this.cashBack = CashBack.NONE;
        this.campground = availablesCamps.get(0);
        this.equipments = campground.getAllowedEquipments();
        this.selectedServices = campground.getProvidedServices();
        this.canceled = false;

        Database.getInstance().getReservationDao().create(this);
        Database.getInstance().getReservationDao().refresh(this);
        User.addlog(LogType.ADD, "Création de la réservation #" + id);
    }

    /**
     * Change l'emplacement actuel de la réservation tout en respectant les contraintes sur les
     * équipements et les services demandés (Ces derniers peuvent changer par effet de bord)
     * 
     * @param camp nouvel emplacement de la réservation
     * @throws ConstraintException
     * @throws SQLException
     */
    public void setCampground(CampGround camp) throws ConstraintException, SQLException {
        // Gére le cas ou la réservation n'est pas encore bien construite
        if (this.startDate == null || this.endDate == null) {
            this.campground = camp;
        } else {
            if (!Database.getInstance().getCampgroundDao().isAvailableForReservation(this, camp,
                    startDate, endDate)) {
                throw new ConstraintException(
                        "Cet emplacement n'est pas disponibles sur les dates de la réservation",
                        false);
            }
        }

        this.campground = camp;
        User.addlog(LogType.MODIFY, "Emplacement de la réservation #" + id + " changé à "
                + campground.getDisplayName());

        // Gestion des contraintes equipements et services
        // ATTENTION -> changer l'emplacement prend la priorité en terme de contrainte
        // et change
        // donc servies et equipement si ils ne sont pas compatibles
        ConstraintException exceptionHandler = null;

        try {
            checkEquipmentsConstraints();
        } catch (ConstraintException e) {
            // On ne relance pas l'exception tout de suite, il faut avant vérifier les
            // services
            exceptionHandler = e;
        }

        try {
            checkServicesConstraint();
        } catch (Exception e) {
            if (exceptionHandler == null)
                throw e; // Seul les services ont bloqué
            else
                throw new ConstraintException(
                        "Les services et equipement demandés ont été modifiés pour correspondre au nouvel emplacement",
                        true);
        }

        if (exceptionHandler != null) // Seul les equipement ont bloqué
            throw exceptionHandler;
    }

    /**
     * Permet de changer les équipements demandés par la réservation en conservant les contraintes
     * imposées par l'emplacement
     * 
     * @param equipment
     * @throws ConstraintException
     */
    public void setEquipments(Equipment equipment) throws ConstraintException {
        User.addlog(LogType.MODIFY,
                "Equipements de la réservation #" + id + " changés à " + equipment.getName());
        this.equipments = equipment;
        checkEquipmentsConstraints();
    }

    /**
     * Permet de changer les services demandés par la réservation en conservant les contraintes
     * imposées par l'emplacement
     * 
     * @param service
     * @throws ConstraintException
     */
    public void setSelectedServices(Service service) throws ConstraintException {
        User.addlog(LogType.MODIFY,
                "Services de la réservation #" + id + " changés à " + service.getName());
        this.selectedServices = service;
        checkServicesConstraint();
    }

    /**
     * Vérifie l'intégrité des contrainte entre les equipement demandés par la réservation et son
     * emplacement
     * 
     * En cas de non compatibilité l'équipement sera modifié pour répondre à la contrainte
     * 
     * @throws ConstraintException
     */
    private void checkEquipmentsConstraints() throws ConstraintException {
        // Gére le cas ou la réservation n'est pas encore bien construite
        if (this.equipments == null || campground == null)
            return;
        if (!equipments.isCompatibleWithCampEquipment(campground.getAllowedEquipments())) {
            equipments = campground.getAllowedEquipments();
            User.addlog(LogType.MODIFY,
                    "Equipements de la réservation #" + id + " changés à " + equipments.getName());
            throw new ConstraintException(
                    "Equipements de la réservation modifiés pour correspondre à l'emplacement selectionné",
                    true);
        }
    }

    /**
     * Vérifie l'intégrité des contrainte entre les services demandés par la réservation et son
     * emplacement
     * 
     * En cas de non compatibilité le service sera modifié pour répondre à la contrainte
     * 
     * @throws ConstraintException
     */
    private void checkServicesConstraint() throws ConstraintException {
        // Gére le cas ou la réservation n'est pas encore bien construite
        if (this.selectedServices == null || campground == null)
            return;

        String err =
                "Services modifiés pour correspondre aux services proposés par l'emplacement selectionné";
        // Cas ou l'emplacement est un mobilhome, on force eau et électricité
        if (campground.getAllowedEquipments() == Equipment.MOBILHOME
                && selectedServices != Service.WATER_AND_ELECTRICITY) {
            selectedServices = Service.WATER_AND_ELECTRICITY;
            User.addlog(LogType.MODIFY, "Services de la réservation #" + id + " changés à "
                    + selectedServices.getName());
            throw new ConstraintException(err, true);
        }

        // Cas ou le service sélectionné n'est pas disponible sur l'emplacement
        if (!selectedServices.isCompatibleWithCampService(campground.getProvidedServices())) {
            selectedServices = campground.getProvidedServices();
            User.addlog(LogType.MODIFY, "Services de la réservation #" + id + " changés à "
                    + selectedServices.getName());
            throw new ConstraintException(err, true);
        }
    }

    /**
     * @param newStartDate
     * @throws ConstraintException
     * @throws SQLException
     */

    /**
     * Vérifie si les nouvelles dates de début de séjour sélectionnées sont valides l'action est
     * loggé
     * 
     * @param newStartDate
     * @throws ConstraintException
     * @throws SQLException
     */

    public void setStartDate(Date newStartDate) throws ConstraintException, SQLException {
        // Gére le cas ou la réservation n'est pas encore bien construite
        if (this.endDate == null || this.campground == null) {
            this.startDate = newStartDate;
            return;
        }

        var campDao = Database.getInstance().getCampgroundDao();
        if (!campDao.isAvailableForReservation(this, this.campground, newStartDate, endDate)) {
            throw new ConstraintException(
                    "L'emplacement sélectionné n'est pas disponibles avec cette date de début",
                    false);
        }

        if (DateUtils.isInPast(newStartDate)) {
            throw new ConstraintException(
                    "La date de début sélectionnée est antérieure à la date actuelle", false);
        }

        if (DateUtils.isAfter(newStartDate, endDate)) {
            throw new ConstraintException(
                    "La date de début sélectionnée est ultérieure à la date de fin", false);
        }
        User.addlog(LogType.MODIFY,
                "Date de début de la réservation #" + id + " changé à " + newStartDate);
        this.startDate = newStartDate;
    }

    /**
     * Vérifie si les nouvelles dates de fin de séjour sélectionnées sont valides l'action est loggé
     * 
     * @param newEndDate
     * @throws ConstraintException
     * @throws SQLException
     */

    public void setEndDate(Date newEndDate) throws ConstraintException, SQLException {
        // Gére le cas ou la réservation n'est pas encore bien construite
        if (this.startDate == null || this.campground == null) {
            this.endDate = newEndDate;
            return;
        }

        var campDao = Database.getInstance().getCampgroundDao();
        if (!campDao.isAvailableForReservation(this, this.campground, startDate, newEndDate)) {
            throw new ConstraintException(
                    "L'emplacement sélectionné n'est pas disponibles avec cette date de fin",
                    false);
        }

        if (DateUtils.toLocale(newEndDate).isBefore(DateUtils.toLocale(startDate))) {
            throw new ConstraintException(
                    "La date de fin sélectionnée est antérieure à la date de début de la réservation.",
                    false);
        }
        User.addlog(LogType.MODIFY,
                "Date de fin de la réservation #" + id + " changé à " + newEndDate);
        this.endDate = newEndDate;
    }

    /**
     * @return Nombres de jours de la réservation
     */
    public int getDayCount() {
        long diff = endDate.getTime() - startDate.getTime();
        return (int) Math.ceil(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)) + 1;
    }

    /**
     * @return Prix total de la réservation
     */
    public int getTotalPrice() {
        var dayCount = getDayCount();
        int rawPrice = campground.getPricePerDays() * nbPersons * dayCount;

        var withService = rawPrice + selectedServices.getPricePerDay() * dayCount;

        var i = (int) Math.floor(withService * cashBack.getReduction());
        return i;

    }

    /**
     * @return Prix d'acompte de la réservation
     */
    public int getDepositPrice() {
        var i = (int) Math.floor((getTotalPrice() * 0.3f) * cashBack.getReduction());// Acompte de
                                                                                     // 30%
        return i;
    }

    /**
     * @return Prix de la réservation restant a payer
     */
    public int getToPayPrice() {
        if (getDepositDate() == null)
            return getTotalPrice();
        if (getPaymentDate() == null)
            return getTotalPrice() - getDepositPrice();
        return 0;
    }

    /**
     * @return vrai si cette réservation est dans le passé (Soit sa date de fin est passée)
     */
    public boolean isInPast() {
        return new Date().compareTo(this.getEndDate()) >= 0;
    }

    @Override
    public String getSearchString() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        return String
                .join(";", "" + this.id, formatter.format(this.startDate),
                        this.client.getFirstName(), this.client.getName(), this.client.getPhone())
                .trim().toLowerCase();
    }

    @Override
    public String getDisplayName() {
        if (startDate == null || endDate == null || client == null)
            return "Reservation " + getId();

        SimpleDateFormat format = new SimpleDateFormat("dd/MM");
        var prefix = canceled ? "[Annulée] " : "";
        return prefix + format.format(startDate) + "-" + format.format(endDate) + " "
                + client.getName();
    }

    /**
     * l'action est loggé
     * 
     * @param nbP
     */
    public void setNbPersons(int nbP) {
        User.addlog(LogType.MODIFY,
                "Nombre de personnes de la réservation #" + id + " changé pour " + nbP);
        this.nbPersons = nbP;
    }

    /**
     * l'action est loggé
     * 
     * @param date
     */

    public void setPaymentDate(Date date) {
        if (date == null) {
            User.addlog(LogType.DELETE, "Paiement annulé pour la réservation #" + id);
        } else {
            User.addlog(LogType.ADD, "Paiement effectué pour la réservation #" + id);
        }
        this.paymentDate = date;
    }

    /**
     * l'action est loggé
     * 
     * @param client
     */

    public void setClient(Client client) {
        this.client = client;
        User.addlog(LogType.MODIFY,
                "Client de la réservation #" + id + " changé pour " + this.client.getDisplayName());
    }

    /**
     * l'action est loggé
     * 
     * @param date
     */

    public void setDepositDate(Date date) {
        if (date == null) {
            User.addlog(LogType.DELETE, "Accompte annulé pour la réservation #" + id);
        } else {
            User.addlog(LogType.ADD, "Accompte versé pour la réservation #" + id);
        }
        this.depositDate = date;
    }

    /**
     * l'action est loggé
     * 
     * @param cb
     */

    public void setCashBack(CashBack cb) {
        User.addlog(LogType.MODIFY, "Remise de la réservation #" + id + " changé pour " + cb);
        this.cashBack = cb;
    }

    /**
     * l'action est loggé
     * 
     * @param canceled
     */

    public void setCanceled(boolean canceled) {
        User.addlog(LogType.DELETE, "Réservation #" + id + " annulée");
        this.canceled = canceled;
    }

    /**
     * l'action est loggé
     * 
     * @param fileData
     */

    public void setBill(byte[] fileData) {
        User.addlog(LogType.ADD, "Génération de la facture de la réservation #" + id);
        this.bill = fileData;
    }

    @Override
    public boolean isForeignCorrect() {
        return client != null && campground != null && problems != null;
    }

    @Override
    public Color getStatusColor() {
        if (canceled)
            return StatusColors.BLACK;
        if (isInPast())
            return StatusColors.RED;
        if (depositDate != null)
            return paymentDate != null ? StatusColors.GREEN : StatusColors.BLUE;
        return StatusColors.YELLOW;
    }

    /**
     * Définit l'échelle de comparaison d'une réservation selon son état
     * 
     * @return
     */

    private int getCompareScale() {
        if (isInPast() || canceled)
            return -10000; // Reservation passée ou annulée
        if (depositDate == null && paymentDate == null)
            return 1000; // Non payé, pas d'accompte
        if (paymentDate == null)
            return 100; // accompte payé
        return 10; // payé
    }

    @Override
    public int compareTo(Item o) {
        Reservation other = (Reservation) o;
        return (other.getCompareScale() - getCompareScale())
                + this.getStartDate().compareTo(other.getStartDate());
    }
}
