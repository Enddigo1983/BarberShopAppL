package server;

import dto.AppointmentDTO;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AppointmentHandler extends DatabaseHandler implements Handler {
    private ObjectOutputStream out;
    private ObjectInputStream in;

    @Override
    protected String getTableName() {
        return "appointments";
    }

    @Override
    public void setStreams(ObjectOutputStream out, ObjectInputStream in) {
        this.out = out;
        this.in = in;
    }

    @Override
    public void handle(String command) throws Exception {
        if ("BOOK_APPOINTMENT".equals(command)) {
            AppointmentDTO appointment = (AppointmentDTO) in.readObject();
            try {
                bookAppointment(appointment);
                out.writeObject("SUCCESS");
                out.writeObject("Запись успешно создана!");
            } catch (SQLException e) {
                out.writeObject("ERROR");
                out.writeObject("Ошибка базы данных: " + e.getMessage());
            }
        } else if ("GET_APPOINTMENTS".equals(command)) {
            try {
                List<String> appointments = getAppointments();
                out.writeObject("SUCCESS");
                out.writeObject(appointments);
            } catch (SQLException e) {
                out.writeObject("ERROR");
                out.writeObject("Ошибка базы данных: " + e.getMessage());
            }
        }
    }

    public void bookAppointment(AppointmentDTO appointment) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO appointments (client_name, service_id, master_id, appointment_date) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, appointment.getClientName());
            stmt.setInt(2, appointment.getServiceId());
            stmt.setInt(3, appointment.getMasterId());
            stmt.setString(4, appointment.getAppointmentDate());
            stmt.executeUpdate();
        }
    }

    public List<String> getAppointments() throws SQLException {
        List<String> appointments = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT a.id, a.client_name, s.name AS service_name, m.name AS master_name, a.appointment_date " +
                             "FROM appointments a " +
                             "JOIN services s ON a.service_id = s.id " +
                             "JOIN masters m ON a.master_id = m.id");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                appointments.add("ID: " + rs.getInt("id") +
                        ", Client: " + rs.getString("client_name") +
                        ", Service: " + rs.getString("service_name") +
                        ", Master: " + rs.getString("master_name") +
                        ", Date: " + rs.getString("appointment_date"));
            }
        }
        return appointments;
    }
}