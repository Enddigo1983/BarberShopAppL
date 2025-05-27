package server;

import entities.User;

public interface UserDAO {
    void create(User user);
    User read(String login);
    void update(User user);
    void delete(String login);
}