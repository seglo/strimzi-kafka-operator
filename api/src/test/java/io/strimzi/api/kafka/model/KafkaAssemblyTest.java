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
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * The purpose of this test is to ensure:
 *
 * 1. we get a correct tree of POJOs when reading a JSON/YAML `Kafka` resource.
 *
 * 2. We get informative exceptions when a JSON/YAML `Kafka` resource cannot be read
 *    because it is invalid.
 */
public class KafkaAssemblyTest {




    protected void assertDesiredResource(KafkaAssembly k, String resource) throws IOException {
        //assertNotNull("The resource " + resourceName + " does not exist", model);
        String content = ModelTestUtils.readResource(getClass(), resource);
        if (content != null) {
            String ssStr = ModelTestUtils.toYamlString(k);
            assertEquals(content.trim(), ssStr.trim());
        } else {
            fail("The resource " + resource + " does not exist");
        }
    }

    @Test
    public void kafkaRoundTrip() throws IOException {
        KafkaAssembly model = ModelTestUtils.fromYaml("kafka.yaml", KafkaAssembly.class);
        assertEquals(KafkaAssembly.RESOURCE_GROUP + "/" + KafkaAssembly.VERSION, model.getApiVersion());
        assertEquals("Kafka", model.getKind());

        ObjectMeta metadata = model.getMetadata();
        assertNotNull(metadata);
        assertEquals("strimzi-ephemeral", metadata.getName());

        assertNotNull(model.getSpec());
        assertNotNull(model.getSpec().getKafka());
        assertNotNull(model.getSpec().getZookeeper());
        assertNotNull(model.getSpec().getTopicOperator());

        assertDesiredResource(model, "kafka.out.yaml");
    }
}
