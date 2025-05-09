package com.ibrahim.webcrawler;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PageRankCalculator {

    public static void showPageRankDialog(JFrame parent) {
        Map<String, Set<String>> graph = new HashMap<>();
        String sessionId = null;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:crawler.db")) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT session_id FROM pages ORDER BY id DESC LIMIT 1");
            if (rs.next()) sessionId = rs.getString("session_id");

            if (sessionId == null) {
                JOptionPane.showMessageDialog(parent, "No crawl session found.");
                return;
            }

            PreparedStatement pstmt = conn.prepareStatement("SELECT url, links FROM pages WHERE session_id = ?");
            pstmt.setString(1, sessionId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String url = rs.getString("url");
                String rawLinks = rs.getString("links");
                if (rawLinks == null || rawLinks.trim().isEmpty()) continue;
                Set<String> linkSet = Arrays.stream(rawLinks.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
                graph.put(url, linkSet);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Error loading graph: " + e.getMessage());
            return;
        }

        Map<String, Double> ranks = computePageRank(graph, 0.85, 20);
        List<Map.Entry<String, Double>> sorted = ranks.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .collect(Collectors.toList());

        JTextArea output = new JTextArea(10, 60);
        output.setFont(new Font("Monospaced", Font.PLAIN, 13));
        output.setText("Top 10 Pages by PageRank:\n\n");
        for (Map.Entry<String, Double> entry : sorted) {
            output.append(String.format("%.4f  -  %s%n", entry.getValue(), entry.getKey()));
        }

        JOptionPane.showMessageDialog(parent, new JScrollPane(output), "📈 PageRank Results", JOptionPane.INFORMATION_MESSAGE);
    }

    private static Map<String, Double> computePageRank(Map<String, Set<String>> graph, double damping, int iterations) {
        Map<String, Double> ranks = new HashMap<>();
        int N = graph.size();
        double initialRank = 1.0 / N;

        for (String node : graph.keySet()) {
            ranks.put(node, initialRank);
        }

        for (int i = 0; i < iterations; i++) {
            Map<String, Double> newRanks = new HashMap<>();

            for (String node : graph.keySet()) {
                double inboundSum = 0.0;
                for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
                    if (entry.getValue().contains(node)) {
                        int outLinks = entry.getValue().size();
                        if (outLinks > 0) {
                            inboundSum += ranks.get(entry.getKey()) / outLinks;
                        }
                    }
                }
                newRanks.put(node, (1 - damping) / N + damping * inboundSum);
            }

            ranks = newRanks;
        }

        return ranks;
    }
}
