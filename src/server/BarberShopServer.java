package server;

import dto.AppointmentDTO;
import interfaces.ServerOperations;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

public class BarberShopServer implements ServerOperations {
    private ServerSocket serverSocket;
    private HandlerFactory handlerFactory;

    public BarberShopServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        handlerFactory = new HandlerFactory();
        System.out.println("Server started on port " + port);
    }

    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.err.println("Error accepting client connection: " + e.getMessage());
        }
    }

    @Override
    public void bookAppointment(AppointmentDTO appointment) throws SQLException {
        AppointmentHandler appointmentHandler = (AppointmentHandler) handlerFactory.createHandler("appointment");
        appointmentHandler.bookAppointment(appointment);
    }

    @Override
    public List<String> getServices() throws SQLException {
        ServiceHandler serviceHandler = (ServiceHandler) handlerFactory.createHandler("service");
        return serviceHandler.getAllServices();
    }

    @Override
    public List<String> getMasters() throws SQLException {
        MasterHandler masterHandler = (MasterHandler) handlerFactory.createHandler("master");
        return masterHandler.getAllMasters();
    }

    @Override
    public List<String> getAppointments() throws SQLException {
        AppointmentHandler appointmentHandler = (AppointmentHandler) handlerFactory.createHandler("appointment");
        return appointmentHandler.getAppointments();
    }

    @Override
    public void addUser(String name, String role) throws SQLException {
        UserHandler userHandler = (UserHandler) handlerFactory.createHandler("user");
        userHandler.addUser(name, role);
    }

    @Override
    public List<String> getUsers() throws SQLException {
        UserHandler userHandler = (UserHandler) handlerFactory.createHandler("user");
        return userHandler.getUsers();
    }

    @Override
    public void updateUser(int id, String newName, String newRole) throws SQLException {
        UserHandler userHandler = (UserHandler) handlerFactory.createHandler("user");
        userHandler.updateUser(id, newName, newRole);
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            int port = 5000; // Default port
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
            BarberShopServer server = new BarberShopServer(port);
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}