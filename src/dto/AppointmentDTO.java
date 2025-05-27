package dto;

import java.io.Serializable;

public class AppointmentDTO implements Serializable {
    private String clientName;
    private int serviceId;
    private int masterId;
    private String appointmentDate;

    public AppointmentDTO(String clientName, int serviceId, int masterId, String appointmentDate) {
        this.clientName = clientName;
        this.serviceId = serviceId;
        this.masterId = masterId;
        this.appointmentDate = appointmentDate;
    }

    public String getClientName() {
        return clientName;
    }

    public int getServiceId() {
        return serviceId;
    }

    public int getMasterId() {
        return masterId;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }
}