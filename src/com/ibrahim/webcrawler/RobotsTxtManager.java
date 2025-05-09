package com.ibrahim.webcrawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class RobotsTxtManager {
    private final Map<String, Set<String>> disallowMap = new HashMap<>();

    public boolean isAllowed(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String domain = url.getProtocol() + "://" + url.getHost();
            String path = url.getPath();

            if (!disallowMap.containsKey(domain)) {
                fetchRobots(domain + "/robots.txt");
            }

            Set<String> disallows = disallowMap.getOrDefault(domain, Collections.emptySet());
            for (String disallowed : disallows) {
                if (path.startsWith(disallowed)) {
                    return false;
                }
            }
        } catch (Exception ignored) {}
        return true;
    }

    private void fetchRobots(String robotsUrl) {
        Set<String> disallowed = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(robotsUrl).openStream()))) {
            String line;
            boolean userAgentMatch = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.startsWith("user-agent:")) {
                    userAgentMatch = line.contains("*");
                } else if (userAgentMatch && line.startsWith("disallow:")) {
                    String dis = line.replace("disallow:", "").trim();
                    if (!dis.isEmpty()) disallowed.add(dis);
                }
            }
        } catch (Exception ignored) {}

        String domain = robotsUrl.replace("/robots.txt", "");
        disallowMap.put(domain, disallowed);
    }
}
