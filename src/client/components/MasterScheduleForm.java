package client.components;

import server.UserDAOImpl;
import client.styles.AppStyles;
import server.UserDAOImpl.MasterScheduleEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

public class MasterScheduleForm extends JPanel {
    private static final Logger logger = Logger.getLogger(MasterScheduleForm.class.getName());
    private final UserDAOImpl userDAO;
    private final int masterId;
    private final String[] DAYS_OF_WEEK = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
    private JComboBox<String>[] startTimeBoxes;
    private JComboBox<String>[] endTimeBoxes;
    private JCheckBox[] workingDayBoxes;

    @SuppressWarnings("unchecked")
    public MasterScheduleForm(UserDAOImpl userDAO, int masterId) {
        this.userDAO = userDAO;
        this.masterId = masterId;
        
        startTimeBoxes = new JComboBox[7];
        endTimeBoxes = new JComboBox[7];
        workingDayBoxes = new JCheckBox[7];
        
        setupUI();
        loadSchedule();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        // Заголовок
        JLabel titleLabel = new JLabel("Настройка расписания работы");
        titleLabel.setFont(AppStyles.HEADER_FONT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // Панель с расписанием
        JPanel schedulePanel = new JPanel(new GridBagLayout());
        schedulePanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Заголовки столбцов
        gbc.gridy = 0;
        gbc.gridx = 0; addHeaderLabel("День недели", schedulePanel, gbc);
        gbc.gridx = 1; addHeaderLabel("Рабочий день", schedulePanel, gbc);
        gbc.gridx = 2; addHeaderLabel("Начало работы", schedulePanel, gbc);
        gbc.gridx = 3; addHeaderLabel("Конец работы", schedulePanel, gbc);

        // Создаем строки для каждого дня недели
        for (int i = 0; i < 7; i++) {
            gbc.gridy = i + 1;
            
            // День недели
            gbc.gridx = 0;
            JLabel dayLabel = new JLabel(DAYS_OF_WEEK[i]);
            AppStyles.setupLabelStyle(dayLabel);
            schedulePanel.add(dayLabel, gbc);

            // Чекбокс рабочего дня
            gbc.gridx = 1;
            workingDayBoxes[i] = new JCheckBox();
            workingDayBoxes[i].setBackground(Color.WHITE);
            schedulePanel.add(workingDayBoxes[i], gbc);

            // Время начала
            gbc.gridx = 2;
            startTimeBoxes[i] = createTimeComboBox();
            schedulePanel.add(startTimeBoxes[i], gbc);

            // Время окончания
            gbc.gridx = 3;
            endTimeBoxes[i] = createTimeComboBox();
            schedulePanel.add(endTimeBoxes[i], gbc);
        }

        add(schedulePanel, BorderLayout.CENTER);

        // Кнопка сохранения
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        JButton saveButton = new JButton("Сохранить расписание");
        AppStyles.setupButtonStyle(saveButton);
        saveButton.addActionListener(e -> saveSchedule());
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addHeaderLabel(String text, JPanel panel, GridBagConstraints gbc) {
        JLabel label = new JLabel(text);
        label.setFont(AppStyles.LABEL_FONT.deriveFont(Font.BOLD));
        panel.add(label, gbc);
    }

    private JComboBox<String> createTimeComboBox() {
        JComboBox<String> timeBox = new JComboBox<>();
        LocalTime time = LocalTime.of(0, 0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        
        for (int i = 0; i < 48; i++) { // Каждые 30 минут в течение суток
            timeBox.addItem(time.format(formatter));
            time = time.plusMinutes(30);
        }
        
        AppStyles.setupComboBoxStyle(timeBox);
        return timeBox;
    }

    private void loadSchedule() {
        try {
            List<MasterScheduleEntry> schedule = userDAO.getMasterSchedule(masterId);
            
            // Устанавливаем значения по умолчанию
            for (int i = 0; i < 7; i++) {
                workingDayBoxes[i].setSelected(false);
                startTimeBoxes[i].setSelectedItem("09:00");
                endTimeBoxes[i].setSelectedItem("18:00");
            }

            // Загружаем сохраненное расписание
            for (MasterScheduleEntry entry : schedule) {
                int day = entry.getDayOfWeek();
                workingDayBoxes[day].setSelected(entry.isWorking());
                startTimeBoxes[day].setSelectedItem(entry.getStartTime());
                endTimeBoxes[day].setSelectedItem(entry.getEndTime());
            }
        } catch (Exception e) {
            logger.severe("Ошибка загрузки расписания: " + e.getMessage());
            showError("Ошибка загрузки расписания");
        }
    }

    private void saveSchedule() {
        try {
            for (int i = 0; i < 7; i++) {
                userDAO.setMasterSchedule(
                    masterId,
                    i,
                    (String) startTimeBoxes[i].getSelectedItem(),
                    (String) endTimeBoxes[i].getSelectedItem(),
                    workingDayBoxes[i].isSelected()
                );
            }
            showSuccess("Расписание успешно сохранено");
        } catch (Exception e) {
            logger.severe("Ошибка сохранения расписания: " + e.getMessage());
            showError("Ошибка сохранения расписания");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Успех", JOptionPane.INFORMATION_MESSAGE);
    }
} 