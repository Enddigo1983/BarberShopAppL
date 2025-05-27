package server;

import entities.Master;
import entities.Order;
import entities.User;
import entities.Service;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.io.File;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

public class UserDAOImpl implements UserDAO {
    private static final Logger logger = Logger.getLogger(UserDAOImpl.class.getName());
    private static UserDAOImpl instance;
    private Connection connection;

    private UserDAOImpl() {
        try {
            File dbFile = new File("barbershop.db");
            if (!dbFile.exists()) {
                logger.warning("Файл базы данных barbershop.db не найден. Создаём новый.");
            }
            connection = DriverManager.getConnection("jdbc:sqlite:barbershop.db");
            checkTableStructure();
        } catch (SQLException e) {
            logger.severe("Ошибка подключения к базе данных: " + e.getMessage());
            throw new RuntimeException("Не удалось подключиться к базе данных", e);
        }
    }

    // Конструктор для тестов
    private UserDAOImpl(String dbUrl) {
        try {
            connection = DriverManager.getConnection(dbUrl);
            checkTableStructure();
        } catch (SQLException e) {
            logger.severe("Ошибка подключения к тестовой базе данных: " + e.getMessage());
            throw new RuntimeException("Не удалось подключиться к тестовой базе данных", e);
        }
    }

    public static synchronized UserDAOImpl getInstance() {
        if (instance == null) {
            instance = new UserDAOImpl();
        }
        return instance;
    }

    // Метод для создания тестового экземпляра
    public static UserDAOImpl getTestInstance(String dbUrl) {
        return new UserDAOImpl(dbUrl);
    }

    private void checkTableStructure() {
        try (Statement stmt = connection.createStatement()) {
            // Проверка таблицы users
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)");
            boolean hasLogin = false, hasHashedPassword = false, hasRole = false,
                    hasName = false, hasEmail = false, hasPhone = false;
            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("login".equals(columnName)) hasLogin = true;
                if ("hashed_password".equals(columnName)) hasHashedPassword = true;
                if ("role".equals(columnName)) hasRole = true;
                if ("name".equals(columnName)) hasName = true;
                if ("email".equals(columnName)) hasEmail = true;
                if ("phone".equals(columnName)) hasPhone = true;
            }

            // Если таблица не существует или не имеет нужных колонок, создаем её заново
            if (!hasLogin || !hasHashedPassword || !hasRole) {
                stmt.executeUpdate("DROP TABLE IF EXISTS users");
                stmt.executeUpdate("CREATE TABLE users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "login TEXT UNIQUE NOT NULL," +
                        "hashed_password TEXT," +
                        "role TEXT NOT NULL CHECK(role IN ('user', 'master', 'admin'))," +
                        "name TEXT," +
                        "email TEXT," +
                        "phone TEXT)");
                logger.info("Таблица users создана с обновленным ограничением роли.");
            } else {
                // Обновляем существующую таблицу для поддержки роли 'master'
                stmt.executeUpdate("CREATE TABLE users_temp (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "login TEXT UNIQUE NOT NULL," +
                        "hashed_password TEXT," +
                        "role TEXT NOT NULL CHECK(role IN ('user', 'master', 'admin'))," +
                        "name TEXT," +
                        "email TEXT," +
                        "phone TEXT)");
                stmt.executeUpdate("INSERT INTO users_temp SELECT * FROM users");
                stmt.executeUpdate("DROP TABLE users");
                stmt.executeUpdate("ALTER TABLE users_temp RENAME TO users");
                logger.info("Ограничение роли в таблице users обновлено.");
            }

