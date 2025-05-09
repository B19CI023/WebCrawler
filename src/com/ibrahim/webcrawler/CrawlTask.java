package com.ibrahim.webcrawler;

import java.util.Set;

public class CrawlTask implements Runnable {
    private final String url;
    private final int depth;
    private final WebCrawler controller;

    public CrawlTask(String url, int depth, WebCrawler controller) {
        this.url = url;
        this.depth = depth;
        this.controller = controller;
    }

    @Override
    public void run() {
        if (depth <= 0 || controller.isVisited(url)) return;

        // ✅ Respect robots.txt
        if (!controller.getRobotsManager().isAllowed(url)) {
            controller.log("🚫 Blocked by robots.txt: " + url);
            return;
        }

        controller.markVisited(url);

        Set<String> links = PageFetcher.fetchLinks(url);
        controller.storePage(url, depth, links);

        for (String link : links) {
            controller.submitTask(new CrawlTask(link, depth - 1, controller));
        }
    }
}
