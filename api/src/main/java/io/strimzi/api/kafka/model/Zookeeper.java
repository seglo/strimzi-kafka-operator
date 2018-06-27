/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.strimzi.crdgenerator.annotations.Description;
import io.sundr.builder.annotations.Buildable;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of a Strimzi-managed Zookeeper "cluster".
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = true,
        builderPackage = "io.strimzi.api.kafka.model"
)
@JsonPropertyOrder({ "replicas", "image", "storage",
        "livenessProbe", "readinessProbe", "jvmOptions",
        "affinity", "metrics"})
public class Zookeeper extends AbstractSsLike {
    public static final String FORBIDDEN_PREFIXES = "server., dataDir, dataLogDir, clientPort, authProvider, quorum.auth, requireClientAuthScheme";
    public static final String DEFAULT_IMAGE =
            System.getenv().getOrDefault("STRIMZI_DEFAULT_ZOOKEEPER_IMAGE", "strimzi/zookeeper:latest");
    public static final int DEFAULT_REPLICAS = 3;
    private Map<String, Object> config = new HashMap<>(0);
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    public Zookeeper() {
        this.image = DEFAULT_IMAGE;
        this.replicas = DEFAULT_REPLICAS;
    }

    @Description("The zookeeper broker config. Properties with the following prefixes cannot be set: " + FORBIDDEN_PREFIXES)
    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
