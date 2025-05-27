package client;

import entities.User;
import entities.Service;
import server.UserDAOImpl;
import client.styles.AppStyles;
import client.components.AppointmentForm;
import entities.Order;
import client.validators.UserValidator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Timer;
import java.io.IOException;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableRowSorter;
import javax.swing.SwingConstants;

public class UserInterface extends JFrame {
    private static final Logger logger = Logger.getLogger(UserInterface.class.getName());
    private UserDAOImpl userDAO;
    private final String userLogin;
    private AppointmentForm appointmentForm;
    private JTable historyTable;
    private Timer notificationTimer;
    private Set<Integer> notifiedOrderIds = new HashSet<>();
    private JTable currentOrdersTable;

    public UserInterface(String login) {
        this.userLogin = login;
        this.userDAO = UserDAOImpl.getInstance();
        initializeUI();
        startNotificationTimer();
    }

    private void initializeUI() {
        setTitle("Личный кабинет");
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

        // Вкладка записи на прием
        appointmentForm = new AppointmentForm(userDAO);
        JPanel appointmentPanel = new JPanel(new BorderLayout());
        appointmentPanel.setBackground(Color.WHITE);
        appointmentPanel.add(appointmentForm, BorderLayout.CENTER);
        tabbedPane.addTab("Запись на приём", new ImageIcon(), appointmentPanel, "Запись на приём к мастеру");

        // Вкладка текущих записей
        JPanel currentOrdersPanel = new JPanel(new BorderLayout());
        currentOrdersPanel.setBackground(Color.WHITE);
        currentOrdersTable = createOrdersTable();
        currentOrdersPanel.add(new JScrollPane(currentOrdersTable), BorderLayout.CENTER);
        tabbedPane.addTab("Текущие записи", new ImageIcon(), currentOrdersPanel, "Ваши текущие записи");

        // Вкладка истории записей
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBackground(Color.WHITE);
        historyTable = createOrdersTable();
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        tabbedPane.addTab("История записей", new ImageIcon(), historyPanel, "История ваших записей");

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

        JLabel welcomeLabel = new JLabel("Клиент: " + userLogin);
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

    private JTable createOrdersTable() {
        JTable table = new JTable(createOrderTableModel());
        setupHistoryTable(table);
        return table;
    }

    private void setupHistoryTable(JTable table) {
        table.setFont(AppStyles.LABEL_FONT);
        table.getTableHeader().setFont(AppStyles.LABEL_FONT);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(Color.LIGHT_GRAY);

        // Настраиваем отображение статуса заказа
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                if (status != null) {
                    switch (status) {
                        case "Создан":
                            setBackground(new Color(255, 255, 224)); // Светло-желтый
                            break;
                        case "Подтвержден":
                            setBackground(new Color(144, 238, 144)); // Светло-зеленый
                            break;
                        case "Завершен":
                            setBackground(new Color(176, 196, 222)); // Светло-синий
                            break;
                        default:
                            setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });
    }

    private DefaultTableModel createOrderTableModel() {
        DefaultTableModel model = new DefaultTableModel(
            new Object[] {"№ заказа", "Услуги", "Мастер", "Дата и время", "Статус", "Стоимость"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Integer.class; // Для правильной сортировки ID
                if (columnIndex == 5) return Double.class; // Для правильной сортировки стоимости
                return String.class;
            }
        };
        return model;
    }

    private void refreshData() {
        try {
            displayCurrentOrders();
            displayOrderHistory();
        } catch (Exception e) {
            logger.severe("Ошибка при обновлении данных: " + e.getMessage());
            showErrorDialog("Ошибка при обновлении данных");
        }
    }

    private void displayCurrentOrders() {
        try {
            List<Order> orders = userDAO.getCurrentUserOrders(userLogin);
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            model.addColumn("ID");
            model.addColumn("Мастер");
            model.addColumn("Услуги");
            model.addColumn("Стоимость");
            model.addColumn("Время записи");
            model.addColumn("Статус");

            for (Order order : orders) {
                String servicesStr = order.getServices().stream()
                    .map(service -> service.getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

                String masterName = userDAO.getMasterNameById(order.getMasterId());

                model.addRow(new Object[]{
                    order.getId(),
                    masterName,
                    servicesStr,
                    String.format("%.2f руб.", order.getTotalPrice()),
                    order.getAppointmentTime(),
                    order.getStatus()
                });
            }

            currentOrdersTable.setModel(model);
            logger.info("Отображено текущих заказов: " + orders.size());
        } catch (Exception e) {
            logger.severe("Ошибка при отображении текущих заказов: " + e.getMessage());
            showErrorDialog("Ошибка при загрузке текущих заказов");
        }
    }

    private void displayOrderHistory() {
        try {
            List<Order> orders = userDAO.getUserOrders(userLogin);
            DefaultTableModel model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            model.addColumn("ID");
            model.addColumn("Мастер");
            model.addColumn("Услуги");
            model.addColumn("Стоимость");
            model.addColumn("Время записи");
            model.addColumn("Статус");

            for (Order order : orders) {
                String servicesStr = order.getServices().stream()
                    .map(service -> service.getName())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

                String masterName = userDAO.getMasterNameById(order.getMasterId());

                model.addRow(new Object[]{
                    order.getId(),
                    masterName,
                    servicesStr,
                    String.format("%.2f руб.", order.getTotalPrice()),
                    order.getAppointmentTime(),
                    order.getStatus()
                });
            }

            historyTable.setModel(model);
            logger.info("Отображено заказов в истории: " + orders.size());
        } catch (Exception e) {
            logger.severe("Ошибка при отображении истории заказов: " + e.getMessage());
            showErrorDialog("Ошибка при загрузке истории заказов");
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

    private void startNotificationTimer() {
        notificationTimer = new Timer(5000, e -> checkForNewConfirmations()); // Проверка каждые 5 секунд
        notificationTimer.start();
    }

    private void checkForNewConfirmations() {
        try {
            List<Order> orders = userDAO.getAllOrders();
            for (Order order : orders) {
                if (order.getClientName().equals(userLogin) 
                    && order.getStatus().equals("Подтвержден") 
                    && !notifiedOrderIds.contains(order.getId())) {
                    
                    // Получаем список услуг для заказа
                    List<entities.Service> services = userDAO.getOrderServices(order.getId());
                    String servicesStr = services.stream()
                        .map(entities.Service::getName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");

                    showNotification("Заказ подтвержден", 
                        "Ваш заказ №" + order.getId() + " был подтвержден мастером.\n" +
                        "Услуги: " + servicesStr + "\n" +
                        "Дата и время: " + order.getAppointmentTime());
                    
                    notifiedOrderIds.add(order.getId());
                }
            }
        } catch (Exception e) {
            logger.severe("Ошибка при проверке подтверждений: " + e.getMessage());
        }
    }

    private void showNotification(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(this, title, false);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(400, 200);
            dialog.setLocationRelativeTo(this);

            JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
            contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
            contentPanel.setBackground(Color.WHITE);

            // Иконка уведомления
            JLabel iconLabel = new JLabel(new ImageIcon("resources/notification_icon.png")); // Создайте иконку
            contentPanel.add(iconLabel, BorderLayout.WEST);

            // Текст уведомления
            JTextArea messageArea = new JTextArea(message);
            messageArea.setEditable(false);
            messageArea.setWrapStyleWord(true);
            messageArea.setLineWrap(true);
            messageArea.setBackground(Color.WHITE);
            messageArea.setFont(AppStyles.LABEL_FONT);
            contentPanel.add(messageArea, BorderLayout.CENTER);

            // Кнопка закрытия
            JButton closeButton = new JButton("OK");
            AppStyles.setupButtonStyle(closeButton);
            closeButton.addActionListener(e -> {
                dialog.dispose();
                refreshData(); // Обновляем данные
            });

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(Color.WHITE);
            buttonPanel.add(closeButton);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.add(contentPanel);
            dialog.setVisible(true);

            // Воспроизведение звука уведомления
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception e) {
                logger.warning("Не удалось воспроизвести звук уведомления");
            }
        });
    }

    @Override
    public void dispose() {
        if (notificationTimer != null) {
            notificationTimer.stop();
        }
        super.dispose();
    }
}