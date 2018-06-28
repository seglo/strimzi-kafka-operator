/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.DoneableKafkaConnectAssembly;
import io.strimzi.api.kafka.KafkaConnectAssemblyList;
import io.strimzi.api.kafka.model.KafkaConnectAssembly;
import io.vertx.core.Vertx;

public class KafkaConnectAssemblyCrdOperator extends AbstractWatchableResourceOperator<KubernetesClient, KafkaConnectAssembly, KafkaConnectAssemblyList, DoneableKafkaConnectAssembly, Resource<KafkaConnectAssembly, DoneableKafkaConnectAssembly>> {
    /**
     * Constructor
     * @param vertx The Vertx instance
     * @param client The Kubernetes client
     */
    public KafkaConnectAssemblyCrdOperator(Vertx vertx, KubernetesClient client) {
        super(vertx, client, KafkaConnectAssembly.RESOURCE_KIND);
    }

    @Override
    protected MixedOperation<KafkaConnectAssembly, KafkaConnectAssemblyList, DoneableKafkaConnectAssembly, Resource<KafkaConnectAssembly, DoneableKafkaConnectAssembly>> operation() {
        CustomResourceDefinition crd = getCustomResourceDefinition();
        return client.customResources(crd, KafkaConnectAssembly.class, KafkaConnectAssemblyList.class, DoneableKafkaConnectAssembly.class);
    }

    public static CustomResourceDefinition getCustomResourceDefinition() {
        return new CustomResourceDefinitionBuilder()
                    .withApiVersion(KafkaConnectAssembly.CRD_API_VERSION)
                    .withKind("CustomResourceDefinition")
                    .withNewSpec()
                        .withGroup(KafkaConnectAssembly.RESOURCE_GROUP)
                        .withVersion(KafkaConnectAssembly.VERSION)
                        .withNewNames()
                            .withKind(KafkaConnectAssembly.RESOURCE_KIND)
                            .withListKind(KafkaConnectAssembly.RESOURCE_LIST_KIND)
                            .withPlural(KafkaConnectAssembly.RESOURCE_PLURAL)
                            .withSingular(KafkaConnectAssembly.RESOURCE_SINGULAR)
                            .withShortNames()
                        .endNames()
                    .endSpec()
                    .withNewMetadata()
                        .withName(KafkaConnectAssembly.CRD_NAME)
                    .endMetadata()
                .build();
    }

    // TODO Generalize/parameterize this class, so it works for all custom resources
}
