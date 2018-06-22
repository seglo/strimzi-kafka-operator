/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.strimzi.crdgenerator.annotations.Description;
import io.strimzi.operator.cluster.model.KafkaCluster;
import io.strimzi.operator.cluster.model.KafkaConfiguration;
import io.sundr.builder.annotations.Buildable;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of a Strimzi-managed Kafka "cluster".
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = true,
        builderPackage = "io.strimzi.operator.cluster.crd.model"
)
@JsonPropertyOrder({ "replicas", "image", "storage", "rackConfig", "brokerRackInitImage", "livenessProbe", "readinessProbe", "jvmOptions", "affinity", "metrics"})
public class Kafka extends AbstractSsLike {

    private Map<String, Object> config = new HashMap<>(0);

    private String brokerRackInitImage = KafkaCluster.DEFAULT_INIT_IMAGE;

    private RackConfig rackConfig;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    public Kafka() {
        this.image = KafkaCluster.DEFAULT_IMAGE;
    }

    @Description("The kafka broker config. Properties with the following prefixes cannot be set: " + KafkaConfiguration.FORBIDDEN_PREFIXES)
    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    @Description("The image of the init container used for initializing the `broker.rack`.")
    public String getBrokerRackInitImage() {
        return brokerRackInitImage;
    }

    public void setBrokerRackInitImage(String brokerRackInitImage) {
        this.brokerRackInitImage = brokerRackInitImage;
    }

    @Description("Configuration of the `broker.rack` broker config.")
    @JsonProperty("rack")
    public RackConfig getRackConfig() {
        return rackConfig;
    }

    public void setRackConfig(RackConfig rackConfig) {
        this.rackConfig = rackConfig;
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
