package entities;

public class User {
    private int id;
    private String login;
    private String hashedPassword;
    private String role;
    private String name;
    private String email;
    private String phone;

    public User(String login, String hashedPassword, String role) {
        this.login = login;
        this.hashedPassword = hashedPassword;
        this.role = role;
    }

    public User(int id, String login, String role) {
        this.id = id;
        this.login = login;
        this.role = role;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLogin() { return login; }
    public String getHashedPassword() { return hashedPassword; }
    public String getRole() { return role; }
    public String getName() { return name != null ? name : login; }
    public String getEmail() { return email != null ? email : ""; }
    public String getPhone() { return phone != null ? phone : ""; }
    
    public void setPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}