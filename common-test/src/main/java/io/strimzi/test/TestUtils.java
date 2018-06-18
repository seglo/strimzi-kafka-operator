/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.assertEquals;

public final class TestUtils {

    private static final Logger LOGGER = LogManager.getLogger(TestUtils.class);

    private TestUtils() {
        // All static methods
    }

    /** Returns a Map of the given sequence of key, value pairs. */
    public static <T> Map<T, T> map(T... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        Map<T, T> result = new HashMap<>(pairs.length / 2);
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(pairs[i], pairs[i + 1]);
        }
        return result;
    }

    /**
     * Poll the given {@code ready} function every {@code pollIntervalMs} milliseconds until it returns true,
     * or throw a TimeoutException if it doesn't returns true within {@code timeoutMs} milliseconds.
     * @return The remaining time left until timeout occurs
     * (helpful if you have several calls which need to share a common timeout),
     * */
    public static long waitFor(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready) {
        return waitFor(description, pollIntervalMs, timeoutMs, ready, () -> { });
    }

    public static long waitFor(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready, Runnable onTimeout) {
        LOGGER.debug("Waiting for {}", description);
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            boolean result = ready.getAsBoolean();
            long timeLeft = deadline - System.currentTimeMillis();
            if (result) {
                return timeLeft;
            }
            if (timeLeft <= 0) {
                onTimeout.run();
                throw new TimeoutException("Timeout after " + timeoutMs + " ms waiting for " + description + " to be ready");
            }
            long sleepTime = Math.min(pollIntervalMs, timeLeft);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{} not ready, will try again in {} ms ({}ms till timeout)", description, sleepTime, timeLeft);
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                return deadline - System.currentTimeMillis();
            }
        }
    }

    public static String indent(String s) {
        StringBuilder sb = new StringBuilder();
        String[] lines = s.split("[\n\r]");
        for (String line : lines) {
            sb.append("    ").append(line).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public static JsonNode yamlFileToJSON(String relativeFilePath) {
        JsonNode node = null;
        try {
            YAMLMapper mapper = new YAMLMapper();
            node = mapper.readTree(new File(relativeFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return node;
    }

    public static String changeOrgAndTag(String image, String newOrg, String newTag) {
        return image.replaceFirst("^strimzi/", newOrg + "/").replaceFirst(":[^:]+$", ":" + newTag);
    }

    /**
     * Read the classpath resource with the given resourceName and return the content as a String
     * @param cls The class relative to which the resource will be loaded.
     * @param resourceName The name of the resource
     * @return The resource content
     * @throws IOException
     */
    public static String readResource(Class<?> cls, String resourceName) {
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

    /**
     * Assert that the given actual string is the same as content of the
     * the classpath resource resourceName.
     * @param cls The class relative to which the resource will be loaded.
     * @param resourceName The name of the resource
     * @param actual The actual
     * @throws IOException
     */
    public static void assertResourceMatch(Class<?> cls, String resourceName, String actual) throws IOException {
        String r = readResource(cls, resourceName);
        assertEquals(r, actual);
    }
}
