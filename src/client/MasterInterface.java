package client;

import server.UserDAOImpl;
import client.styles.AppStyles;
import client.components.MasterScheduleForm;
import entities.Order;
import entities.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

public class MasterInterface extends JFrame {
    private static final Logger logger = Logger.getLogger(MasterInterface.class.getName());
    private UserDAOImpl userDAO;
    private JTable activeOrdersTable;
    private JTable confirmedOrdersTable;
    private JTable completedOrdersTable;
    private final String masterLogin;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public MasterInterface(String login) {
        this.masterLogin = login;
        try {
            userDAO = UserDAOImpl.getInstance();
            initializeUI();
            refreshData();
        } catch (Exception e) {
            logger.severe("Ошибка при создании интерфейса мастера: " + e.getMessage());
            showError("Ошибка при создании интерфейса");
        }
    }

    private void initializeUI() {
        setTitle("Панель мастера");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        // Основная панель
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // Верхняя панель с информацией
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Панель с вкладками
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(AppStyles.LABEL_FONT);
        tabbedPane.setBackground(Color.WHITE);

        // Вкладка новых заказов
        JPanel newOrdersPanel = createOrdersPanel("Новые заказы");
        activeOrdersTable = createOrdersTable();
        newOrdersPanel.add(new JScrollPane(activeOrdersTable), BorderLayout.CENTER);
        tabbedPane.addTab("Новые заказы", newOrdersPanel);

        // Вкладка подтвержденных заказов
        JPanel confirmedOrdersPanel = createOrdersPanel("Подтвержденные заказы");
        confirmedOrdersTable = createOrdersTable();
        confirmedOrdersPanel.add(new JScrollPane(confirmedOrdersTable), BorderLayout.CENTER);
        tabbedPane.addTab("Подтвержденные", confirmedOrdersPanel);

        // Вкладка завершенных заказов
        JPanel completedOrdersPanel = createOrdersPanel("Завершенные заказы");
        completedOrdersTable = createOrdersTable();
        completedOrdersPanel.add(new JScrollPane(completedOrdersTable), BorderLayout.CENTER);
        tabbedPane.addTab("Завершенные", completedOrdersPanel);

        // Вкладка с расписанием
        int masterId = userDAO.getMasterIdByLogin(masterLogin);
        JPanel schedulePanel = new MasterScheduleForm(userDAO, masterId);
        tabbedPane.addTab("Моё расписание", new ImageIcon(), schedulePanel, "Настройка расписания работы");

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);

        pack();
        
