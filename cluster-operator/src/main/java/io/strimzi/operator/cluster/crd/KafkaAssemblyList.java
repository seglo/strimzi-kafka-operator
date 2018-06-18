/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.strimzi.operator.cluster.crd.model.KafkaAssembly;

/**
 * A {@code CustomResourceList<KafkaAssembly>} required for using Fabric8 CRD support.
 */
public class KafkaAssemblyList extends CustomResourceList<KafkaAssembly> {
    private static final long serialVersionUID = 1L;
}
