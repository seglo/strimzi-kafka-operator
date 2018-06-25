/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apiextensions.DoneableCustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.DoneableKafkaAssembly;
import io.strimzi.api.kafka.KafkaAssemblyList;
import io.strimzi.api.kafka.model.KafkaAssembly;
import io.vertx.core.Vertx;

public class KafkaAssemblyCrdOperator extends AbstractResourceOperator<KubernetesClient, KafkaAssembly, KafkaAssemblyList, DoneableKafkaAssembly, Resource<KafkaAssembly, DoneableKafkaAssembly>> {
    /**
     * Constructor
     * @param vertx The Vertx instance
     * @param client The Kubernetes client
     */
    public KafkaAssemblyCrdOperator(Vertx vertx, KubernetesClient client) {
        super(vertx, client, KafkaAssembly.RESOURCE_KIND);
    }

    @Override
    protected MixedOperation<KafkaAssembly, KafkaAssemblyList, DoneableKafkaAssembly, Resource<KafkaAssembly, DoneableKafkaAssembly>> operation() {
        NonNamespaceOperation<CustomResourceDefinition, CustomResourceDefinitionList, DoneableCustomResourceDefinition, Resource<CustomResourceDefinition, DoneableCustomResourceDefinition>> x = client.customResourceDefinitions();
        Resource<CustomResourceDefinition, DoneableCustomResourceDefinition> y = x.withName(KafkaAssembly.RESOURCE_NAME);
        CustomResourceDefinition crd = y.get();
        return client.customResources(crd, KafkaAssembly.class, KafkaAssemblyList.class, DoneableKafkaAssembly.class);
    }
}
