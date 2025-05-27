import client.LoginFrame;
import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        showMainMenu();
    }

    private static void showMainMenu() {
        while (true) {
            String[] options = new String[]{"Клиент", "Мастер", "Администратор", "Выход"};
            int choice = JOptionPane.showOptionDialog(null, "Выберите опцию:",
                    "Система управления барбершопом", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            if (choice == 3 || choice == -1) { // "Выход" or dialog closed
                System.exit(0);
            }

            // Открываем LoginFrame для авторизации
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            // После авторизации LoginFrame закроется, и интерфейс откроется внутри него
            return;
        }
    }
}