            // Добавляем новые колонки, если их нет
            if (!hasName) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN name TEXT");
            }
            if (!hasEmail) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN email TEXT");
            }
            if (!hasPhone) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN phone TEXT");
            }
            logger.info("Таблица users готова к использованию.");

            // Проверка таблицы masters
            try {
                rs = stmt.executeQuery("SELECT * FROM masters LIMIT 1");
            } catch (SQLException e) {
                // Таблица не существует, создаем её
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS masters (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "user_id INTEGER NOT NULL," +
                        "FOREIGN KEY (user_id) REFERENCES users(id))");
                logger.info("Таблица masters создана.");
            }

            // Проверка таблицы services
            try {
                rs = stmt.executeQuery("SELECT * FROM services LIMIT 1");
            } catch (SQLException e) {
                // Таблица не существует, создаем её и добавляем базовые услуги
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS services (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "price DECIMAL(10,2) NOT NULL)");
                
                // Добавляем базовые услуги
                String[] services = {
                    "INSERT INTO services (name, price) VALUES ('Стрижка', 25.00)",
                    "INSERT INTO services (name, price) VALUES ('Покраска', 50.00)",
                    "INSERT INTO services (name, price) VALUES ('Бритье', 20.00)",
                    "INSERT INTO services (name, price) VALUES ('Укладка', 10.00)"
                };
                
                for (String sql : services) {
                    stmt.executeUpdate(sql);
                }
                logger.info("Таблица services создана и заполнена базовыми услугами");
            }

            // Изменяем таблицу orders для поддержки множественных услуг
            stmt.executeUpdate("DROP TABLE IF EXISTS order_services");
            stmt.executeUpdate("CREATE TABLE order_services (" +
                    "order_id INTEGER," +
                    "service_id INTEGER," +
                    "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (service_id) REFERENCES services(id)," +
                    "PRIMARY KEY (order_id, service_id))");

            // Проверяем и обновляем структуру таблицы orders
            stmt.executeUpdate("DROP TABLE IF EXISTS orders");
            stmt.executeUpdate("CREATE TABLE orders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "client_name TEXT NOT NULL," +
                    "master_id INTEGER," +
                    "appointment_time TEXT NOT NULL," +
                    "status TEXT DEFAULT 'Создан'," +
                    "total_price DECIMAL(10,2) DEFAULT 0," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (master_id) REFERENCES masters(id))");

            // Создаем таблицу расписания мастеров
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS master_schedule (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "master_id INTEGER NOT NULL," +
                    "day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 0 AND 6)," +
                    "start_time TEXT NOT NULL," +
                    "end_time TEXT NOT NULL," +
                    "is_working BOOLEAN DEFAULT 1," +
                    "FOREIGN KEY (master_id) REFERENCES masters(id)," +
                    "UNIQUE(master_id, day_of_week))");

            logger.info("Структура таблиц обновлена для поддержки множественных услуг и расписания мастеров");
        } catch (SQLException e) {
            logger.severe("Ошибка при проверке структуры таблиц: " + e.getMessage());
            throw new RuntimeException("Не удалось проверить структуру таблиц", e);
        }
    }

    @Override
    public void create(User user) {
        String sql = "INSERT INTO users (login, hashed_password, role) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getLogin());
            pstmt.setString(2, user.getHashedPassword());
            pstmt.setString(3, user.getRole());
            pstmt.executeUpdate();
            logger.info("Пользователь " + user.getLogin() + " успешно добавлен.");
        } catch (SQLException e) {
            logger.severe("Ошибка добавления пользователя: " + e.getMessage());
            throw new RuntimeException("Не удалось добавить пользователя", e);
        }
    }

    @Override
    public User read(String login) {
        String sql = "SELECT * FROM users WHERE login = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User(rs.getString("login"), rs.getString("hashed_password"), rs.getString("role"));
                user.setId(rs.getInt("id"));
                return user;
            }
        } catch (SQLException e) {
            logger.severe("Ошибка чтения пользователя: " + e.getMessage());
            throw new RuntimeException("Не удалось прочитать пользователя", e);
        }
        return null;
    }

    @Override
    public void update(User user) {
        try {
            connection.setAutoCommit(false);  // Начинаем транзакцию
            
            // Обновляем информацию пользователя
            String sql = "UPDATE users SET role = ?, name = ? WHERE login = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, user.getRole());
                pstmt.setString(2, user.getName());
                pstmt.setString(3, user.getLogin());
                pstmt.executeUpdate();
                logger.info("Пользователь " + user.getLogin() + " обновлен. Роль: " + user.getRole() + ", Имя: " + user.getName());
            }

            // Получаем ID пользователя
            User fullUser = read(user.getLogin());
            if (fullUser != null) {
                // Проверяем, есть ли запись мастера для этого пользователя
                String checkSql = "SELECT id FROM masters WHERE user_id = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, fullUser.getId());
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next()) {
                        // Если запись мастера существует, обновляем имя мастера
                        String updateMasterSql = "UPDATE masters SET name = ? WHERE user_id = ?";
                        try (PreparedStatement updateMasterStmt = connection.prepareStatement(updateMasterSql)) {
                            updateMasterStmt.setString(1, user.getName());
                            updateMasterStmt.setInt(2, fullUser.getId());
                            updateMasterStmt.executeUpdate();
                            logger.info("Обновлено имя мастера для пользователя " + user.getLogin() + " на " + user.getName());
                        }
                    } else if ("master".equalsIgnoreCase(user.getRole())) {
                        // Если пользователь стал мастером, создаем новую запись
                        String insertSql = "INSERT INTO masters (name, user_id) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                            insertStmt.setString(1, user.getName());
                            insertStmt.setInt(2, fullUser.getId());
                            insertStmt.executeUpdate();
                            logger.info("Создана новая запись мастера для пользователя " + user.getLogin());
                        }
                    }
                }
            }

            connection.commit();  // Завершаем транзакцию
            logger.info("Все изменения успешно сохранены");
            
        } catch (SQLException e) {
            try {
                connection.rollback();  // Откатываем изменения в случае ошибки
            } catch (SQLException ex) {
                logger.severe("Ошибка отката транзакции: " + ex.getMessage());
            }
            logger.severe("Ошибка обновления пользователя: " + e.getMessage());
            throw new RuntimeException("Не удалось обновить пользователя", e);
        } finally {
            try {
                connection.setAutoCommit(true);  // Возвращаем автокоммит
            } catch (SQLException e) {
                logger.severe("Ошибка восстановления автокоммита: " + e.getMessage());
            }
        }
    }

    @Override
    public void delete(String login) {
        String sql = "DELETE FROM users WHERE login = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.executeUpdate();
            logger.info("Пользователь " + login + " удалён.");
        } catch (SQLException e) {
            logger.severe("Ошибка удаления пользователя: " + e.getMessage());
            throw new RuntimeException("Не удалось удалить пользователя", e);
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User(rs.getString("login"), rs.getString("hashed_password"), rs.getString("role"));
                user.setId(rs.getInt("id"));
                users.add(user);
            }
            logger.info("Получен список всех пользователей: " + users.size() + " записей.");
        } catch (SQLException e) {
            logger.severe("Ошибка получения списка пользователей: " + e.getMessage());
            throw new RuntimeException("Не удалось получить список пользователей", e);
        }
        return users;
    }

    public List<Master> getAllMasters() {
        List<Master> masters = new ArrayList<>();
        String sql = "SELECT * FROM masters";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                masters.add(new Master(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("user_id")
                ));
            }
            logger.info("Получен список всех мастеров: " + masters.size() + " записей.");
        } catch (SQLException e) {
            logger.severe("Ошибка получения списка мастеров: " + e.getMessage());
            throw new RuntimeException("Не удалось получить список мастеров", e);
        }
        return masters;
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY appointment_time DESC";
        try {
            // First, let's check if the table exists
            try (Statement checkStmt = connection.createStatement()) {
                ResultSet tables = connection.getMetaData().getTables(null, null, "orders", null);
                if (!tables.next()) {
                    logger.severe("Таблица orders не существует!");
                    return orders;
                }
            }

            // Now let's try to get the orders
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                int count = 0;
                while (rs.next()) {
                    try {
                        int id = rs.getInt("id");
                        String clientName = rs.getString("client_name");
                        int masterId = rs.getInt("master_id");
                        String appointmentTime = rs.getString("appointment_time");
                        String status = rs.getString("status");
                        double totalPrice = rs.getDouble("total_price");

                        logger.info("Чтение заказа: ID=" + id + 
                                  ", Клиент=" + clientName + 
                                  ", Мастер=" + masterId + 
                                  ", Время=" + appointmentTime + 
                                  ", Статус=" + status +
                                  ", Цена=" + totalPrice);

                        Order order = new Order(id, clientName, masterId, appointmentTime);
                        order.setStatus(status != null ? status : "Создан");
                        order.setTotalPrice(totalPrice);
                        
                        // Загружаем услуги для заказа
                        List<Service> services = getOrderServices(id);
                        order.setServices(services);
                        
                        orders.add(order);
                        count++;
                    } catch (SQLException e) {
                        logger.warning("Ошибка при обработке заказа: " + e.getMessage());
                        continue;
                    }
                }
                logger.info("Успешно прочитано " + count + " заказов из базы данных");
            }
            return orders;
        } catch (SQLException e) {
            logger.severe("Ошибка при получении списка заказов: " + e.getMessage() + "\nStackTrace: " + e.getStackTrace()[0]);
            throw new RuntimeException("Не удалось получить список заказов: " + e.getMessage(), e);
        }
    }

    public void createOrder(Order order) {
        String sql = "INSERT INTO orders (client_name, service, master_id, appointment_time, status, price) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, order.getClientName());
            pstmt.setString(2, order.getService());
            pstmt.setInt(3, order.getMasterId());
            pstmt.setString(4, order.getAppointmentTime());
            pstmt.setString(5, order.getStatus());
            pstmt.setDouble(6, order.getPrice());
            pstmt.executeUpdate();
            logger.info("Заказ успешно создан для клиента: " + order.getClientName());
        } catch (SQLException e) {
            logger.severe("Ошибка создания заказа: " + e.getMessage());
            throw new RuntimeException("Не удалось создать заказ", e);
        }
    }

    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.severe("Ошибка хеширования пароля: " + e.getMessage());
            throw new RuntimeException("Не удалось захешировать пароль", e);
        }
    }

    public String getMasterNameById(int masterId) {
        String sql = "SELECT name FROM masters WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, masterId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
            return "Неизвестный мастер";
        } catch (SQLException e) {
            logger.severe("Ошибка получения имени мастера: " + e.getMessage());
            return "Ошибка получения имени";
        }
    }

    public void deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            logger.info("Пользователь с ID " + userId + " удалён.");
        } catch (SQLException e) {
            logger.severe("Ошибка удаления пользователя: " + e.getMessage());
            throw new RuntimeException("Не удалось удалить пользователя", e);
        }
    }

    public Order getOrderById(int orderId) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("client_name"),
                    rs.getInt("master_id"),
                    rs.getString("appointment_time")
                );
                order.setStatus(rs.getString("status"));
                order.setTotalPrice(rs.getDouble("total_price"));
                
                // Загружаем услуги для заказа
                List<Service> services = getOrderServices(orderId);
                order.setServices(services);
                
                return order;
            }
            return null;
        } catch (SQLException e) {
            logger.severe("Ошибка получения заказа по ID: " + e.getMessage());
            throw new RuntimeException("Не удалось получить заказ", e);
        }
    }

    public void updateUser(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, phone = ? WHERE login = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPhone());
            pstmt.setString(4, user.getLogin());
            pstmt.executeUpdate();
            logger.info("Данные пользователя " + user.getLogin() + " успешно обновлены");
        } catch (SQLException e) {
            logger.severe("Ошибка обновления данных пользователя: " + e.getMessage());
            throw new RuntimeException("Не удалось обновить данные пользователя", e);
        }
    }

    public void updatePassword(String login, String newHashedPassword) {
        String sql = "UPDATE users SET hashed_password = ? WHERE login = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newHashedPassword);
            pstmt.setString(2, login);
            pstmt.executeUpdate();
            logger.info("Пароль пользователя " + login + " успешно обновлен");
        } catch (SQLException e) {
            logger.severe("Ошибка обновления пароля: " + e.getMessage());
            throw new RuntimeException("Не удалось обновить пароль", e);
        }
    }

    public void createMaster(Master master) {
        String sql = "INSERT INTO masters (name, user_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, master.getName());
            pstmt.setInt(2, master.getUserId());
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    master.setId(generatedKeys.getInt(1));
                }
            }
            logger.info("Мастер " + master.getName() + " успешно добавлен.");
        } catch (SQLException e) {
            logger.severe("Ошибка добавления мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось добавить мастера", e);
        }
    }

    public void updateMaster(Master master) {
        String sql = "UPDATE masters SET name = ?, user_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, master.getName());
            pstmt.setInt(2, master.getUserId());
            pstmt.setInt(3, master.getId());
            pstmt.executeUpdate();
            logger.info("Мастер " + master.getName() + " успешно обновлен.");
        } catch (SQLException e) {
            logger.severe("Ошибка обновления мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось обновить мастера", e);
        }
    }

    public void deleteMaster(int masterId) {
        String sql = "DELETE FROM masters WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, masterId);
            pstmt.executeUpdate();
            logger.info("Мастер с ID " + masterId + " успешно удален.");
        } catch (SQLException e) {
            logger.severe("Ошибка удаления мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось удалить мастера", e);
        }
    }

    public Master getMasterById(int masterId) {
        String sql = "SELECT * FROM masters WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, masterId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Master(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("user_id")
                    );
                }
            }
        } catch (SQLException e) {
            logger.severe("Ошибка получения мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось получить мастера", e);
        }
        return null;
    }

    public List<Order> getActiveOrdersForMaster(String masterLogin) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.* FROM orders o " +
                    "JOIN masters m ON o.master_id = m.id " +
                    "JOIN users u ON m.user_id = u.id " +
                    "WHERE u.login = ? AND o.status = 'Создан' " +
                    "ORDER BY o.appointment_time DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, masterLogin);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("client_name"),
                    rs.getInt("master_id"),
                    rs.getString("appointment_time")
                );
                order.setStatus(rs.getString("status"));
                order.setTotalPrice(rs.getDouble("total_price"));
                
                // Загружаем услуги для заказа
                List<Service> services = getOrderServices(order.getId());
                order.setServices(services);
                
                orders.add(order);
            }
            logger.info("Получены активные заказы для мастера " + masterLogin + ": " + orders.size() + " записей");
        } catch (SQLException e) {
            logger.severe("Ошибка получения активных заказов для мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось получить активные заказы", e);
        }
        return orders;
    }

    public List<Order> getCompletedOrdersForMaster(String masterLogin) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.* FROM orders o " +
                    "JOIN masters m ON o.master_id = m.id " +
                    "JOIN users u ON m.user_id = u.id " +
                    "WHERE u.login = ? AND o.status = 'Завершен'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, masterLogin);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("client_name"),
                    rs.getInt("master_id"),
                    rs.getString("appointment_time")
                );
                order.setStatus(rs.getString("status"));
                order.setTotalPrice(rs.getDouble("total_price"));
                
                // Загружаем услуги для заказа
                List<Service> services = getOrderServices(order.getId());
                order.setServices(services);
                
                orders.add(order);
            }
            logger.info("Получены завершенные заказы для мастера " + masterLogin + ": " + orders.size() + " записей");
        } catch (SQLException e) {
            logger.severe("Ошибка получения завершенных заказов для мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось получить завершенные заказы", e);
        }
        return orders;
    }

    public void confirmOrder(int orderId) {
        String sql = "UPDATE orders SET status = 'Подтвержден' WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            pstmt.executeUpdate();
            logger.info("Заказ #" + orderId + " подтвержден мастером");
        } catch (SQLException e) {
            logger.severe("Ошибка подтверждения заказа: " + e.getMessage());
            throw new RuntimeException("Не удалось подтвердить заказ", e);
        }
    }

    public void completeOrder(int orderId) {
        String sql = "UPDATE orders SET status = 'Завершен' WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            pstmt.executeUpdate();
            logger.info("Заказ #" + orderId + " отмечен как завершенный");
        } catch (SQLException e) {
            logger.severe("Ошибка завершения заказа: " + e.getMessage());
            throw new RuntimeException("Не удалось завершить заказ", e);
        }
    }

    public int getMasterIdByLogin(String masterLogin) {
        String sql = "SELECT m.id FROM masters m " +
                    "JOIN users u ON m.user_id = u.id " +
                    "WHERE u.login = ? AND u.role = 'master'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, masterLogin);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new RuntimeException("Мастер не найден");
        } catch (SQLException e) {
            logger.severe("Ошибка получения ID мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось получить ID мастера", e);
        }
    }

    public void createMasterForUser(String userLogin, String masterName) {
        try {
            // Получаем пользователя
            User user = read(userLogin);
            if (user == null) {
                throw new RuntimeException("Пользователь не найден");
            }

            // Проверяем, что пользователь имеет роль мастера
            if (!"master".equalsIgnoreCase(user.getRole())) {
                throw new RuntimeException("Пользователь не является мастером");
            }

            // Проверяем, не существует ли уже запись мастера для этого пользователя
            String checkSql = "SELECT id FROM masters WHERE user_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, user.getId());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    logger.info("Запись мастера для пользователя " + userLogin + " уже существует");
                    return;
                }
            }

            // Создаем запись мастера
            String sql = "INSERT INTO masters (name, user_id) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, masterName);
                pstmt.setInt(2, user.getId());
                pstmt.executeUpdate();
                logger.info("Создана запись мастера для пользователя " + userLogin);
            }

            // Обновляем имя пользователя, если оно не установлено
            if (user.getName() == null || user.getName().isEmpty()) {
                String updateUserSql = "UPDATE users SET name = ? WHERE id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateUserSql)) {
                    updateStmt.setString(1, masterName);
                    updateStmt.setInt(2, user.getId());
                    updateStmt.executeUpdate();
                    logger.info("Обновлено имя пользователя " + userLogin);
                }
            }
        } catch (SQLException e) {
            logger.severe("Ошибка создания записи мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось создать запись мастера", e);
        }
    }

    public List<Master> getAvailableMasters() {
        List<Master> masters = new ArrayList<>();
        String sql = "SELECT m.id as master_id, m.name as master_name, " +
                    "u.id as user_id, u.name as user_name, u.login " +
                    "FROM users u " +
                    "LEFT JOIN masters m ON m.user_id = u.id " +
                    "WHERE u.role = 'master'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String masterName = rs.getString("master_name");
                String userName = rs.getString("user_name");
                String login = rs.getString("login");
                Integer masterId = rs.getInt("master_id");
                if (rs.wasNull()) {
                    masterId = null;
                }
                
                // Определяем отображаемое имя в порядке приоритета:
                // 1. Имя из таблицы masters
                // 2. Имя пользователя из таблицы users
                // 3. Логин пользователя
                String displayName;
                if (masterName != null && !masterName.trim().isEmpty()) {
                    displayName = masterName;
                } else if (userName != null && !userName.trim().isEmpty()) {
                    displayName = userName;
                } else {
                    displayName = login;
                }
                
                // Если записи мастера нет, создаем её
                if (masterId == null) {
                    String insertSql = "INSERT INTO masters (name, user_id) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        insertStmt.setString(1, displayName);
                        insertStmt.setInt(2, userId);
                        insertStmt.executeUpdate();
                        
                        try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                masterId = generatedKeys.getInt(1);
                            } else {
                                throw new SQLException("Не удалось получить ID нового мастера");
                            }
                        }
                        logger.info("Создана новая запись мастера: " + displayName);
                    }
                }
                
                Master master = new Master(masterId, displayName, userId);
                masters.add(master);
                logger.info("Добавлен мастер в список: " + displayName + " (ID: " + masterId + ")");
            }
            logger.info("Получен список доступных мастеров: " + masters.size() + " записей");
        } catch (SQLException e) {
            logger.severe("Ошибка получения списка мастеров: " + e.getMessage());
            throw new RuntimeException("Не удалось получить список мастеров", e);
        }
        return masters;
    }

    public List<Order> getConfirmedOrdersForMaster(String masterLogin) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.* FROM orders o " +
                    "JOIN masters m ON o.master_id = m.id " +
                    "JOIN users u ON m.user_id = u.id " +
                    "WHERE u.login = ? AND o.status = 'Подтвержден' " +
                    "ORDER BY o.appointment_time DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, masterLogin);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("client_name"),
                    rs.getInt("master_id"),
                    rs.getString("appointment_time")
                );
                order.setStatus(rs.getString("status"));
                order.setTotalPrice(rs.getDouble("total_price"));
                
                // Загружаем услуги для заказа
                List<Service> services = getOrderServices(order.getId());
                order.setServices(services);
                
                orders.add(order);
            }
            logger.info("Получены подтвержденные заказы для мастера " + masterLogin + ": " + orders.size() + " записей");
        } catch (SQLException e) {
            logger.severe("Ошибка получения подтвержденных заказов для мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось получить подтвержденные заказы", e);
        }
        return orders;
    }

    public List<Service> getAllServices() {
        List<Service> services = new ArrayList<>();
        String sql = "SELECT * FROM services ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                services.add(new Service(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price")
                ));
            }
            logger.info("Получен список услуг: " + services.size() + " записей");
        } catch (SQLException e) {
            logger.severe("Ошибка получения списка услуг: " + e.getMessage());
            throw new RuntimeException("Не удалось получить список услуг", e);
        }
        return services;
    }

    private boolean isTimeSlotAvailable(int masterId, String appointmentTime) {
        String sql = "SELECT COUNT(*) FROM orders " +
                    "WHERE master_id = ? AND appointment_time = ? AND status != 'Отменен'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, masterId);
            pstmt.setString(2, appointmentTime);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
            return true;
        } catch (SQLException e) {
            logger.severe("Ошибка при проверке доступности времени: " + e.getMessage());
            throw new RuntimeException("Не удалось проверить доступность времени", e);
        }
    }

    public void createOrder(Order order, List<Service> services) {
        // Проверяем доступность времени перед созданием заказа
        if (!isTimeSlotAvailable(order.getMasterId(), order.getAppointmentTime())) {
            throw new RuntimeException("Выбранное время уже занято другим клиентом");
        }

        try {
            connection.setAutoCommit(false);
            
            // Вычисляем общую стоимость
            double totalPrice = services.stream()
                .mapToDouble(Service::getPrice)
                .sum();

            // Создаем заказ
            String orderSql = "INSERT INTO orders (client_name, master_id, appointment_time, total_price) VALUES (?, ?, ?, ?)";
            int orderId;
            try (PreparedStatement pstmt = connection.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, order.getClientName());
                pstmt.setInt(2, order.getMasterId());
                pstmt.setString(3, order.getAppointmentTime());
                pstmt.setDouble(4, totalPrice);
                
                // Проверяем еще раз перед вставкой (для защиты от race condition)
                if (!isTimeSlotAvailable(order.getMasterId(), order.getAppointmentTime())) {
                    throw new RuntimeException("Выбранное время было занято другим клиентом");
                }
                
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        orderId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Не удалось получить ID созданного заказа");
                    }
                }
            }

            // Добавляем услуги к заказу
            String servicesSql = "INSERT INTO order_services (order_id, service_id) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(servicesSql)) {
                for (Service service : services) {
                    pstmt.setInt(1, orderId);
                    pstmt.setInt(2, service.getId());
                    pstmt.executeUpdate();
                }
            }

            connection.commit();
            logger.info("Создан заказ #" + orderId + " на сумму " + totalPrice + " руб.");
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                logger.severe("Ошибка отката транзакции: " + ex.getMessage());
            }
            logger.severe("Ошибка создания заказа: " + e.getMessage());
            throw new RuntimeException("Не удалось создать заказ", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.severe("Ошибка восстановления автокоммита: " + e.getMessage());
            }
        }
    }

    public List<Service> getOrderServices(int orderId) {
        List<Service> services = new ArrayList<>();
        String sql = "SELECT s.* FROM services s " +
                    "JOIN order_services os ON s.id = os.service_id " +
                    "WHERE os.order_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    services.add(new Service(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price")
                    ));
                }
            }
            logger.info("Получен список услуг для заказа #" + orderId + ": " + services.size() + " услуг");
        } catch (SQLException e) {
            logger.severe("Ошибка получения услуг заказа: " + e.getMessage());
            throw new RuntimeException("Не удалось получить услуги заказа", e);
        }
        return services;
    }

    public void setMasterSchedule(int masterId, int dayOfWeek, String startTime, String endTime, boolean isWorking) {
        String sql = "INSERT OR REPLACE INTO master_schedule (master_id, day_of_week, start_time, end_time, is_working) " +
                    "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, masterId);
            pstmt.setInt(2, dayOfWeek);
            pstmt.setString(3, startTime);
            pstmt.setString(4, endTime);
            pstmt.setBoolean(5, isWorking);
            pstmt.executeUpdate();
            logger.info("Обновлено расписание мастера ID " + masterId + " на день " + dayOfWeek);
        } catch (SQLException e) {
            logger.severe("Ошибка обновления расписания мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось обновить расписание мастера", e);
        }
    }

    public static class MasterScheduleEntry {
        private final int dayOfWeek;
        private final String startTime;
        private final String endTime;
        private final boolean isWorking;

        public MasterScheduleEntry(int dayOfWeek, String startTime, String endTime, boolean isWorking) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isWorking = isWorking;
        }

        public int getDayOfWeek() { return dayOfWeek; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public boolean isWorking() { return isWorking; }
    }

    public List<MasterScheduleEntry> getMasterSchedule(int masterId) {
        List<MasterScheduleEntry> schedule = new ArrayList<>();
        String sql = "SELECT * FROM master_schedule WHERE master_id = ? ORDER BY day_of_week";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, masterId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                schedule.add(new MasterScheduleEntry(
                    rs.getInt("day_of_week"),
                    rs.getString("start_time"),
                    rs.getString("end_time"),
                    rs.getBoolean("is_working")
                ));
            }
            logger.info("Получено расписание мастера ID " + masterId + ": " + schedule.size() + " записей");
        } catch (SQLException e) {
            logger.severe("Ошибка получения расписания мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось получить расписание мастера", e);
        }
        return schedule;
    }

    public List<String> getMasterSchedule(int masterId, String date) {
        // Получаем день недели для выбранной даты
        LocalDate localDate = LocalDate.parse(date);
        int dayOfWeek = localDate.getDayOfWeek().getValue() % 7; // Преобразуем в 0-6, где 0 - воскресенье

        // Получаем расписание мастера на этот день
        String scheduleSql = "SELECT start_time, end_time, is_working FROM master_schedule " +
                           "WHERE master_id = ? AND day_of_week = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(scheduleSql)) {
            pstmt.setInt(1, masterId);
            pstmt.setInt(2, dayOfWeek);
            ResultSet rs = pstmt.executeQuery();
            
            // Если мастер не работает в этот день или нет записи в расписании
            if (!rs.next() || !rs.getBoolean("is_working")) {
                return new ArrayList<>();
            }

            // Получаем начало и конец рабочего дня
            LocalTime startTime = LocalTime.parse(rs.getString("start_time"));
            LocalTime endTime = LocalTime.parse(rs.getString("end_time"));

            // Получаем уже занятые слоты
            List<String> occupiedTimes = new ArrayList<>();
            String appointmentsSql = "SELECT appointment_time FROM orders " +
                                   "WHERE master_id = ? AND DATE(appointment_time) = ? AND status != 'Отменен'";
            
            try (PreparedStatement apptStmt = connection.prepareStatement(appointmentsSql)) {
                apptStmt.setInt(1, masterId);
                apptStmt.setString(2, date);
                ResultSet apptRs = apptStmt.executeQuery();
                
                while (apptRs.next()) {
                    occupiedTimes.add(apptRs.getString("appointment_time").split(" ")[1]);
                }
            }

            // Генерируем доступные слоты
            List<String> availableSlots = new ArrayList<>();
            LocalTime currentTime = startTime;
            
            while (!currentTime.isAfter(endTime.minusHours(1))) {
                String timeSlot = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                if (!occupiedTimes.contains(timeSlot)) {
                    availableSlots.add(timeSlot);
                }
                currentTime = currentTime.plusHours(1);
            }
            
            return availableSlots;
        } catch (SQLException e) {
            logger.severe("Ошибка при получении расписания мастера: " + e.getMessage());
            throw new RuntimeException("Не удалось получить расписание мастера", e);
        }
    }

    public List<Order> getUserOrders(String userLogin) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.* FROM orders o " +
                    "WHERE o.client_name = ? " +
                    "ORDER BY o.appointment_time DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userLogin);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("client_name"),
                    rs.getInt("master_id"),
                    rs.getString("appointment_time")
                );
                order.setStatus(rs.getString("status"));
                order.setTotalPrice(rs.getDouble("total_price"));
                
                // Загружаем услуги для заказа
                List<Service> services = getOrderServices(order.getId());
                order.setServices(services);
                
                orders.add(order);
            }
            logger.info("Получены заказы для пользователя " + userLogin + ": " + orders.size() + " записей");
        } catch (SQLException e) {
            logger.severe("Ошибка получения заказов пользователя: " + e.getMessage());
            throw new RuntimeException("Не удалось получить заказы пользователя", e);
        }
        return orders;
    }

    public List<Order> getCurrentUserOrders(String userLogin) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.* FROM orders o " +
                    "WHERE o.client_name = ? " +
                    "AND o.status IN ('Создан', 'Подтвержден') " +
                    "ORDER BY o.appointment_time ASC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userLogin);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("client_name"),
                    rs.getInt("master_id"),
                    rs.getString("appointment_time")
                );
                order.setStatus(rs.getString("status"));
                order.setTotalPrice(rs.getDouble("total_price"));
                
                // Загружаем услуги для заказа
                List<Service> services = getOrderServices(order.getId());
                order.setServices(services);
                
                orders.add(order);
            }
            logger.info("Получены текущие заказы для пользователя " + userLogin + ": " + orders.size() + " записей");
        } catch (SQLException e) {
            logger.severe("Ошибка получения текущих заказов пользователя: " + e.getMessage());
            throw new RuntimeException("Не удалось получить текущие заказы пользователя", e);
        }
        return orders;
    }
}