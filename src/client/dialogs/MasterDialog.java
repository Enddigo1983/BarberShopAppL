package client.dialogs;

import entities.Master;
import entities.User;
import server.UserDAOImpl;
import client.validators.UserValidator;
import client.validators.UserValidator.ValidationResult;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MasterDialog extends JDialog {
    private JTextField nameField;
    private JComboBox<User> userComboBox;
    private boolean approved = false;
    private Master master;
    private UserDAOImpl userDAO;

    public MasterDialog(JFrame parent, Master master) {
        super(parent, master == null ? "Добавление мастера" : "Редактирование мастера", true);
        this.master = master;
        this.userDAO = UserDAOImpl.getInstance();
        initComponents();
        loadData();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Панель для полей ввода
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        inputPanel.add(new JLabel("Имя мастера:"));
        nameField = new JTextField(20);
        inputPanel.add(nameField);
        
        inputPanel.add(new JLabel("Пользователь:"));
        userComboBox = new JComboBox<>();
        inputPanel.add(userComboBox);
        
        add(inputPanel, BorderLayout.CENTER);
        
        // Панель для кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Отмена");
        
        okButton.addActionListener(e -> onOK());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadData() {
        // Загружаем список пользователей
        List<User> users = userDAO.getAllUsers();
        for (User user : users) {
            userComboBox.addItem(user);
        }

        // Если редактируем существующего мастера
        if (master != null) {
            nameField.setText(master.getName());
            for (int i = 0; i < userComboBox.getItemCount(); i++) {
                User user = userComboBox.getItemAt(i);
                if (user.getId() == master.getUserId()) {
                    userComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void onOK() {
        String name = nameField.getText().trim();
        User selectedUser = (User) userComboBox.getSelectedItem();

        // Валидация имени
        ValidationResult nameValidation = UserValidator.validateName(name);
        if (!nameValidation.isValid()) {
            JOptionPane.showMessageDialog(this, nameValidation.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Выберите пользователя", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (master == null) {
                // Создание нового мастера
                master = new Master(0, name, selectedUser.getId());
                userDAO.createMaster(master);
            } else {
                // Обновление существующего мастера
                master.setName(name);
                master.setUserId(selectedUser.getId());
                userDAO.updateMaster(master);
            }
            approved = true;
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Ошибка при сохранении мастера: " + e.getMessage(), 
                "Ошибка", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isApproved() {
        return approved;
    }

    public Master getMaster() {
        return master;
    }
} 