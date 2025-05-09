package com.ibrahim.webcrawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageFetcher {

    public static Set<String> fetchLinks(String urlString) {
        Set<String> links = new HashSet<>();

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int status = connection.getResponseCode();
            if (status != 200) {
                System.out.println("❌ Failed to fetch: " + urlString + " [Status: " + status + "]");
                return links;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder html = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line);
            }
            reader.close();

            Pattern pattern = Pattern.compile("href\\s*=\\s*\"(http[s]?://[^\"]+)\"", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html.toString());
            while (matcher.find()) {
                links.add(matcher.group(1));
            }

            System.out.println("✅ Found " + links.size() + " links in: " + urlString);

        } catch (Exception e) {
            System.out.println("⚠️ Error fetching " + urlString + ": " + e.getMessage());
        }

        return links;
    }
}