        // Запускаем автоматическое обновление каждые 30 секунд
        Timer timer = new Timer(30000, e -> refreshData());
        timer.start();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setBackground(AppStyles.PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel welcomeLabel = new JLabel("Мастер: " + masterLogin);
        welcomeLabel.setFont(AppStyles.HEADER_FONT);
        welcomeLabel.setForeground(Color.WHITE);
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setOpaque(false);

        JButton refreshButton = new JButton("Обновить");
        JButton logoutButton = new JButton("Выход");
        
        AppStyles.setupButtonStyle(refreshButton);
        AppStyles.setupButtonStyle(logoutButton);
        
        refreshButton.addActionListener(e -> refreshData());
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        controlPanel.add(refreshButton);
        controlPanel.add(logoutButton);

        headerPanel.add(controlPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createOrdersPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(Color.WHITE);

        if (title.equals("Новые заказы")) {
            JButton confirmButton = new JButton("Подтвердить заказ");
            AppStyles.setupButtonStyle(confirmButton);
            confirmButton.addActionListener(e -> confirmSelectedOrder());
            buttonPanel.add(confirmButton);
        } else if (title.equals("Подтвержденные заказы")) {
            JButton completeButton = new JButton("Завершить заказ");
            AppStyles.setupButtonStyle(completeButton);
            completeButton.addActionListener(e -> completeSelectedOrder());
            buttonPanel.add(completeButton);
        }

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JTable createOrdersTable() {
        JTable table = new JTable();
        table.setFont(AppStyles.LABEL_FONT);
        table.setRowHeight(25);
        table.getTableHeader().setFont(AppStyles.LABEL_FONT);
        table.getTableHeader().setBackground(AppStyles.PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(230, 240, 255));
        table.setSelectionForeground(AppStyles.TEXT_COLOR);
        table.setGridColor(new Color(240, 240, 240));
        table.setShowGrid(true);
        return table;
    }

    private void refreshData() {
        try {
            displayActiveOrders();
            displayConfirmedOrders();
            displayCompletedOrders();
            logger.info("Данные обновлены для мастера " + masterLogin);
        } catch (Exception e) {
            logger.severe("Ошибка при обновлении данных: " + e.getMessage());
            showError("Ошибка при обновлении данных");
        }
    }

    private void displayActiveOrders() {
        try {
            List<Order> orders = userDAO.getActiveOrdersForMaster(masterLogin);
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            model.addColumn("ID");
            model.addColumn("Клиент");
            model.addColumn("Услуги");
            model.addColumn("Стоимость");
            model.addColumn("Время записи");
            model.addColumn("Статус");

            for (Order order : orders) {
                String servicesStr = order.getServices().stream()
                    .map(service -> service.getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

                model.addRow(new Object[]{
                    order.getId(),
                    order.getClientName(),
                    servicesStr,
                    String.format("%.2f руб.", order.getTotalPrice()),
                    order.getAppointmentTime(),
                    order.getStatus()
                });
            }

            activeOrdersTable.setModel(model);
            logger.info("Отображено новых заказов: " + orders.size());
        } catch (Exception e) {
            logger.severe("Ошибка при отображении новых заказов: " + e.getMessage());
            showError("Ошибка при загрузке новых заказов");
        }
    }

    private void displayConfirmedOrders() {
        try {
            List<Order> orders = userDAO.getConfirmedOrdersForMaster(masterLogin);
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            model.addColumn("ID");
            model.addColumn("Клиент");
            model.addColumn("Услуги");
            model.addColumn("Стоимость");
            model.addColumn("Время записи");
            model.addColumn("Статус");

            for (Order order : orders) {
                String servicesStr = order.getServices().stream()
                    .map(service -> service.getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

                model.addRow(new Object[]{
                    order.getId(),
                    order.getClientName(),
                    servicesStr,
                    String.format("%.2f руб.", order.getTotalPrice()),
                    order.getAppointmentTime(),
                    order.getStatus()
                });
            }

            confirmedOrdersTable.setModel(model);
            logger.info("Отображено подтвержденных заказов: " + orders.size());
        } catch (Exception e) {
            logger.severe("Ошибка при отображении подтвержденных заказов: " + e.getMessage());
            showError("Ошибка при загрузке подтвержденных заказов");
        }
    }

    private void displayCompletedOrders() {
        try {
            List<Order> orders = userDAO.getCompletedOrdersForMaster(masterLogin);
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            model.addColumn("ID");
            model.addColumn("Клиент");
            model.addColumn("Услуги");
            model.addColumn("Стоимость");
            model.addColumn("Время записи");
            model.addColumn("Статус");

            for (Order order : orders) {
                String servicesStr = order.getServices().stream()
                    .map(service -> service.getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

                model.addRow(new Object[]{
                    order.getId(),
                    order.getClientName(),
                    servicesStr,
                    String.format("%.2f руб.", order.getTotalPrice()),
                    order.getAppointmentTime(),
                    order.getStatus()
                });
            }

            completedOrdersTable.setModel(model);
            logger.info("Отображено завершенных заказов: " + orders.size());
        } catch (Exception e) {
            logger.severe("Ошибка при отображении завершенных заказов: " + e.getMessage());
            showError("Ошибка при загрузке завершенных заказов");
        }
    }

    private void confirmSelectedOrder() {
        int selectedRow = activeOrdersTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Выберите заказ для подтверждения");
            return;
        }

        try {
            int orderId = (int) activeOrdersTable.getValueAt(selectedRow, 0);
            userDAO.confirmOrder(orderId);
            showSuccess("Заказ успешно подтвержден");
            refreshData();
        } catch (Exception e) {
            logger.severe("Ошибка при подтверждении заказа: " + e.getMessage());
            showError("Ошибка при подтверждении заказа");
        }
    }

    private void completeSelectedOrder() {
        int selectedRow = confirmedOrdersTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Выберите заказ для завершения");
            return;
        }

        try {
            int orderId = (int) confirmedOrdersTable.getValueAt(selectedRow, 0);
            userDAO.completeOrder(orderId);
            showSuccess("Заказ успешно завершен");
            refreshData();
        } catch (Exception e) {
            logger.severe("Ошибка при завершении заказа: " + e.getMessage());
            showError("Ошибка при завершении заказа");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Успех", JOptionPane.INFORMATION_MESSAGE);
    }
} 