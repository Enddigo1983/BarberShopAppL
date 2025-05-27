package client.styles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AppStyles {
    // Цвета
    public static final Color PRIMARY_COLOR = new Color(51, 153, 255);
    public static final Color SECONDARY_COLOR = new Color(240, 240, 240);
    public static final Color TEXT_COLOR = new Color(51, 51, 51);
    public static final Color BUTTON_COLOR = new Color(51, 153, 255);
    public static final Color BUTTON_HOVER_COLOR = new Color(0, 102, 204);
    
    // Шрифты
    public static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 18);
    public static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    
    // Отступы
    public static final int PADDING = 15;
    public static final EmptyBorder PANEL_PADDING = new EmptyBorder(PADDING, PADDING, PADDING, PADDING);
    public static final EmptyBorder COMPONENT_PADDING = new EmptyBorder(5, 10, 5, 10);
    
    // Размеры
    public static final Dimension BUTTON_SIZE = new Dimension(200, 35);
    public static final Dimension FIELD_SIZE = new Dimension(200, 30);
    
    public static void setupButtonStyle(JButton button) {
        button.setFont(BUTTON_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(BUTTON_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(BUTTON_SIZE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Добавляем эффект при наведении
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER_COLOR);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_COLOR);
            }
        });
    }
    
    public static void setupLabelStyle(JLabel label) {
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_COLOR);
    }
    
    public static void setupTextFieldStyle(JTextField textField) {
        textField.setFont(LABEL_FONT);
        textField.setPreferredSize(FIELD_SIZE);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }
    
    public static void setupComboBoxStyle(JComboBox<?> comboBox) {
        comboBox.setFont(LABEL_FONT);
        comboBox.setPreferredSize(FIELD_SIZE);
        comboBox.setBackground(Color.WHITE);
        ((JComponent) comboBox.getRenderer()).setPreferredSize(new Dimension(FIELD_SIZE.width - 20, 25));
    }
} 