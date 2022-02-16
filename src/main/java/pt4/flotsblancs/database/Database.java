package pt4.flotsblancs.database;

import java.sql.SQLException;

import lombok.Getter;
import pt4.flotsblancs.database.model.*;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.table.TableUtils;
import io.github.cdimascio.dotenv.Dotenv;

public class Database {
    private static Database instance = null;

    private JdbcPooledConnectionSource conn;

    @Getter
    private Dao<Client, String> clientsDao;

    @Getter
    private Dao<User, String> usersDao;

    @Getter
    private Dao<CampGround, String> campgroundDao;

    @Getter
    private Dao<Bill, String> billDao;

    @Getter
    private Dao<StockBill, String> stockBillDao;

    @Getter
    private Dao<Log, String> logDao;

    @Getter
    private Dao<Problem, String> problemDao;

    @Getter
    private Dao<ClientProblem, String> clientProblemDao;

    @Getter
    private Dao<Reservation, String> reservationDao;

    @Getter
    private Dao<Stock, String> stockDao;


    private Database() throws SQLException {
        Logger.setGlobalLogLevel(Level.ERROR);

        // Chargement variables d'environnement
        Dotenv dotenv = Dotenv.load();

        // Lancement de la connexion avec la BD
        this.conn = new JdbcPooledConnectionSource(dotenv.get("DB_URL"), dotenv.get("DB_USER"),
                dotenv.get("DB_PASSWORD"));

        createAllTablesIfNotExists();
        createAllDAOs();
    }

    public static Database getInstance() throws SQLException {
        if (Database.instance == null)
            Database.instance = new Database();
        return instance;
    }

    public boolean isConnected() {
        if (this.conn == null)
            return false;
        String pingQuery = this.conn.getDatabaseType().getPingStatement();
        DatabaseConnection dbConn;
        try {
            dbConn = conn.getReadOnlyConnection(null);
            dbConn.executeStatement(pingQuery, 1);
            conn.releaseConnection(dbConn);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private void createAllTablesIfNotExists() throws SQLException {
        TableUtils.createTableIfNotExists(conn, Client.class);
        TableUtils.createTableIfNotExists(conn, User.class);
        TableUtils.createTableIfNotExists(conn, CampGround.class);
        TableUtils.createTableIfNotExists(conn, Bill.class);
        TableUtils.createTableIfNotExists(conn, StockBill.class);
        TableUtils.createTableIfNotExists(conn, Log.class);
        TableUtils.createTableIfNotExists(conn, Problem.class);
        TableUtils.createTableIfNotExists(conn, ClientProblem.class);
        TableUtils.createTableIfNotExists(conn, Reservation.class);
        TableUtils.createTableIfNotExists(conn, Stock.class);
    }

    private void createAllDAOs() throws SQLException {
        clientsDao = DaoManager.createDao(conn, Client.class);
        usersDao = DaoManager.createDao(conn, User.class);
        campgroundDao = DaoManager.createDao(conn, CampGround.class);
        billDao = DaoManager.createDao(conn, Bill.class);
        stockBillDao = DaoManager.createDao(conn, StockBill.class);
        logDao = DaoManager.createDao(conn, Log.class);
        problemDao = DaoManager.createDao(conn, Problem.class);
        clientProblemDao = DaoManager.createDao(conn, ClientProblem.class);
        reservationDao = DaoManager.createDao(conn, Reservation.class);
        stockDao = DaoManager.createDao(conn, Stock.class);
    }
}