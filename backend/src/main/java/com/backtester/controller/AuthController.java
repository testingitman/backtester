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
    public String callback(@RequestParam("request_token") String token) throws IOException {
        String apiKey = Config.get("kite_api_key");
        String secret = Config.get("kite_api_secret");
        String checksum = sha256(apiKey + token + secret);
        String body = String.format("api_key=%s&request_token=%s&checksum=%s", apiKey, token, checksum);
        URL url = new URL("https://api.kite.trade/session/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.flush();
        os.close();
        if (conn.getResponseCode() != 200) {
            return "Failed to authenticate";
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(conn.getInputStream());
        String access = root.path("data").path("access_token").asText();
        Config.set("kite_access_token", access);
        return "Authentication successful";
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
