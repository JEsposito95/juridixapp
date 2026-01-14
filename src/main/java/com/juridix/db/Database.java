package com.juridix.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class Database {

    private static final String URL = "jdbc:sqlite:juridix.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL);
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to SQLite", e);
        }
    }
}
