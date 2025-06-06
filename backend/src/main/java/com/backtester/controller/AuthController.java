package com.backtester.controller;

import com.backtester.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        String apiKey = Config.get("kite_api_key");
        String redirect = Config.get("kite_redirect_uri");
        if (apiKey == null || apiKey.isEmpty() || redirect == null || redirect.isEmpty()) {
            String missing = (apiKey == null || apiKey.isEmpty()) ? "kite_api_key" : "kite_redirect_uri";
            logger.error("{} not configured", missing);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write(String.format("{\"error\": \"%s not configured\"}", missing));
            return;
        }

        String url = String.format(
                "https://kite.zerodha.com/connect/login?v=3&api_key=%s&redirect_uri=%s",
                apiKey,
                URLEncoder.encode(redirect, "UTF-8"));
        response.sendRedirect(url);
    }

    @GetMapping("/callback")
    public void callback(@RequestParam("request_token") String token,
                         HttpServletResponse response) throws IOException {
        String apiKey = Config.get("kite_api_key");
        String secret = Config.get("kite_api_secret");
        String checksum = sha256(apiKey + token + secret);
        String body = String.format("api_key=%s&request_token=%s&checksum=%s", apiKey, token, checksum);
        URL url = new URL("https://api.kite.trade/session/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
            os.flush();
        }
        String message;
        if (conn.getResponseCode() != 200) {
            logger.error("Failed to authenticate, code {}", conn.getResponseCode());
            message = "Failed to capture access token";
        } else {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(conn.getInputStream());
            String access = root.path("data").path("access_token").asText();
            Config.set("kite_access_token", access);
            Config.save();
            message = "Access token captured successfully";
        }
        response.setContentType("text/html");
        response.getWriter().write("<html><body><h1>" + message + "</h1></body></html>");
    }

    @GetMapping("/check")
    public java.util.Map<String, Boolean> check() {
        String apiKey = Config.get("kite_api_key");
        String access = Config.get("kite_access_token");
        if (access == null || access.isEmpty()) {
            return java.util.Map.of("valid", false);
        }
        try {
            URL url = new URL(String.format("https://api.kite.trade/user/profile?api_key=%s&access_token=%s", apiKey, access));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-Kite-Version", "3");
            boolean ok = conn.getResponseCode() == 200;
            return java.util.Map.of("valid", ok);
        } catch (Exception e) {
            return java.util.Map.of("valid", false);
        }
    }

    private String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
