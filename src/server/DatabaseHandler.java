package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public abstract class DatabaseHandler {
    private static Connection connection;
    private static String databasePath;

    // Static block to load the database path from server.properties
    static {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("server.properties")) {
            props.load(fis);
            databasePath = props.getProperty("databasePath", "barbershop.db");
        } catch (IOException e) {
            System.err.println("Error loading database path: " + e.getMessage());
            databasePath = "barbershop.db"; // Fallback to default
        }
    }

    protected Connection getConnection() throws SQLException {
        // Singleton pattern for database connection
        if (connection == null || connection.isClosed()) {
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
                System.out.println("Database connection established to: " + databasePath);
            } catch (SQLException e) {
                throw new SQLException("Failed to establish database connection: " + e.getMessage());
            }
        }
        return connection;
    }

    // Abstract method to be implemented by subclasses
    protected abstract String getTableName();

    // Close the connection (called when the server shuts down)
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
}