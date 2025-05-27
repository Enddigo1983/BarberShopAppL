package server;

public class HandlerFactory {
    public DatabaseHandler createHandler(String type) {
        switch (type.toLowerCase()) {
            case "service":
                return new ServiceHandler();
            case "master":
                return new MasterHandler();
            case "appointment":
                return new AppointmentHandler();
            case "user":
                return new UserHandler();
            default:
                throw new IllegalArgumentException("Unknown handler type: " + type);
        }
    }
}