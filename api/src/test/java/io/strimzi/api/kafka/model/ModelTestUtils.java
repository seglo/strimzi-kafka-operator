/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ModelTestUtils {

    /** Returns a Map of the given sequence of key, value pairs. */
    static <T> Map<T, T> map(T... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        Map<T, T> result = new HashMap<>(pairs.length / 2);
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(pairs[i], pairs[i + 1]);
        }
        return result;
    }

    static <T> T fromYaml(String resource, Class<T> c) {
        return fromYaml(resource, c, false);
    }

    static <T> T fromYaml(String resource, Class<T> c, boolean ignoreUnknownProperties) {
        URL url = c.getResource(resource);
        if (url == null) {
            return null;
        }
        ObjectMapper mapper = new YAMLMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, !ignoreUnknownProperties);
        try {
            return mapper.readValue(url, c);
        } catch (InvalidFormatException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static <T> String toYamlString(T instance) {
        ObjectMapper mapper = new YAMLMapper()
                .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writeValueAsString(instance);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read the classpath resource with the given resourceName and return the content as a String
     * @param cls The class relative to which the resource will be loaded.
     * @param resourceName The name of the resource
     * @return The resource content
     * @throws IOException
     */
    static String readResource(Class<?> cls, String resourceName) {
        try {
            InputStream expectedStream = cls.getResourceAsStream(resourceName);
            if (expectedStream != null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(expectedStream, StandardCharsets.UTF_8))) {
                        String line = reader.readLine();
                        while (line != null) {
                            sb.append(line).append("\n");
                            line = reader.readLine();
                        }
                        return sb.toString();
                    }
                } finally {
                    expectedStream.close();
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
