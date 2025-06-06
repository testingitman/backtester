package com.backtester.service;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class RssService {
    private static final Logger logger = LoggerFactory.getLogger(RssService.class);

    public SyndFeed fetchFeed(String rssUrl) {
        try {
            logger.debug("Fetching RSS feed from {}", rssUrl);
            URL url = new URL(rssUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (XmlReader reader = new XmlReader(conn)) {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(reader);
                logger.debug("Parsed {} entries", feed.getEntries().size());
                return feed;
            }
        } catch (Exception e) {
            logger.error("Failed to fetch or parse RSS feed", e);
            return null;
        }
    }
}
