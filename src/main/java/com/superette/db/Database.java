package com.superette.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Database {
    private static String url;

    public static void init(String dbPath) {
        try {
            Path p = Paths.get(dbPath);
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            url = "jdbc:sqlite:" + p.toString();
            try (Connection conn = getConnection()) {
                String schema = readResource("/schema.sql");
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(schema);
                }
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Init DB failed", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = Database.class.getResourceAsStream(path)) {
            if (in == null)
                throw new IOException("Resource not found: " + path);
            try (Scanner s = new Scanner(in, StandardCharsets.UTF_8)) {
                s.useDelimiter("\\A");
                return s.hasNext() ? s.next() : "";
            }
        }
    }
}
