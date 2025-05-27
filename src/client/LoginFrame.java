package client;

import server.AuthService;
import client.styles.AppStyles;
import client.validators.UserValidator;
import client.validators.UserValidator.ValidationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

public class LoginFrame extends JFrame {
    private static final Logger logger = Logger.getLogger(LoginFrame.class.getName());
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private AuthService authService;

    public LoginFrame() {
        try {
            authService = new AuthService();
            setupUI();
        } catch (RuntimeException e) {
            logger.severe("Ошибка инициализации AuthService: " + e.getMessage());
            showErrorDialog("Ошибка подключения к базе данных. Попробуйте позже.");
            System.exit(1);
        }
    }

    private void setupUI() {
        setTitle("Вход в систему");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(400, 350));
        setLocationRelativeTo(null);

        // Основная панель
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(AppStyles.PANEL_PADDING);
        mainPanel.setBackground(AppStyles.SECONDARY_COLOR);

        // Заголовок
        JLabel headerLabel = new JLabel("Добро пожаловать", SwingConstants.CENTER);
        headerLabel.setFont(AppStyles.HEADER_FONT);
        headerLabel.setForeground(AppStyles.TEXT_COLOR);
        headerLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Панель с полями
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(AppStyles.SECONDARY_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Логин
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Логин:");
        AppStyles.setupLabelStyle(usernameLabel);
        fieldsPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField();
        AppStyles.setupTextFieldStyle(usernameField);
        fieldsPanel.add(usernameField, gbc);

        // Пароль
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel passwordLabel = new JLabel("Пароль:");
        AppStyles.setupLabelStyle(passwordLabel);
        fieldsPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField();
        AppStyles.setupTextFieldStyle(passwordField);
        fieldsPanel.add(passwordField, gbc);

        mainPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(AppStyles.SECONDARY_COLOR);
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        loginButton = new JButton("Войти");
        AppStyles.setupButtonStyle(loginButton);
        loginButton.addActionListener(e -> handleLogin());
        buttonPanel.add(loginButton);

        registerButton = new JButton("Регистрация");
        AppStyles.setupButtonStyle(registerButton);
        registerButton.addActionListener(e -> openRegistration());
        buttonPanel.add(registerButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);

        // Добавляем обработку клавиши Enter
        getRootPane().setDefaultButton(loginButton);

        pack();
    }

    private void handleLogin() {
        String login = usernameField.getText();
        String password = new String(passwordField.getPassword());

        // Проверка логина
        ValidationResult loginValidation = UserValidator.validateLogin(login);
        if (!loginValidation.isValid()) {
            showErrorDialog(loginValidation.getMessage());
            return;
        }

        // Проверка пароля
        ValidationResult passwordValidation = UserValidator.validatePassword(password);
        if (!passwordValidation.isValid()) {
            showErrorDialog(passwordValidation.getMessage());
            return;
        }

        try {
            logger.info("Попытка входа для логина: " + login);
            if (authService.login(login, password)) {
                logger.info("Вход успешен для логина: " + login);
                
                if (authService.needsPasswordUpdate(login)) {
                    handlePasswordUpdate(login);
                }
                
                openUserInterface(login);
                dispose();
            } else {
                logger.warning("Неверный логин или пароль для: " + login);
                showErrorDialog("Неверный логин или пароль!");
            }
        } catch (Exception e) {
            logger.severe("Ошибка входа: " + e.getMessage());
            showErrorDialog("Произошла ошибка при входе. Попробуйте снова.");
        }
    }

    private void handlePasswordUpdate(String login) {
        String newPassword = showPasswordUpdateDialog();
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            try {
                authService.updatePassword(login, newPassword);
                showSuccessDialog("Пароль успешно обновлён!");
                logger.info("Пароль обновлён для: " + login);
            } catch (Exception e) {
                logger.severe("Ошибка обновления пароля: " + e.getMessage());
                showErrorDialog("Ошибка обновления пароля. Вы сможете обновить его позже.");
            }
        }
    }

    private String showPasswordUpdateDialog() {
        JPasswordField passwordField = new JPasswordField();
        AppStyles.setupTextFieldStyle(passwordField);
        
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Введите новый пароль:"), BorderLayout.NORTH);
        panel.add(passwordField, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Обновление пароля",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            return new String(passwordField.getPassword());
        }
        return null;
    }

    private void openUserInterface(String login) {
        try {
            String role = authService.getRole(login);
            logger.info("Роль пользователя " + login + ": " + role);
            
            if (role == null) {
                showErrorDialog("Ошибка: роль пользователя не определена!");
                return;
            }

            SwingUtilities.invokeLater(() -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        new AdministratorInterface(login).setVisible(true);
                        break;
                    case "master":
                        new MasterInterface(login).setVisible(true);
                        break;
                    default:
                        new UserInterface(login).setVisible(true);
                        break;
                }
            });
        } catch (Exception e) {
            logger.severe("Ошибка открытия интерфейса: " + e.getMessage());
            showErrorDialog("Ошибка при открытии интерфейса.");
        }
    }

    private void openRegistration() {
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Ошибка",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void showSuccessDialog(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Успех",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}