/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.strimzi.crdgenerator.annotations.Description;
import io.sundr.builder.annotations.Buildable;

/**
 * The {@code spec} of a {@link KafkaAssembly}.
 */
@Buildable(editableEnabled = false, validationEnabled = true, generateBuilderPackage = true, builderPackage = "io.strimzi.operator.cluster.crd.model"/*, inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")*/)
@JsonPropertyOrder({ "kafka", "zookeeper", "topicOperator" })
public class KafkaAssemblySpec {
    private Kafka kafka;
    private Zookeeper zookeeper;
    private TopicOperator topicOperator;

    @Description("Configuration of the Kafka cluster")
    @JsonProperty(required = true)
    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    @Description("Configuration of the Zookeeper cluster")
    @JsonProperty(required = true)
    public Zookeeper getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(Zookeeper zookeeper) {
        this.zookeeper = zookeeper;
    }

    @Description("Configuration of the Topic Operator")
    public TopicOperator getTopicOperator() {
        return topicOperator;
    }

    public void setTopicOperator(TopicOperator topicOperator) {
        this.topicOperator = topicOperator;
    }

    @Override
    public String toString() {
        YAMLMapper mapper = new YAMLMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
