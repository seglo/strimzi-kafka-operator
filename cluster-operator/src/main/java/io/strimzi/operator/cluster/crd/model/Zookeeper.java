/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.strimzi.crdgenerator.annotations.Description;
import io.strimzi.operator.cluster.model.KafkaConfiguration;
import io.strimzi.operator.cluster.model.ZookeeperConfiguration;

import java.util.Map;

/**
 * Representation of a Strimzi-managed Zookeeper "cluster".
 */
@JsonPropertyOrder({ "replicas", "image", "storage", "livenessProbe", "readinessProbe", "jvmOptions", "affinity", "metrics"})
public class Zookeeper extends AbstractSsLike {

    private Map<String, Object> config;

    public Zookeeper() {
        this.image = "strimzi/kafka:latest";
    }

    @Description("The zookeeper broker config. Properties with the following prefixes cannot be set: " + ZookeeperConfiguration.FORBIDDEN_PREFIXES)
    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}
