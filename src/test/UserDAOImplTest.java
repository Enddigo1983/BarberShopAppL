package test;

import entities.User;
import entities.Order;
import server.UserDAOImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class UserDAOImplTest {
    private UserDAOImpl userDAO;
    private Connection connection;
    private static final String TEST_DB = "jdbc:sqlite:test.db";

    @BeforeEach
    void setUp() throws Exception {
        // Создаем тестовую базу данных
        connection = DriverManager.getConnection(TEST_DB);
        createTestTables();
        userDAO = UserDAOImpl.getTestInstance(TEST_DB);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Закрываем соединение и удаляем тестовую базу
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("test.db"));
    }

    private void createTestTables() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Создаем таблицу пользователей
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "login TEXT UNIQUE NOT NULL," +
                    "hashed_password TEXT," +
                    "role TEXT NOT NULL," +
                    "name TEXT," +
                    "email TEXT," +
                    "phone TEXT)");

            // Создаем таблицу мастеров
            stmt.execute("CREATE TABLE IF NOT EXISTS masters (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "user_id INTEGER," +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

            // Создаем таблицу заказов
            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "client_name TEXT NOT NULL," +
                    "service TEXT NOT NULL," +
                    "master_id INTEGER," +
                    "appointment_time TEXT NOT NULL," +
                    "status TEXT DEFAULT 'Создан'," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (master_id) REFERENCES masters(id))");
        }
    }

    @Test
    void testCreateAndReadUser() {
        // Создаем тестового пользователя
        User testUser = new User("testUser", "hashedPass123", "USER");
        userDAO.create(testUser);

        // Читаем пользователя из базы
        User retrievedUser = userDAO.read("testUser");
        
        assertNotNull(retrievedUser);
        assertEquals("testUser", retrievedUser.getLogin());
        assertEquals("hashedPass123", retrievedUser.getHashedPassword());
        assertEquals("USER", retrievedUser.getRole());
    }

    @Test
    void testUpdateUser() {
        // Создаем и обновляем пользователя
        User user = new User("updateTest", "oldPass", "USER");
        userDAO.create(user);

        user.setPassword("newPass");
        user.setName("New Name");
        user.setEmail("test@example.com");
        userDAO.updateUser(user);

        // Проверяем обновленные данные
        User updated = userDAO.read("updateTest");
        assertEquals("newPass", updated.getHashedPassword());
        assertEquals("New Name", updated.getName());
        assertEquals("test@example.com", updated.getEmail());
    }

    @Test
    void testDeleteUser() {
        // Создаем и удаляем пользователя
        User user = new User("deleteTest", "pass123", "USER");
        userDAO.create(user);
        userDAO.delete("deleteTest");

        // Проверяем, что пользователь удален
        assertNull(userDAO.read("deleteTest"));
    }

    @Test
    void testGetAllUsers() {
        // Создаем несколько пользователей
        userDAO.create(new User("user1", "pass1", "USER"));
        userDAO.create(new User("user2", "pass2", "ADMIN"));

        // Получаем список всех пользователей
        List<User> users = userDAO.getAllUsers();
        
        assertNotNull(users);
        assertTrue(users.size() >= 2);
        assertTrue(users.stream().anyMatch(u -> u.getLogin().equals("user1")));
        assertTrue(users.stream().anyMatch(u -> u.getLogin().equals("user2")));
    }

    @Test
    void testCreateAndGetOrder() {
        // Создаем заказ
        Order order = new Order(0, "John Doe", "Haircut", 1, "2024-03-20 15:00");
        userDAO.createOrder(order);

        // Получаем список заказов
        List<Order> orders = userDAO.getAllOrders();
        
        assertNotNull(orders);
        assertFalse(orders.isEmpty());
        Order retrievedOrder = orders.get(0);
        assertEquals("John Doe", retrievedOrder.getClientName());
        assertEquals("Haircut", retrievedOrder.getService());
    }

    @Test
    void testHashPassword() {
        String password = "myPassword123";
        String hashedPassword = userDAO.hashPassword(password);
        
        assertNotNull(hashedPassword);
        assertNotEquals(password, hashedPassword);
        assertTrue(hashedPassword.matches("[a-f0-9]{32}")); // MD5 hash format
    }

    @Test
    void testGetMasterNameById() {
        // Создаем мастера в базе
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO masters (id, name, user_id) VALUES (1, 'Master John', 1)");
        } catch (Exception e) {
            fail("Failed to create test master: " + e.getMessage());
        }

        String masterName = userDAO.getMasterNameById(1);
        assertEquals("Master John", masterName);
    }

    @Test
    void testGetNonExistentMaster() {
        String masterName = userDAO.getMasterNameById(999);
        assertEquals("Неизвестный мастер", masterName);
    }
} 