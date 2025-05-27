package client.validators;

import java.util.regex.Pattern;

public class UserValidator {
    private static final int MIN_LOGIN_LENGTH = 3;
    private static final int MAX_LOGIN_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&_-]+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[А-Яа-яЁёA-Za-z\\s-]+$");
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}$");

    public static ValidationResult validateLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            return new ValidationResult(false, "Логин не может быть пустым");
        }
        if (login.length() < MIN_LOGIN_LENGTH) {
            return new ValidationResult(false, "Логин должен содержать минимум " + MIN_LOGIN_LENGTH + " символа");
        }
        if (login.length() > MAX_LOGIN_LENGTH) {
            return new ValidationResult(false, "Логин не может быть длиннее " + MAX_LOGIN_LENGTH + " символов");
        }
        if (!LOGIN_PATTERN.matcher(login).matches()) {
            return new ValidationResult(false, "Логин может содержать только буквы, цифры, дефис и подчеркивание");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return new ValidationResult(false, "Пароль не может быть пустым");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new ValidationResult(false, "Пароль должен содержать минимум " + MIN_PASSWORD_LENGTH + " символов");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return new ValidationResult(false, "Пароль должен содержать хотя бы одну букву и одну цифру");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new ValidationResult(false, "Имя не может быть пустым");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return new ValidationResult(false, "Имя может содержать только буквы, пробел и дефис");
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateDateTime(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            return new ValidationResult(false, "Дата и время не могут быть пустыми");
        }
        if (!DATE_TIME_PATTERN.matcher(dateTime).matches()) {
            return new ValidationResult(false, "Неверный формат даты и времени. Используйте формат: гггг-ММ-дд ЧЧ:мм");
        }
        try {
            // Проверка корректности даты и времени
            String[] parts = dateTime.split(" ");
            String[] dateParts = parts[0].split("-");
            String[] timeParts = parts[1].split(":");
            
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int day = Integer.parseInt(dateParts[2]);
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            if (year < 2024 || year > 2025) {
                return new ValidationResult(false, "Год должен быть 2024 или 2025");
            }
            if (month < 1 || month > 12) {
                return new ValidationResult(false, "Месяц должен быть от 1 до 12");
            }
            if (day < 1 || day > 31) {
                return new ValidationResult(false, "День должен быть от 1 до 31");
            }
            if (hour < 9 || hour > 20) {
                return new ValidationResult(false, "Время работы с 9:00 до 20:00");
            }
            if (minute < 0 || minute > 59) {
                return new ValidationResult(false, "Минуты должны быть от 0 до 59");
            }
        } catch (Exception e) {
            return new ValidationResult(false, "Неверный формат даты и времени");
        }
        return new ValidationResult(true, "");
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