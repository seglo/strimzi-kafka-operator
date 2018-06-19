/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strimzi.crdgenerator.annotations.Description;
import io.strimzi.crdgenerator.annotations.Example;
import io.sundr.builder.annotations.Buildable;

import java.util.Map;

/**
 * Representation of the rack configuration.
 */
@Buildable(editableEnabled = false, validationEnabled = true, generateBuilderPackage = true, builderPackage = "io.strimzi.operator.cluster.crd.model"/*, inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")*/)
public class RackConfig {

    private String topologyKey;
    private Map<String, Object> additionalProperties;

    public RackConfig() {

    }

    public RackConfig(String topologyKey) {
        this.topologyKey = topologyKey;
    }

    @Description("A key that matches labels assigned to the OpenShift or Kubernetes cluster nodes. " +
            "The value of the label is used to set the broker's `broker.rack` config.")
    @Example("failure-domain.beta.kubernetes.io/zone")
    @JsonProperty(required = true)
    public String getTopologyKey() {
        return topologyKey;
    }

    public static RackConfig fromJson(String json) {
        RackConfig rackConfig = JsonUtils.fromJson(json, RackConfig.class);
        if (rackConfig != null && (rackConfig.getTopologyKey() == null || rackConfig.getTopologyKey().equals(""))) {
            throw new IllegalArgumentException("In rack configuration the 'topologyKey' field is mandatory");
        }
        return rackConfig;
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
