package com.ibrahim.webcrawler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;

public class WebGraphWindow extends JFrame {
    private final Map<String, Set<String>> graph;
    private final Map<String, Integer> depthMap;

    public WebGraphWindow(Map<String, Set<String>> graph) {
        this.graph = graph;
        this.depthMap = loadDepths();

        setTitle("Web Link Structure (Current Session)");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        GraphPanel panel = new GraphPanel(graph, depthMap);

        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("🔍 Search");
        JButton zoomInBtn = new JButton("➕ Zoom In");
        JButton zoomOutBtn = new JButton("➖ Zoom Out");
        JButton exportBtn = new JButton("📤 Export PNG");

        searchBtn.addActionListener(e -> panel.setSearchQuery(searchField.getText().trim()));
        zoomInBtn.addActionListener(e -> panel.zoom(1.2));
        zoomOutBtn.addActionListener(e -> panel.zoom(0.8));
        exportBtn.addActionListener(e -> exportImage(panel));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Search URL:"));
        controlPanel.add(searchField);
        controlPanel.add(searchBtn);
        controlPanel.add(zoomInBtn);
        controlPanel.add(zoomOutBtn);
        controlPanel.add(exportBtn);

        add(controlPanel, BorderLayout.NORTH);
        add(new JScrollPane(panel), BorderLayout.CENTER);
        add(panel.getLegendPanel(), BorderLayout.SOUTH);

        setVisible(true);
    }

    private void exportImage(JPanel panel) {
        try {
            BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            panel.paint(g2);
            g2.dispose();

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Graph Image");
            chooser.setSelectedFile(new File("web_graph.png"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                ImageIO.write(image, "png", file);
                JOptionPane.showMessageDialog(this, "✅ Graph saved to:\n" + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Failed to export: " + ex.getMessage());
        }
    }

    private Map<String, Integer> loadDepths() {
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:crawler.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT url, depth FROM pages")) {
            while (rs.next()) {
                map.put(rs.getString("url"), rs.getInt("depth"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    static class GraphPanel extends JPanel {
        private final Map<String, Set<String>> graph;
        private final Map<String, Integer> depthMap;
        private final Map<String, Point> positions = new HashMap<>();
        private final int nodeRadius = 10;
        private double zoomFactor = 1.0;
        private String searchQuery = "";
        private final Color[] palette = {
            new Color(255, 99, 71),   // Tomato
            new Color(60, 179, 113),  // MediumSeaGreen
            new Color(100, 149, 237), // CornflowerBlue
            new Color(255, 215, 0),   // Gold
            new Color(186, 85, 211)   // MediumOrchid
        };

        public GraphPanel(Map<String, Set<String>> graph, Map<String, Integer> depthMap) {
            this.graph = graph;
            this.depthMap = depthMap;
            setPreferredSize(new Dimension(1800, 1200));
            setBackground(Color.WHITE);
            layoutNodes();
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        public void setSearchQuery(String query) {
            this.searchQuery = query.toLowerCase();
            repaint();
        }

        public void zoom(double factor) {
            zoomFactor *= factor;
            layoutNodes();
            revalidate();
            repaint();
        }

        private void layoutNodes() {
            positions.clear();
            int cols = (int) Math.ceil(Math.sqrt(graph.size()));
            int spacing = (int) (150 * zoomFactor);
            int row = 0, col = 0;

            for (String node : graph.keySet()) {
                int x = 100 + col * spacing;
                int y = 100 + row * spacing;
                positions.put(node, new Point(x, y));
                col++;
                if (col >= cols) {
                    col = 0;
                    row++;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw edges
            g2.setColor(new Color(0, 102, 204));
            for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
                Point from = positions.get(entry.getKey());
                if (from == null) continue;
                for (String toNode : entry.getValue()) {
                    Point to = positions.get(toNode);
                    if (to != null) {
                        g2.drawLine(from.x, from.y, to.x, to.y);
                    }
                }
            }

            // Draw nodes
            for (Map.Entry<String, Point> entry : positions.entrySet()) {
                String url = entry.getKey();
                Point p = entry.getValue();
                int depth = depthMap.getOrDefault(url, 0);
                boolean isMatch = !searchQuery.isEmpty() && url.toLowerCase().contains(searchQuery);

                g2.setColor(isMatch ? Color.RED : getColorByDepth(depth));
                g2.fillOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);

                g2.setColor(Color.BLACK);
                g2.drawString(getShortLabel(url), p.x + 12, p.y + 5);
            }
        }

        private Color getColorByDepth(int depth) {
            return palette[depth % palette.length];
        }

        private String getShortLabel(String url) {
            try {
                java.net.URL parsed = new java.net.URL(url);
                String host = parsed.getHost().replace("www.", "");
                String path = parsed.getPath();
                return path.length() > 1 ? host + "/..." : host;
            } catch (Exception e) {
                return url.length() > 30 ? url.substring(0, 27) + "..." : url;
            }
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            for (Map.Entry<String, Point> entry : positions.entrySet()) {
                Point p = entry.getValue();
                if (p.distance(e.getPoint()) <= nodeRadius + 3) {
                    return "<html><b>Full URL:</b><br>" + entry.getKey() + "</html>";
                }
            }
            return null;
        }

        public JPanel getLegendPanel() {
            JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT));
            legend.setBackground(Color.WHITE);
            legend.setBorder(BorderFactory.createTitledBorder("Depth Color Legend"));
            for (int i = 0; i < palette.length; i++) {
                JPanel colorBox = new JPanel();
                colorBox.setBackground(palette[i]);
                colorBox.setPreferredSize(new Dimension(20, 20));
                legend.add(new JLabel("Depth " + i + ":"));
                legend.add(colorBox);
            }
            return legend;
        }
    }
}
