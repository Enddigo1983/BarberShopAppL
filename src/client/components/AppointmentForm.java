package client.components;

import entities.Master;
import entities.Order;
import entities.Service;
import server.UserDAOImpl;
import client.styles.AppStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

public class AppointmentForm extends JPanel {
    private static final Logger logger = Logger.getLogger(AppointmentForm.class.getName());
    private final UserDAOImpl userDAO;
    private JTextField nameField;
    private JList<Service> serviceList;
    private DefaultListModel<Service> serviceListModel;
    private JComboBox<MasterItem> masterComboBox;
    private JTextField dateField;
    private JComboBox<String> timeComboBox;
    private JLabel totalPriceLabel;
    private List<Service> selectedServices = new ArrayList<>();

    public AppointmentForm(UserDAOImpl userDAO) {
        this.userDAO = userDAO;
        setupUI();
        // Обновляем список мастеров каждые 30 секунд
        Timer timer = new Timer(30000, e -> loadMasters());
        timer.start();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Форма записи
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Имя клиента
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Ваше имя:");
        AppStyles.setupLabelStyle(nameLabel);
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        nameField = new JTextField();
        AppStyles.setupTextFieldStyle(nameField);
        formPanel.add(nameField, gbc);

        // Услуги
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel serviceLabel = new JLabel("Услуги:");
        AppStyles.setupLabelStyle(serviceLabel);
        formPanel.add(serviceLabel, gbc);

        gbc.gridx = 1;
        serviceListModel = new DefaultListModel<>();
        serviceList = new JList<>(serviceListModel);
        serviceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        loadServices();
        
        // Добавляем слушатель выбора услуг для обновления общей стоимости
        serviceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedServices = serviceList.getSelectedValuesList();
                updateTotalPrice();
            }
        });
        
        JScrollPane serviceScrollPane = new JScrollPane(serviceList);
        serviceScrollPane.setPreferredSize(new Dimension(200, 100));
        formPanel.add(serviceScrollPane, gbc);

        // Общая стоимость
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel priceLabel = new JLabel("Общая стоимость:");
        AppStyles.setupLabelStyle(priceLabel);
        formPanel.add(priceLabel, gbc);

        gbc.gridx = 1;
        totalPriceLabel = new JLabel("0 руб.");
        AppStyles.setupLabelStyle(totalPriceLabel);
        formPanel.add(totalPriceLabel, gbc);

        // Мастер
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel masterLabel = new JLabel("Мастер:");
        AppStyles.setupLabelStyle(masterLabel);
        formPanel.add(masterLabel, gbc);

        gbc.gridx = 1;
        masterComboBox = new JComboBox<>();
        loadMasters();
        AppStyles.setupComboBoxStyle(masterComboBox);
        formPanel.add(masterComboBox, gbc);

        // Дата
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel dateLabel = new JLabel("Дата (ГГГГ-ММ-ДД):");
        AppStyles.setupLabelStyle(dateLabel);
        formPanel.add(dateLabel, gbc);

        gbc.gridx = 1;
        dateField = new JTextField();
        AppStyles.setupTextFieldStyle(dateField);
        dateField.addActionListener(e -> updateAvailableTimeSlots());
        formPanel.add(dateField, gbc);

        // Время
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel timeLabel = new JLabel("Доступное время:");
        AppStyles.setupLabelStyle(timeLabel);
        formPanel.add(timeLabel, gbc);

        gbc.gridx = 1;
        timeComboBox = new JComboBox<>();
        AppStyles.setupComboBoxStyle(timeComboBox);
        formPanel.add(timeComboBox, gbc);

        // Add document listener to date field
        dateField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateAvailableTimeSlots(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateAvailableTimeSlots(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateAvailableTimeSlots(); }
        });

        add(formPanel, BorderLayout.CENTER);

        // Кнопка записи
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        JButton submitButton = new JButton("Записаться");
        AppStyles.setupButtonStyle(submitButton);
        submitButton.addActionListener(e -> handleSubmit());
        buttonPanel.add(submitButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadServices() {
        try {
            List<Service> services = userDAO.getAllServices();
            serviceListModel.clear();
            for (Service service : services) {
                serviceListModel.addElement(service);
            }
            logger.info("Загружено услуг: " + services.size());
        } catch (Exception e) {
            logger.severe("Ошибка загрузки списка услуг: " + e.getMessage());
            showError("Ошибка загрузки списка услуг");
        }
    }

    private void updateTotalPrice() {
        double total = selectedServices.stream()
            .mapToDouble(Service::getPrice)
            .sum();
        totalPriceLabel.setText(String.format("%.2f руб.", total));
    }

    private void loadMasters() {
        try {
            List<Master> masters = userDAO.getAvailableMasters();
            MasterItem selectedItem = (MasterItem) masterComboBox.getSelectedItem();
            
            masterComboBox.removeAllItems();
            
            if (masters.isEmpty()) {
                logger.warning("Нет доступных мастеров в базе данных");
                showError("В данный момент нет доступных мастеров");
                return;
            }

            logger.info("Загружаем " + masters.size() + " мастеров:");
            for (Master master : masters) {
                MasterItem item = new MasterItem(master);
                masterComboBox.addItem(item);
                logger.info("Добавлен мастер: " + master.getName() + " (ID: " + master.getId() + ")");
                
                // Восстанавливаем выбранного мастера, если он был
                if (selectedItem != null && selectedItem.getMaster().getId() == master.getId()) {
                    masterComboBox.setSelectedItem(item);
                }
            }
            
            logger.info("Загрузка мастеров завершена. Всего мастеров: " + masterComboBox.getItemCount());
        } catch (Exception e) {
            logger.severe("Ошибка загрузки списка мастеров: " + e.getMessage());
            e.printStackTrace();  // Добавляем вывод стека ошибки для отладки
            showError("Ошибка загрузки списка мастеров");
        }
    }

    private void updateAvailableTimeSlots() {
        String date = dateField.getText().trim();
        MasterItem selectedMaster = (MasterItem) masterComboBox.getSelectedItem();
        
        if (date.matches("\\d{4}-\\d{2}-\\d{2}") && selectedMaster != null) {
            try {
                List<String> availableSlots = userDAO.getMasterSchedule(selectedMaster.getMaster().getId(), date);
                timeComboBox.removeAllItems();
                for (String slot : availableSlots) {
                    timeComboBox.addItem(slot);
                }
                if (availableSlots.isEmpty()) {
                    showMessage("На выбранную дату нет свободных слотов", "Информация", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e) {
                logger.severe("Ошибка при получении доступного времени: " + e.getMessage());
                showError("Ошибка при получении доступного времени");
            }
        }
    }

    private void handleSubmit() {
        try {
            String clientName = nameField.getText().trim();
            List<Service> services = serviceList.getSelectedValuesList();
            MasterItem selectedMaster = (MasterItem) masterComboBox.getSelectedItem();
            String date = dateField.getText().trim();
            String time = (String) timeComboBox.getSelectedItem();

            // Проверка заполнения полей
            if (clientName.isEmpty() || services.isEmpty() || date.isEmpty() || time == null || selectedMaster == null) {
                showError("Все поля должны быть заполнены и хотя бы одна услуга должна быть выбрана");
                return;
            }

            // Проверка формата даты
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                showError("Неверный формат даты. Используйте формат ГГГГ-ММ-ДД");
                return;
            }

            // Создание заказа
            String dateTime = date + " " + time;
            Order order = new Order(0, "John Doe", clientName, selectedMaster.getMaster().getId(), dateTime);
            order.setServices(services);
            userDAO.createOrder(order, services);

            // Очистка формы
            clearForm();
            showSuccess("Запись успешно создана");

        } catch (Exception e) {
            logger.severe("Ошибка создания записи: " + e.getMessage());
            showError("Ошибка при создании записи: " + e.getMessage());
        }
    }

    private void clearForm() {
        nameField.setText("");
        serviceList.clearSelection();
        dateField.setText("");
        timeComboBox.setSelectedIndex(0);
        if (masterComboBox.getItemCount() > 0) {
            masterComboBox.setSelectedIndex(0);
        }
        totalPriceLabel.setText("0 руб.");
        selectedServices.clear();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Успех", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showMessage(String message, String title, int type) {
        JOptionPane.showMessageDialog(this, message, title, type);
    }

    // Вспомогательный класс для отображения мастеров в выпадающем списке
    private static class MasterItem {
        private final Master master;

        public MasterItem(Master master) {
            this.master = master;
        }

        public Master getMaster() {
            return master;
        }

        @Override
        public String toString() {
            return master.getName();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MasterItem that = (MasterItem) obj;
            return master.getId() == that.master.getId();
        }

        @Override
        public int hashCode() {
            return master.getId();
        }
    }
} 