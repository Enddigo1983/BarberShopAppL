package test;

import entities.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {
    private Order order;
    private static final int TEST_ID = 1;
    private static final String TEST_CLIENT = "John Doe";
    private static final String TEST_SERVICE = "Haircut";
    private static final int TEST_MASTER_ID = 1;
    private static final String TEST_APPOINTMENT_TIME = "2024-03-20 14:00";
    private static final String TEST_STATUS = "Создан";

    @BeforeEach
    void setUp() {
        order = new Order(TEST_ID, TEST_CLIENT, TEST_SERVICE, TEST_MASTER_ID, TEST_APPOINTMENT_TIME);
    }

    @Test
    void testOrderCreation() {
        assertNotNull(order);
        assertEquals(TEST_ID, order.getId());
        assertEquals(TEST_CLIENT, order.getClientName());
        assertEquals(TEST_SERVICE, order.getService());
        assertEquals(TEST_MASTER_ID, order.getMasterId());
        assertEquals(TEST_APPOINTMENT_TIME, order.getAppointmentTime());
    }

    @Test
    void testDefaultStatus() {
        assertEquals(TEST_STATUS, order.getStatus());
    }

    @Test
    void testStatusChange() {
        String newStatus = "Выполнен";
        order.setStatus(newStatus);
        assertEquals(newStatus, order.getStatus());
    }

    @Test
    void testCreatedAtNotNull() {
        assertNotNull(order.getCreatedAt());
    }

    @Test
    void testCreatedAtIsBeforeNow() {
        LocalDateTime now = LocalDateTime.now();
        assertTrue(order.getCreatedAt().isBefore(now) || order.getCreatedAt().isEqual(now));
    }

    @Test
    void testAppointmentTimeFormat() {
        // Проверяем, что время записи соответствует ожидаемому формату
        assertTrue(order.getAppointmentTime().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"));
    }
} 