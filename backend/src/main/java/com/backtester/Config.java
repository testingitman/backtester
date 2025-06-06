package com.backtester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.io.FileWriter;

public class Config {
    private static Map<String, Object> values;
    private static File configFile;

    static {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            configFile = new File("config.yaml");
            if (!configFile.exists()) {
                configFile = new File("../config.yaml");
            }
            values = mapper.readValue(configFile, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.yaml", e);
        }
    }

    public static String get(String key) {
        Object v = values.get(key);
        return v == null ? null : v.toString();
    }

    public static void set(String key, String value) {
        values.put(key, value);
    }

    public static void save() {
        if (configFile == null) return;
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            try (FileWriter writer = new FileWriter(configFile)) {
                mapper.writeValue(writer, values);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config.yaml", e);
        }
    }
}
