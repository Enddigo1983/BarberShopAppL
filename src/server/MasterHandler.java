package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MasterHandler extends DatabaseHandler {
    @Override
    protected String getTableName() {
        return "masters";
    }

    public List<String> getAllMasters() throws SQLException {
        List<String> masters = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM masters");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                masters.add(rs.getString("name"));
            }
        }
        return masters;
    }
}