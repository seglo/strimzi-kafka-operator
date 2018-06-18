/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Representation of a Strimzi-managed Zookeeper "cluster".
 */
@JsonPropertyOrder({ "replicas", "image", "storage", "livenessProbe", "readinessProbe", "jvmOptions", "affinity", "metrics"})
public class Zookeeper extends AbstractSsLike {
    public Zookeeper() {
        this.image = "strimzi/kafka:latest";
    }
}
