/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.strimzi.crdgenerator.annotations.Crd;

/**
 * A description of a Kafka assembly, as exposed by the Strimzi Kafka CRD.
 */
@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@Crd(
        apiVersion = "apiextensions.k8s.io/v1beta1",
        spec = @Crd.Spec(
                names = @Crd.Spec.Names(
                        kind = KafkaAssembly.RESOURCE_KIND,
                        plural = "kafkas"
                ),
                group = KafkaAssembly.RESOURCE_GROUP,
                scope = "Namespaced",
                version = "v1alpha1"
        )
)
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec"})
public class KafkaAssembly extends CustomResource {

    private static final long serialVersionUID = 1L;
    public static final String RESOURCE_KIND = "Kafka";
    public static final String RESOURCE_GROUP = "cluster-operator.strimzi.io";
    public static final String RESOURCE_NAME = RESOURCE_KIND + "." + RESOURCE_GROUP;

    private String apiVersion;
    private ObjectMeta metadata;
    private transient KafkaAssemblySpec spec;

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @JsonProperty("kind")
    @Override
    public String getKind() {
        return RESOURCE_KIND;
    }

    @Override
    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    public KafkaAssemblySpec getSpec() {
        return spec;
    }

    public void setSpec(KafkaAssemblySpec spec) {
        this.spec = spec;
    }

    @Override
    public String toString() {
        YAMLMapper mapper = new YAMLMapper().disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
