package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserHandler extends DatabaseHandler {
    @Override
    protected String getTableName() {
        return "users";
    }

    public void addUser(String name, String role) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (name, role) VALUES (?, ?)")) {
            stmt.setString(1, name);
            stmt.setString(2, role);
            stmt.executeUpdate();
        }
    }

    public List<String> getUsers() throws SQLException {
        List<String> users = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name, role FROM users");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add("ID: " + rs.getInt("id") + ", Name: " + rs.getString("name") + ", Role: " + rs.getString("role"));
            }
        }
        return users;
    }

    public void updateUser(int id, String newName, String newRole) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET name = COALESCE(?, name), role = ? WHERE id = ?")) {
            stmt.setString(1, newName);
            stmt.setString(2, newRole);
            stmt.setInt(3, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("User with ID " + id + " not found.");
            }
        }
    }
}