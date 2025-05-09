package com.ibrahim.webcrawler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class HistoryViewer extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> sessionDropdown;

    public HistoryViewer() {
        setTitle("Crawl History Viewer");
        setSize(900, 450);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Table setup
        String[] columns = { "ID", "URL", "Depth", "Links", "Session ID" };
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Search field
        JTextField searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }

            private void filter() {
                String keyword = searchField.getText();
                if (keyword.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + keyword, 1)); // Filter on URL
                }
            }
        });

        // Session selector
        sessionDropdown = new JComboBox<>();
        sessionDropdown.addActionListener(e -> {
            String selectedSession = (String) sessionDropdown.getSelectedItem();
            if (selectedSession != null) loadSessionData(selectedSession);
        });

        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.add(new JLabel("🔍 Search: "), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(new JLabel("🧭 Select Session:"));
        controlPanel.add(sessionDropdown);
        topPanel.add(controlPanel, BorderLayout.SOUTH);

        // Load sessions
        loadSessionList();

        // Clear button
        JButton clearButton = new JButton("🧹 Clear History");
        clearButton.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:crawler.db")) {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("DELETE FROM pages");
                stmt.close();
                model.setRowCount(0);
                sessionDropdown.removeAllItems();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(clearButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void loadSessionList() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:crawler.db")) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT session_id FROM pages ORDER BY id DESC");

            sessionDropdown.removeAllItems();
            while (rs.next()) {
                sessionDropdown.addItem(rs.getString("session_id"));
            }

            if (sessionDropdown.getItemCount() > 0) {
                sessionDropdown.setSelectedIndex(0); // auto-load first
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSessionData(String sessionId) {
        model.setRowCount(0);
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:crawler.db")) {
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT id, url, depth, links FROM pages WHERE session_id = ?"
            );
            pstmt.setString(1, sessionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String url = rs.getString("url");
                int depth = rs.getInt("depth");
                String links = rs.getString("links");
                model.addRow(new Object[]{id, url, depth, links, sessionId});
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
