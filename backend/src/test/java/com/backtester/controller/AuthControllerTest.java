package com.backtester.controller;

import com.backtester.Config;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String origApiKey;
    private String origRedirect;

    @BeforeEach
    void saveConfig() {
        origApiKey = Config.get("kite_api_key");
        origRedirect = Config.get("kite_redirect_uri");
    }

    @AfterEach
    void restoreConfig() {
        Config.set("kite_api_key", origApiKey);
        Config.set("kite_redirect_uri", origRedirect);
    }

    @Test
    void loginReturnsErrorWhenRedirectMissing() throws Exception {
        Config.set("kite_api_key", "key");
        Config.set("kite_redirect_uri", "");
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"kite_redirect_uri not configured\"}"));
    }

    @Test
    void loginReturnsErrorWhenApiKeyMissing() throws Exception {
        Config.set("kite_api_key", "");
        Config.set("kite_redirect_uri", "http://example.com");
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"kite_api_key not configured\"}"));
    }
}
