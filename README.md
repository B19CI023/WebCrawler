# 🌐 WebCrawler – Java GUI Crawler with Visualization, Analytics & PageRank

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![SQLite](https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

> An advanced Java-based web crawler built with Swing GUI and SQLite backend. Visualizes link graphs, computes PageRank, and provides crawl analytics in real-time.

---

## 🖥️ Project Overview

This application is a **desktop GUI web crawler** that:
- Crawls starting from a given URL
- Obeys crawl depth, keyword filters, max page limits
- Visualizes the website graph (with zoom, search, export)
- Displays crawl stats (pages, domains, link types)
- Computes **PageRank** from the link graph
- Respects `robots.txt` constraints

---

## ✨ Features

✅ Swing GUI (Eclipse compatible)  
✅ Live crawl logs  
✅ SQLite-backed data persistence  
✅ Graph-based link visualization  
✅ PageRank computation  
✅ Summary statistics panel  
✅ Export crawl results to CSV or PNG  
✅ Zoom/search in graph viewer  
✅ `robots.txt` blocking compliance

---

## 🧰 Tech Stack

| Tech | Purpose |
|------|---------|
| **Java (JDK 8+)** | Core language |
| **Swing** | GUI framework |
| **SQLite (via JDBC)** | Data storage |
| **JGraph-style drawing** | Custom visualization |
| **Eclipse IDE** | Project development |

---

## 🚀 Getting Started

### Prerequisites
- Java 8+ installed
- Eclipse (or any IDE)
- [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) JAR file in `/lib`

### Running the Project

1. **Clone this repo** or download the ZIP  
2. Open in Eclipse → File > Import > Existing Project  
3. Download these 2 files: `sqlite-jdbc.jar` and `jsoup-1.20.1.jar`
4. **Add JDBC JAR** to your build path:
   - Right-click project → Build Path → Configure → Add External JARs → choose `sqlite-jdbc.jar` and `jsoup-1.20.1.jar`
5. **Run** the `Main.java` file (right-click → Run As → Java Application)

---

## 📤 Export Options

- CSV: Crawled page details
- PNG: Link graph export
---

## 📊 PageRank Support

Each node in the crawl graph is scored using **PageRank**, highlighting important or well-connected URLs.

---


