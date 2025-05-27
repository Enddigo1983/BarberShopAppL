package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceHandler extends DatabaseHandler {
    @Override
    protected String getTableName() {
        return "services";
    }

    public List<String> getAllServices() throws SQLException {
        List<String> services = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM services");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                services.add(rs.getString("name"));
            }
        }
        return services;
    }
}