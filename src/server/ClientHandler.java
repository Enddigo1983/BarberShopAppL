package server;

import dto.AppointmentDTO;
import exceptions.ServerException;
import interfaces.ServerOperations;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private ServerOperations server;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket clientSocket, ServerOperations server) {
        this.clientSocket = clientSocket;
        this.server = server;
        try {
            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("Error initializing streams for client: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String request = (String) in.readObject();
                if (request == null || request.equals("EXIT")) {
                    break;
                }

                switch (request) {
                    case "BOOK_APPOINTMENT":
                        handleBookAppointment();
                        break;
                    case "GET_SERVICES":
                        handleGetServices();
                        break;
                    case "GET_MASTERS":
                        handleGetMasters();
                        break;
                    case "GET_APPOINTMENTS":
                        handleGetAppointments();
                        break;
                    case "ADD_USER":
                        handleAddUser();
                        break;
                    case "GET_USERS":
                        handleGetUsers();
                        break;
                    case "UPDATE_USERS":
                        handleUpdateUsers();
                        break;
                    default:
                        sendError("Unknown request: " + request);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handleBookAppointment() throws IOException, ClassNotFoundException {
        try {
            AppointmentDTO appointment = (AppointmentDTO) in.readObject();
            server.bookAppointment(appointment);
            out.writeObject("SUCCESS");
            out.writeObject("Appointment booked successfully");
        } catch (SQLException e) {
            sendError("Failed to book appointment: " + e.getMessage());
        }
    }

    private void handleGetServices() throws IOException {
        try {
            List<String> services = server.getServices();
            out.writeObject("SUCCESS");
            out.writeObject(services);
        } catch (SQLException e) {
            sendError("Failed to retrieve services: " + e.getMessage());
        }
    }

    private void handleGetMasters() throws IOException {
        try {
            List<String> masters = server.getMasters();
            out.writeObject("SUCCESS");
            out.writeObject(masters);
        } catch (SQLException e) {
            sendError("Failed to retrieve masters: " + e.getMessage());
        }
    }

    private void handleGetAppointments() throws IOException {
        try {
            List<String> appointments = server.getAppointments();
            out.writeObject("SUCCESS");
            out.writeObject(appointments);
        } catch (SQLException e) {
            sendError("Failed to retrieve appointments: " + e.getMessage());
        }
    }

    private void handleAddUser() throws IOException, ClassNotFoundException {
        try {
            String name = (String) in.readObject();
            String role = (String) in.readObject();
            server.addUser(name, role);
            out.writeObject("SUCCESS");
            out.writeObject("User added successfully");
        } catch (SQLException e) {
            sendError("Failed to add user: " + e.getMessage());
        }
    }

    private void handleGetUsers() throws IOException {
        try {
            List<String> users = server.getUsers();
            out.writeObject("SUCCESS");
            out.writeObject(users);
        } catch (SQLException e) {
            sendError("Failed to retrieve users: " + e.getMessage());
        }
    }

    private void handleUpdateUsers() throws IOException, ClassNotFoundException {
        try {
            int userId = (int) in.readObject();
            String newName = (String) in.readObject();
            String newRole = (String) in.readObject();
            server.updateUser(userId, newName, newRole);
            out.writeObject("SUCCESS");
            out.writeObject("User updated successfully");
        } catch (SQLException e) {
            sendError("Failed to update user: " + e.getMessage());
        }
    }

    private void sendError(String message) throws IOException {
        out.writeObject("ERROR");
        out.writeObject(message);
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }
}