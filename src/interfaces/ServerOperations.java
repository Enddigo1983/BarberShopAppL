package interfaces;

import dto.AppointmentDTO;

import java.sql.SQLException;
import java.util.List;

public interface ServerOperations {
    void bookAppointment(AppointmentDTO appointment) throws SQLException;
    List<String> getServices() throws SQLException;
    List<String> getMasters() throws SQLException;
    List<String> getAppointments() throws SQLException;
    void addUser(String name, String role) throws SQLException;
    List<String> getUsers() throws SQLException;
    void updateUser(int id, String newName, String newRole) throws SQLException;
}