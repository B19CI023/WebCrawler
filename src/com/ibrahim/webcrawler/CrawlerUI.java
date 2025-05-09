package com.ibrahim.webcrawler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CrawlerUI extends JFrame {

	private JTextField urlField, depthField, maxPagesField, keywordsField;
	private JCheckBox sameDomainCheckbox;
	private JButton startButton, historyButton, visualizeButton, exportButton, pagerankButton;
	private JLabel statusLabel;
	private JTextArea logArea;
	private JTextArea statsArea;

	private Set<String> currentCrawlUrls = new HashSet<>();

	public CrawlerUI() {
	    setTitle("🌐 Web Crawler Dashboard");
	    setSize(900, 720);
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    setLocationRelativeTo(null);
	    setLayout(new BorderLayout(10, 10));
	    getContentPane().setBackground(Color.WHITE);

	    Font labelFont = new Font("SansSerif", Font.BOLD, 15);
	    Font fieldFont = new Font("SansSerif", Font.PLAIN, 14);
	    Font buttonFont = new Font("SansSerif", Font.PLAIN, 14);

	    // === Input Panel ===
	    JPanel formPanel = new JPanel(new GridLayout(5, 2, 12, 10));
	    formPanel.setBorder(BorderFactory.createTitledBorder("Crawl Configuration"));
	    formPanel.setBackground(new Color(245, 245, 245));

	    urlField = new JTextField("https://example.com");
	    depthField = new JTextField("2");
	    maxPagesField = new JTextField("100");
	    keywordsField = new JTextField();
	    sameDomainCheckbox = new JCheckBox("Restrict to same domain only");

	    formPanel.add(new JLabel("🔗 Start URL:"));
	    formPanel.add(urlField);
	    formPanel.add(new JLabel("🔍 Crawl Depth:"));
	    formPanel.add(depthField);
	    formPanel.add(new JLabel("📈 Max Pages:"));
	    formPanel.add(maxPagesField);
	    formPanel.add(new JLabel("🧠 Keywords (comma-separated):"));
	    formPanel.add(keywordsField);
	    formPanel.add(sameDomainCheckbox);

	    // === Buttons ===
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
	    buttonPanel.setBackground(Color.WHITE);
	    startButton = createStyledButton("🚀 Start Crawl", buttonFont);
	    historyButton = createStyledButton("📚 History", buttonFont);
	    visualizeButton = createStyledButton("📊 Visualize", buttonFont);
	    exportButton = createStyledButton("💾 Export CSV", buttonFont);
	    pagerankButton = createStyledButton("⭐ Show PageRank", buttonFont);
	    buttonPanel.add(startButton);
	    buttonPanel.add(historyButton);
	    buttonPanel.add(visualizeButton);
	    buttonPanel.add(exportButton);
	    buttonPanel.add(pagerankButton);

	    // === Crawl Log ===
	    logArea = new JTextArea();
	    logArea.setEditable(false);
	    logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
	    JScrollPane logScroll = new JScrollPane(logArea);
	    logScroll.setBorder(BorderFactory.createTitledBorder("Live Crawl Log"));
	    logScroll.setPreferredSize(new Dimension(600, 140));

	    // === Crawl Stats ===
	    statsArea = new JTextArea();
	    statsArea.setEditable(false);
	    statsArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
	    JScrollPane statsScroll = new JScrollPane(statsArea);
	    statsScroll.setBorder(BorderFactory.createTitledBorder("Crawl Summary"));
	    statsScroll.setPreferredSize(new Dimension(600, 100));

	    // === Status Label ===
	    statusLabel = new JLabel("✅ Ready", SwingConstants.CENTER);
	    statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
	    statusLabel.setForeground(new Color(0, 128, 0));

	    // === Layout ===
	    add(formPanel, BorderLayout.NORTH);
	    add(buttonPanel, BorderLayout.CENTER);
	    add(logScroll, BorderLayout.SOUTH);
	    add(statsScroll, BorderLayout.EAST);
	    add(statusLabel, BorderLayout.PAGE_END);

	    // === Crawl Action ===
	    startButton.addActionListener((ActionEvent e) -> {
	        String url = urlField.getText().trim();
	        int depth, maxPages;

	        try {
	            new java.net.URL(url);
	            depth = Integer.parseInt(depthField.getText().trim());
	            maxPages = Integer.parseInt(maxPagesField.getText().trim());
	        } catch (Exception ex) {
	            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
	            return;
	        }

	        Set<String> keywords = Arrays.stream(keywordsField.getText().split(","))
	                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

	        startButton.setEnabled(false);
	        logArea.setText("");
	        statsArea.setText("");
	        statusLabel.setText("⏳ Crawling in progress...");

	        new Thread(() -> {
	            WebCrawler crawler = new WebCrawler();

	            crawler.setLogCallback(msg -> SwingUtilities.invokeLater(() -> {
	                logArea.append(msg + "\n");
	                logArea.setCaretPosition(logArea.getDocument().getLength());
	            }));

	            crawler.setSameDomainOnly(sameDomainCheckbox.isSelected());
	            crawler.setMaxPages(maxPages);
	            crawler.setKeywords(keywords);
	            crawler.startCrawl(url, depth);

	            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

	            currentCrawlUrls = crawler.getVisitedUrls();
	            crawler.shutdown();

	            SwingUtilities.invokeLater(() -> {
	                statusLabel.setText("✅ Crawl completed.");
	                startButton.setEnabled(true);
	                showSummaryStats();
	            });
	        }).start();
	    });

	    // === History ===
	    historyButton.addActionListener(e -> new HistoryViewer());

	    // === Visualize ===
	    visualizeButton.addActionListener(e -> visualizeGraph());

	    // === Export ===
	    exportButton.addActionListener(e -> exportCSV());

	    // === PageRank ===
	    pagerankButton.addActionListener(e -> {
	        PageRankCalculator.showPageRankDialog(this);
	    });

	    setVisible(true);
	}

	private void showSummaryStats() {
	    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:crawler.db")) {
	        Statement stmt = conn.createStatement();
	        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM pages");
	        int totalPages = rs.next() ? rs.getInt("total") : 0;
	        rs.close();

	        rs = stmt.executeQuery("SELECT COUNT(DISTINCT substr(url, instr(url, '//')+2, instr(substr(url, instr(url, '//')+2), '/')-1)) AS domains FROM pages");
	        int domainCount = rs.next() ? rs.getInt("domains") : 0;
	        rs.close();

	        rs = stmt.executeQuery("SELECT url, links FROM pages");
	        int internal = 0, external = 0;
	        while (rs.next()) {
	            String pageUrl = rs.getString("url");
	            String links = rs.getString("links");
	            if (links == null) continue;
	            String domain = new java.net.URL(pageUrl).getHost();
	            for (String link : links.split(",")) {
	                if (link.contains(domain)) internal++;
	                else external++;
	            }
	        }

	        statsArea.setText(String.format("📄 Total Pages Crawled: %d%n🌐 Unique Domains: %d%n🔗 Internal Links: %d%n🔗 External Links: %d",
	                totalPages, domainCount, internal, external));

	    } catch (Exception e) {
	        statsArea.setText("⚠️ Failed to load crawl stats.");
	    }
	}

	private void visualizeGraph() {
	    Map<String, Set<String>> graph = new HashMap<>();
	    String latestSession = null;

	    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:crawler.db")) {
	        Statement stmt = conn.createStatement();
	        ResultSet rs = stmt.executeQuery("SELECT session_id FROM pages ORDER BY id DESC LIMIT 1");
	        if (rs.next()) latestSession = rs.getString("session_id");
	        rs.close();

	        if (latestSession == null) {
	            JOptionPane.showMessageDialog(this, "No crawl data available.");
	            return;
	        }

	        PreparedStatement pstmt = conn.prepareStatement("SELECT url, links FROM pages WHERE session_id = ?");
	        pstmt.setString(1, latestSession);
	        rs = pstmt.executeQuery();

	        while (rs.next()) {
	            String url = rs.getString("url");
	            String rawLinks = rs.getString("links");
	            if (rawLinks == null || rawLinks.trim().isEmpty()) continue;

	            Set<String> linkSet = Arrays.stream(rawLinks.split(","))
	                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
	            if (!linkSet.isEmpty()) graph.put(url, linkSet);
	        }
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }

	    new WebGraphWindow(graph);
	}

	private void exportCSV() {
	    String latestSession = null;

	    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:crawler.db")) {
	        Statement stmt = conn.createStatement();
	        ResultSet rs = stmt.executeQuery("SELECT session_id FROM pages ORDER BY id DESC LIMIT 1");
	        if (rs.next()) latestSession = rs.getString("session_id");

	        if (latestSession == null) {
	            JOptionPane.showMessageDialog(this, "No session data found.");
	            return;
	        }

	        JFileChooser chooser = new JFileChooser();
	        chooser.setDialogTitle("Save Crawl Report");
	        chooser.setSelectedFile(new java.io.File("crawl_export.csv"));
	        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

	        String path = chooser.getSelectedFile().getAbsolutePath();
	        try (PreparedStatement pstmt = conn.prepareStatement("SELECT url, depth, links FROM pages WHERE session_id = ?");
	             java.io.PrintWriter writer = new java.io.PrintWriter(path)) {

	            pstmt.setString(1, latestSession);
	            ResultSet data = pstmt.executeQuery();
	            writer.println("url,depth,num_links,links");

	            while (data.next()) {
	                String url = data.getString("url").replace(",", " ");
	                int depth = data.getInt("depth");
	                String links = data.getString("links");
	                int count = (links == null || links.trim().isEmpty()) ? 0 : links.split(",").length;
	                writer.printf("\"%s\",%d,%d,\"%s\"%n", url, depth, count, links == null ? "" : links);
	            }

	            JOptionPane.showMessageDialog(this, "Exported to " + path);

	        } catch (Exception ex) {
	            ex.printStackTrace();
	            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
	        }

	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	}

	private JButton createStyledButton(String text, Font font) {
	    JButton btn = new JButton(text);
	    btn.setFont(font);
	    btn.setFocusPainted(false);
	    btn.setBackground(new Color(0, 120, 215));
	    btn.setForeground(Color.WHITE);
	    btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	    btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
	    return btn;
	}

}