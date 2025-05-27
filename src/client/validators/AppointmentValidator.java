package client.validators;

import entities.Master;
import java.util.List;

public class AppointmentValidator {
    public static ValidationResult validateAppointment(String clientName, String service, String masterName, String appointmentTime) {
        if (clientName.isEmpty() || service == null || masterName == null || appointmentTime.isEmpty()) {
            return new ValidationResult(false, "Все поля должны быть заполнены!");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateMasterId(String masterName, List<Master> masters) {
        int masterId = -1;
        for (Master master : masters) {
            if (master.getName().equals(masterName)) {
                masterId = master.getId();
                break;
            }
        }
        if (masterId == -1) {
            return new ValidationResult(false, "Мастер не найден!");
        }
        return new ValidationResult(true, String.valueOf(masterId));
    }

    public static class ValidationResult {
        private final boolean isValid;
        private final String message;

        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getMessage() {
            return message;
        }
    }
} 