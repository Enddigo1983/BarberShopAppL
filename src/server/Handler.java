package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface Handler {
    void setStreams(ObjectOutputStream out, ObjectInputStream in);
    void handle(String command) throws Exception;
}