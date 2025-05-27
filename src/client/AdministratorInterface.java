package client;

import server.UserDAOImpl;
import server.AuthService;
import client.styles.AppStyles;
import entities.User;
import entities.Order;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.logging.Logger;

public class AdministratorInterface extends JFrame {
    private static final Logger logger = Logger.getLogger(AdministratorInterface.class.getName());
    private UserDAOImpl userDAO;
    private AuthService authService;
    private JTable usersTable;
    private JTable ordersTable;
    private final String adminLogin;

    public AdministratorInterface(String login) {
        this.adminLogin = login;
        try {
            userDAO = UserDAOImpl.getInstance();
            authService = new AuthService();
            initializeUI();
        } catch (Exception e) {
            logger.severe("Ошибка при создании AdministratorInterface для " + login + ": " + e.getMessage());
            showErrorDialog("Ошибка при создании интерфейса.");
        }
    }

    private void initializeUI() {
        setTitle("Панель администратора");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        // Основная панель
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(AppStyles.PANEL_PADDING);
        mainPanel.setBackground(AppStyles.SECONDARY_COLOR);

        // Верхняя панель с информацией
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Панель с вкладками
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(AppStyles.LABEL_FONT);
        tabbedPane.setBackground(Color.WHITE);

        // Вкладка пользователей
        JPanel usersPanel = createUsersPanel();
        tabbedPane.addTab("Пользователи", usersPanel);

        // Вкладка заказов
        JPanel ordersPanel = createOrdersPanel();
        tabbedPane.addTab("Заказы", ordersPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);

        pack();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setBackground(AppStyles.PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Приветствие администратора
        JLabel welcomeLabel = new JLabel("Администратор: " + adminLogin);
        welcomeLabel.setFont(AppStyles.HEADER_FONT);
        welcomeLabel.setForeground(Color.WHITE);
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        // Панель с кнопками управления
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setOpaque(false);

        JButton refreshButton = new JButton("Обновить данные");
        AppStyles.setupButtonStyle(refreshButton);
        refreshButton.addActionListener(e -> refreshData());
        controlPanel.add(refreshButton);

        headerPanel.add(controlPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Таблица пользователей
        usersTable = new JTable();
        setupTable(usersTable);
        JScrollPane scrollPane = new JScrollPane(usersTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppStyles.PRIMARY_COLOR, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(Color.WHITE);

        JButton addUserButton = new JButton("Добавить пользователя");
        JButton editUserButton = new JButton("Редактировать");
        JButton deleteUserButton = new JButton("Удалить");

        AppStyles.setupButtonStyle(addUserButton);
        AppStyles.setupButtonStyle(editUserButton);
        AppStyles.setupButtonStyle(deleteUserButton);

        addUserButton.addActionListener(e -> showAddUserDialog());
        editUserButton.addActionListener(e -> editSelectedUser());
        deleteUserButton.addActionListener(e -> deleteSelectedUser());

        buttonPanel.add(addUserButton);
        buttonPanel.add(editUserButton);
        buttonPanel.add(deleteUserButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        displayUsers();
        return panel;
    }

    private JPanel createOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Таблица заказов
        ordersTable = new JTable();
        setupTable(ordersTable);
        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppStyles.PRIMARY_COLOR, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Панель с кнопками
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(Color.WHITE);

        JButton viewDetailsButton = new JButton("Просмотр деталей");
        AppStyles.setupButtonStyle(viewDetailsButton);
        viewDetailsButton.addActionListener(e -> viewOrderDetails());
        buttonPanel.add(viewDetailsButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        displayOrders();
        return panel;
    }

    private void setupTable(JTable table) {
        table.setFont(AppStyles.LABEL_FONT);
        table.setRowHeight(25);
        table.getTableHeader().setFont(AppStyles.LABEL_FONT);
        table.getTableHeader().setBackground(AppStyles.PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(230, 240, 255));
        table.setSelectionForeground(AppStyles.TEXT_COLOR);
        table.setGridColor(new Color(240, 240, 240));
        table.setShowGrid(true);
    }

    private void displayUsers() {
        try {
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            model.addColumn("ID");
            model.addColumn("Логин");
            model.addColumn("Имя");
            model.addColumn("Роль");

            List<User> users = userDAO.getAllUsers();
            for (User user : users) {
                model.addRow(new Object[]{
                    user.getId(), 
                    user.getLogin(), 
                    user.getName(),
                    user.getRole()
                });
            }
            usersTable.setModel(model);
            logger.info("Список пользователей обновлен: " + users.size() + " записей");
        } catch (Exception e) {
            logger.severe("Ошибка при отображении пользователей: " + e.getMessage());
            showErrorDialog("Ошибка при загрузке пользователей");
        }
    }

    private void displayOrders() {
        try {
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            model.addColumn("ID");
            model.addColumn("Клиент");
            model.addColumn("Услуга");
            model.addColumn("Мастер");
            model.addColumn("Время записи");
            model.addColumn("Статус");

            List<Order> orders = userDAO.getAllOrders();
            for (Order order : orders) {
                model.addRow(new Object[]{
                    order.getId(),
                    order.getClientName(),
                    order.getService(),
                    order.getMasterId(),
                    order.getAppointmentTime(),
                    order.getStatus()
                });
            }
            ordersTable.setModel(model);
            logger.info("Список заказов обновлен: " + orders.size() + " записей");
        } catch (Exception e) {
            logger.severe("Ошибка при отображении заказов: " + e.getMessage());
            showErrorDialog("Ошибка при загрузке заказов");
        }
    }

    private void refreshData() {
        displayUsers();
        displayOrders();
        showSuccessDialog("Данные успешно обновлены");
    }

    private void showAddUserDialog() {
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }

    private void editSelectedUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            showErrorDialog("Выберите пользователя для редактирования");
            return;
        }

        int userId = (int) usersTable.getValueAt(selectedRow, 0);
        String userLogin = (String) usersTable.getValueAt(selectedRow, 1);
        String userName = (String) usersTable.getValueAt(selectedRow, 2);
        String userRole = (String) usersTable.getValueAt(selectedRow, 3);

        JDialog editDialog = new JDialog(this, "Редактирование пользователя", true);
        editDialog.setLayout(new BorderLayout(10, 10));
        editDialog.setSize(400, 300);
        editDialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Поля для редактирования
        JTextField loginField = new JTextField(userLogin);
        JTextField nameField = new JTextField(userName);
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> roleComboBox = new JComboBox<>(new String[]{"user", "master", "admin"});
        roleComboBox.setSelectedItem(userRole);

        // Добавление компонентов на форму
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Логин:"), gbc);
        gbc.gridx = 1;
        formPanel.add(loginField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Имя:"), gbc);
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Новый пароль:"), gbc);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Роль:"), gbc);
        gbc.gridx = 1;
        formPanel.add(roleComboBox, gbc);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Сохранить");
        JButton cancelButton = new JButton("Отмена");

        AppStyles.setupButtonStyle(saveButton);
        AppStyles.setupButtonStyle(cancelButton);

        saveButton.addActionListener(e -> {
            try {
                String newLogin = loginField.getText().trim();
                String newName = nameField.getText().trim();
                String newPassword = new String(passwordField.getPassword());
                String newRole = (String) roleComboBox.getSelectedItem();

                if (newLogin.isEmpty()) {
                    showErrorDialog("Логин не может быть пустым");
                    return;
                }

                User updatedUser = new User(newLogin, null, newRole);
                updatedUser.setId(userId);
                updatedUser.setName(newName);
                
                userDAO.update(updatedUser);
                
                if (!newPassword.isEmpty()) {
                    authService.updatePassword(newLogin, newPassword);
                }

                displayUsers();
                editDialog.dispose();
                showSuccessDialog("Пользователь успешно обновлен");
            } catch (Exception ex) {
                logger.severe("Ошибка при обновлении пользователя: " + ex.getMessage());
                showErrorDialog("Ошибка при обновлении пользователя: " + ex.getMessage());
            }
        });

        cancelButton.addActionListener(e -> editDialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        editDialog.add(formPanel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
        editDialog.setVisible(true);
    }

    private void deleteSelectedUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            showErrorDialog("Выберите пользователя для удаления");
            return;
        }

        String userLogin = (String) usersTable.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Вы уверены, что хотите удалить пользователя " + userLogin + "?",
            "Подтверждение удаления",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                userDAO.delete(userLogin);
                displayUsers();
                showSuccessDialog("Пользователь успешно удален");
            } catch (Exception e) {
                logger.severe("Ошибка при удалении пользователя: " + e.getMessage());
                showErrorDialog("Ошибка при удалении пользователя: " + e.getMessage());
            }
        }
    }

    private void viewOrderDetails() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            showErrorDialog("Выберите заказ для просмотра");
            return;
        }

        int orderId = (int) ordersTable.getValueAt(selectedRow, 0);
        try {
            Order order = userDAO.getOrderById(orderId);
            if (order == null) {
                showErrorDialog("Заказ не найден");
                return;
            }

            JDialog detailsDialog = new JDialog(this, "Детали заказа", true);
            detailsDialog.setLayout(new BorderLayout(5, 5));
            detailsDialog.setSize(500, 400);
            detailsDialog.setLocationRelativeTo(this);

            JPanel detailsPanel = new JPanel(new GridBagLayout());
            detailsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Отображение деталей заказа
            addDetailRow(detailsPanel, gbc, 0, "ID заказа:", String.valueOf(order.getId()));
            addDetailRow(detailsPanel, gbc, 1, "Клиент:", order.getClientName());
            addDetailRow(detailsPanel, gbc, 2, "Услуга:", order.getService());
            addDetailRow(detailsPanel, gbc, 3, "Мастер:", String.valueOf(order.getMasterId()));
            addDetailRow(detailsPanel, gbc, 4, "Время записи:", order.getAppointmentTime());
            addDetailRow(detailsPanel, gbc, 5, "Статус:", order.getStatus());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton closeButton = new JButton("Закрыть");
            AppStyles.setupButtonStyle(closeButton);
            closeButton.addActionListener(e -> detailsDialog.dispose());
            buttonPanel.add(closeButton);

            detailsDialog.add(detailsPanel, BorderLayout.CENTER);
            detailsDialog.add(buttonPanel, BorderLayout.SOUTH);
            detailsDialog.setVisible(true);

        } catch (Exception e) {
            logger.severe("Ошибка при просмотре деталей заказа: " + e.getMessage());
            showErrorDialog("Ошибка при загрузке деталей заказа");
        }
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(value), gbc);
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