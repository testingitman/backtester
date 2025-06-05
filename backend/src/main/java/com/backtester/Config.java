package com.backtester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class Config {
    private static Map<String, Object> values;

    static {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            values = mapper.readValue(new File("config.yaml"), Map.class);
        } catch (IOException e) {
            values = Collections.emptyMap();
        }
    }

    public static String get(String key) {
        Object v = values.get(key);
        return v == null ? null : v.toString();
    }
}
