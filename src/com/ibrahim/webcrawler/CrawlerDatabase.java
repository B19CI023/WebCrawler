package com.ibrahim.webcrawler;

import java.sql.*;
import java.util.Set;

public class CrawlerDatabase {
    private Connection connection;

    public CrawlerDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ SQLite JDBC driver registered");

            connection = DriverManager.getConnection("jdbc:sqlite:crawler.db");

            boolean resetSchema = true; // ⚠️ Set to false after first run

            Statement stmt = connection.createStatement();

            if (resetSchema) {
                System.out.println("⚠️ Resetting database schema...");
                stmt.execute("DROP TABLE IF EXISTS pages");
            }

            String createTable =
                "CREATE TABLE IF NOT EXISTS pages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "url TEXT, " +
                "depth INTEGER, " +
                "links TEXT, " +
                "session_id TEXT);";

            stmt.execute(createTable);
            stmt.close();
            System.out.println("🗃️ Database ready.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void savePage(String url, int depth, Set<String> links, String sessionId) {
        try {
            String linkData = String.join(",", links);
            PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO pages (url, depth, links, session_id) VALUES (?, ?, ?, ?);"
            );
            pstmt.setString(1, url);
            pstmt.setInt(2, depth);
            pstmt.setString(3, linkData);
            pstmt.setString(4, sessionId);
            pstmt.executeUpdate();
            pstmt.close();

            System.out.println("💾 Saved to DB: " + url + " [Session: " + sessionId + "]");
        } catch (SQLException e) {
            System.out.println("❌ DB Error: " + e.getMessage());
        }
    }

    public String getLastSessionId() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT session_id FROM pages ORDER BY id DESC LIMIT 1");
            if (rs.next()) return rs.getString("session_id");
        } catch (SQLException e) {
            System.out.println("❌ Failed to fetch last session ID: " + e.getMessage());
        }
        return null;
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.out.println("❌ Error closing DB: " + e.getMessage());
        }
    }
}
