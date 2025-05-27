-- Create the users table
CREATE TABLE users (
                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                       name TEXT NOT NULL,
                       role TEXT NOT NULL CHECK(role IN ('client', 'master'))
);

-- Insert initial users
INSERT INTO users (name, role) VALUES ('Alice', 'master');
INSERT INTO users (name, role) VALUES ('Bob', 'client');

-- Create the services table
CREATE TABLE services (
                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                          name TEXT NOT NULL
);

-- Insert initial services
INSERT INTO services (name) VALUES ('Haircut');
INSERT INTO services (name) VALUES ('Shave');
INSERT INTO services (name) VALUES ('Beard Trim');

-- Create the masters table
CREATE TABLE masters (
                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                         name TEXT NOT NULL
);

-- Insert initial masters
INSERT INTO masters (name) VALUES ('Alice');
INSERT INTO masters (name) VALUES ('Charlie');

-- Create the appointments table
CREATE TABLE appointments (
                              id INTEGER PRIMARY KEY AUTOINCREMENT,
                              client_name TEXT NOT NULL,
                              service_id INTEGER NOT NULL,
                              master_id INTEGER NOT NULL,
                              FOREIGN KEY (service_id) REFERENCES services(id),
                              FOREIGN KEY (master_id) REFERENCES masters(id)
);

-- Insert an initial appointment (client Bob, service Haircut, master Alice)
INSERT INTO appointments (client_name, service_id, master_id) VALUES ('Bob', 1, 1);