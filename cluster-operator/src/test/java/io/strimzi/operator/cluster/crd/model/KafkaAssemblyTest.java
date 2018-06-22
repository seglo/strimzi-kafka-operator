/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.Test;

import java.io.IOException;

import static io.strimzi.operator.cluster.ResourceUtils.fromYaml;
import static io.strimzi.operator.cluster.ResourceUtils.toYamlString;
import static io.strimzi.test.TestUtils.readResource;
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
        String content = readResource(getClass(), resource);
        if (content != null) {
            String ssStr = toYamlString(k);
            assertEquals(content.trim(), ssStr.trim());
        } else {
            fail("The resource " + resource + " does not exist");
        }
    }

    @Test
    public void kafkaRoundTrip() throws IOException {
        KafkaAssembly model = fromYaml("kafka.yaml", KafkaAssembly.class);
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
