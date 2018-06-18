/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import io.fabric8.kubernetes.client.CustomResource;
import io.strimzi.test.Namespace;
import io.strimzi.test.Resources;
import io.strimzi.test.StrimziRunner;
import io.strimzi.test.TestUtils;
import io.strimzi.test.k8s.KubeClusterException;
import io.strimzi.test.k8s.KubeClusterResource;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.strimzi.operator.cluster.ResourceUtils.fromYaml;
import static io.strimzi.operator.cluster.ResourceUtils.toYamlString;
import static org.junit.Assert.assertTrue;

/**
 * The purpose of this test is to confirm that we can create a
 * resource from the POJOs, serialize it and create the resource in K8S.
 * I.e. that such instance resources obtained from POJOs are valid according to the schema
 * validation done by K8S.
 */
@RunWith(StrimziRunner.class)
@Namespace(KafkaCrdIT.NAMESPACE)
@Resources(value = "../examples/install/crd/kafka-crd.yaml", asAdmin = true)
public class KafkaCrdIT {
    public static final String NAMESPACE = "kafkacrd-it";

    @ClassRule
    public static KubeClusterResource cluster = new KubeClusterResource();

    private <T extends CustomResource> void createDelete(Class<T> resourceClass, String resource) {
        String ssStr = TestUtils.readResource(resourceClass, resource);
        System.out.println(ssStr);
        createDelete(ssStr);
        T model = fromYaml(resource, resourceClass, true);
        ssStr = toYamlString(model);
        System.out.println(ssStr);
        createDelete(ssStr);
    }

    private void createDelete(String ssStr) {
        RuntimeException thrown = null;
        RuntimeException thrown2 = null;
        try {
            try {
                cluster.client().createContent(ssStr);
            } catch (RuntimeException t) {
                thrown = t;
            }
        } finally {
            try {
                cluster.client().deleteContent(ssStr);
            } catch (RuntimeException t) {
                thrown2 = t;
            }
        }
        if (thrown != null) {
            if (thrown2 != null) {
                thrown.addSuppressed(thrown2);
            }
            throw thrown;
        } else if (thrown2 != null) {
            throw thrown2;
        }
    }

    @Test
    public void testKafka() {
        createDelete(KafkaAssembly.class, "kafka.yaml");
    }

    @Test
    public void testKafkaMinimal() {
        createDelete(KafkaAssembly.class, "kafka-minimal.yaml");
    }

    @Test
    public void testKafkaWithExtraProperty() {
        createDelete(KafkaAssembly.class, "kafka-with-extra-property.yaml");
    }

    @Test
    public void testKafkaWithMissingRequired() {
        try {
            createDelete(KafkaAssembly.class, "kafka-with-missing-required-property.yaml");
        } catch (KubeClusterException.InvalidResource e) {
            assertTrue(e.getMessage().contains("spec.zookeeper in body is required"));
            assertTrue(e.getMessage().contains("spec.kafka in body is required"));
        }
    }

    @Test
    public void testKafkaWithInvalidResourceMemory() {
        try {
            createDelete(KafkaAssembly.class, "kafka-with-invalid-resource-memory.yaml");
        } catch (KubeClusterException.InvalidResource e) {
            assertTrue(e.getMessage().contains("spec.kafka.resources.limits.memory in body should match '[0-9]+([kKmMgGtTpPeE]i?)?$'"));
        }
    }

}
