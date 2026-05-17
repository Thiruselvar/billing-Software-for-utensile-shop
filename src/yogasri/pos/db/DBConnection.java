package yogasri.pos.db;

import yogasri.pos.util.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static Connection connection;

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            AppConfig cfg = AppConfig.getInstance();
            String url = cfg.get("db.url", "jdbc:mysql://localhost:3306/yogasri_pos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            String user = cfg.get("db.user", "root");
            String pass = cfg.get("db.password", "");
            connection = DriverManager.getConnection(url, user, pass);
        }
        return connection;
    }
}

