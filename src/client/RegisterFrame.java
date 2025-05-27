package client;

import server.AuthService;
import server.UserDAOImpl;
import client.styles.AppStyles;
import client.validators.UserValidator;
import client.validators.UserValidator.ValidationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

public class RegisterFrame extends JFrame {
    private JTextField loginField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField nameField;
    private JButton registerButton;
    private AuthService authService;
    private UserDAOImpl userDAO;
    private static final Logger logger = Logger.getLogger(RegisterFrame.class.getName());

    public RegisterFrame() {
        authService = new AuthService();
        userDAO = UserDAOImpl.getInstance();
        setupUI();
    }

    private void setupUI() {
        setTitle("Регистрация");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(400, 350));
        setLocationRelativeTo(null);
        
        // Основная панель
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(AppStyles.PANEL_PADDING);
        mainPanel.setBackground(AppStyles.SECONDARY_COLOR);

        // Заголовок
        JLabel headerLabel = new JLabel("Регистрация нового пользователя", SwingConstants.CENTER);
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
        JLabel loginLabel = new JLabel("Логин:");
        AppStyles.setupLabelStyle(loginLabel);
        fieldsPanel.add(loginLabel, gbc);

        gbc.gridx = 1;
        loginField = new JTextField();
        AppStyles.setupTextFieldStyle(loginField);
        fieldsPanel.add(loginField, gbc);

        // Пароль
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel passwordLabel = new JLabel("Пароль:");
        AppStyles.setupLabelStyle(passwordLabel);
        fieldsPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField();
        AppStyles.setupTextFieldStyle(passwordField);
        fieldsPanel.add(passwordField, gbc);

        // Подтверждение пароля
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel confirmLabel = new JLabel("Подтвердите пароль:");
        AppStyles.setupLabelStyle(confirmLabel);
        fieldsPanel.add(confirmLabel, gbc);

        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField();
        AppStyles.setupTextFieldStyle(confirmPasswordField);
        fieldsPanel.add(confirmPasswordField, gbc);

        // Имя
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel nameLabel = new JLabel("Имя:");
        AppStyles.setupLabelStyle(nameLabel);
        fieldsPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        nameField = new JTextField();
        AppStyles.setupTextFieldStyle(nameField);
        fieldsPanel.add(nameField, gbc);

        mainPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Панель с кнопкой
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(AppStyles.SECONDARY_COLOR);
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        registerButton = new JButton("Зарегистрироваться");
        AppStyles.setupButtonStyle(registerButton);
        registerButton.addActionListener(e -> handleRegistration());
        buttonPanel.add(registerButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);

        pack();
    }

    private void handleRegistration() {
        String login = loginField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String name = nameField.getText().trim();

        try {
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

            // Проверка совпадения паролей
            if (!password.equals(confirmPassword)) {
                showErrorDialog("Пароли не совпадают");
                return;
            }

            // Регистрация пользователя с ролью "user"
            authService.register(login, password, "user");
            logger.info("Пользователь " + login + " успешно зарегистрирован");

            showSuccessDialog("Регистрация успешно завершена");
            dispose();
            new LoginFrame().setVisible(true);
        } catch (Exception e) {
            logger.severe("Ошибка при регистрации: " + e.getMessage());
            showErrorDialog("Ошибка при регистрации: " + e.getMessage());
        }
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
}