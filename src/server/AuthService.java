package server;

import entities.User;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthService {
    private UserDAOImpl userDAO;
    private MessageDigest md;

    public AuthService() {
        userDAO = UserDAOImpl.getInstance();
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public boolean login(String login, String password) {
        User user = userDAO.read(login);
        if (user == null) {
            return false;
        }
        // Проверка на старого пользователя с пустым паролем
        if (user.getHashedPassword() == null || user.getHashedPassword().isEmpty()) {
            return true;
        }
        String hashedInput = hashPassword(password);
        return user.getHashedPassword().equals(hashedInput);
    }

    public boolean register(String login, String password, String role) {
        if (userDAO.read(login) == null) {
            String hashedPassword = hashPassword(password);
            User newUser = new User(login, hashedPassword, role);
            userDAO.create(newUser);
            return true;
        }
        return false;
    }

    public void updatePassword(String login, String newPassword) {
        User user = userDAO.read(login);
        if (user != null) {
            user = new User(user.getLogin(), hashPassword(newPassword), user.getRole());
            user.setId(user.getId());
            userDAO.update(user);
        }
    }

    private String hashPassword(String password) {
        byte[] hashedBytes = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String getRole(String login) {
        User user = userDAO.read(login);
        return user != null ? user.getRole() : null;
    }

    public boolean needsPasswordUpdate(String login) {
        User user = userDAO.read(login);
        return user != null && (user.getHashedPassword() == null || user.getHashedPassword().isEmpty());
    }
}