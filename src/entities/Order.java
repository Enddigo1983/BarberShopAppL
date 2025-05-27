package entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private String clientName;
    private int masterId;
    private String appointmentTime;
    private String status;
    private double totalPrice;
    private List<Service> services;
    private LocalDateTime createdAt;

    public Order(int id, String clientName, int masterId, String appointmentTime) {
        this.id = id;
        this.clientName = clientName;
        this.masterId = masterId;
        this.appointmentTime = appointmentTime;
        this.status = "Создан";
        this.services = new ArrayList<>();
        this.totalPrice = 0.0;
        this.createdAt = LocalDateTime.now();
    }

    public Order(int i, String johnDoe, String clientName, int masterId, String appointmentTime) {
        this(-1, clientName, masterId, appointmentTime);
    }

    public int getId() {
        return id;
    }

    public String getClientName() {
        return clientName;
    }

    public int getMasterId() {
        return masterId;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
        this.totalPrice = services.stream()
            .mapToDouble(Service::getPrice)
            .sum();
    }

    public void addService(Service service) {
        this.services.add(service);
        this.totalPrice += service.getPrice();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Заказ #").append(id)
          .append("\nКлиент: ").append(clientName)
          .append("\nВремя записи: ").append(appointmentTime)
          .append("\nСтатус: ").append(status)
          .append("\nУслуги:");
        
        for (Service service : services) {
            sb.append("\n- ").append(service.toString());
        }
        
        sb.append("\nИтоговая стоимость: ").append(totalPrice).append(" руб.");
        return sb.toString();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getService() {
        return "";
    }

    public double getPrice() {
        return 0;
    }
}