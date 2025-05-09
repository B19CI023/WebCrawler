package com.ibrahim.webcrawler;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class WebCrawler {
    private final Set<String> visited = new HashSet<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final CrawlerDatabase database = new CrawlerDatabase();
    private final String sessionId;
    private Consumer<String> logCallback;
    private RobotsTxtManager robotsManager = new RobotsTxtManager();

    private boolean sameDomainOnly = false;
    private int maxPages = Integer.MAX_VALUE;
    private int pagesVisited = 0;
    private Set<String> keywords = new HashSet<>();
    private String baseDomain = "";

    private GraphUpdateListener graphUpdateListener;

    public WebCrawler() {
        this.sessionId = UUID.randomUUID().toString();
        log("🆕 Session ID: " + sessionId);
    }
    
    public interface GraphUpdate {
        void onPageCrawled(String url, Set<String> links, int depth);
    }


    public void setLogCallback(Consumer<String> callback) {
        this.logCallback = callback;
    }

    public void setSameDomainOnly(boolean flag) {
        this.sameDomainOnly = flag;
    }

    public void setMaxPages(int limit) {
        this.maxPages = limit;
    }

    public void setKeywords(Set<String> keywordSet) {
        this.keywords = keywordSet;
    }

    public void setBaseDomain(String url) {
        try {
            baseDomain = new java.net.URL(url).getHost().replace("www.", "");
        } catch (Exception e) {
            baseDomain = "";
        }
    }

    public synchronized boolean isVisited(String url) {
        return visited.contains(url);
    }

    public synchronized boolean shouldCrawl() {
        return pagesVisited < maxPages;
    }

    public synchronized void markVisited(String url) {
        if (!visited.contains(url)) {
            visited.add(url);
            pagesVisited++;
            log("👣 Visiting: " + url);
        }
    }

    public void submitTask(CrawlTask task) {
        executor.submit(task);
    }

    public void startCrawl(String startUrl, int depth) {
        setBaseDomain(startUrl);
        log("🚀 Starting crawl at " + startUrl + " to depth " + depth);
        submitTask(new CrawlTask(startUrl, depth, this));
    }

    public void storePage(String url, int depth, Set<String> links) {
        database.savePage(url, depth, links, sessionId);
        log("📄 Stored: " + url + " with " + links.size() + " links.");

        if (graphUpdateListener != null) {
            graphUpdateListener.addNode(url, links, depth);
        }
    }

    public Set<String> getVisitedUrls() {
        return visited;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isSameDomainOnly() {
        return sameDomainOnly;
    }

    public String getBaseDomain() {
        return baseDomain;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void shutdown() {
        executor.shutdown();
        database.close();
        log("🛑 Crawler shutdown.");
    }

    public void log(String msg) {
        System.out.println(msg);
        if (logCallback != null) logCallback.accept(msg);
    }

    public RobotsTxtManager getRobotsManager() {
        return robotsManager;
    }

    public interface GraphUpdateListener {
        void addNode(String from, Set<String> toLinks, int depth);
    }

    public void setGraphUpdateListener(GraphUpdateListener listener) {
        this.graphUpdateListener = listener;
    }
}
