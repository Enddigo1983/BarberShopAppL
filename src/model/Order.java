public class Order {
    private int id;
    private String clientName;
    private String service;
    private int masterId;
    private String appointmentTime;
    private String status;  // Создан, Подтвержден, Завершен

    public Order(int id, String clientName, String service, int masterId, String appointmentTime) {
        this.id = id;
        this.clientName = clientName;
        this.service = service;
        this.masterId = masterId;
        this.appointmentTime = appointmentTime;
        this.status = "Создан";  // По умолчанию
    }

    // Геттеры
    public int getId() { return id; }
    public String getClientName() { return clientName; }
    public String getService() { return service; }
    public int getMasterId() { return masterId; }
    public String getAppointmentTime() { return appointmentTime; }
    public String getStatus() { return status; }

    // Сеттеры
    public void setId(int id) { this.id = id; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setService(String service) { this.service = service; }
    public void setMasterId(int masterId) { this.masterId = masterId; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Order{id=%d, client='%s', service='%s', masterId=%d, time='%s', status='%s'}",
            id, clientName, service, masterId, appointmentTime, status);
    }
} 