/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.strimzi.api.kafka.DoneableKafkaConnectS2IAssembly;
import io.strimzi.api.kafka.KafkaConnectS2IAssemblyList;
import io.strimzi.api.kafka.model.KafkaConnectS2IAssembly;
import io.vertx.core.Vertx;

public class KafkaConnectS2IAssemblyCrdOperator
        extends AbstractResourceOperator<OpenShiftClient,
            KafkaConnectS2IAssembly,
            KafkaConnectS2IAssemblyList,
            DoneableKafkaConnectS2IAssembly,
            Resource<KafkaConnectS2IAssembly, DoneableKafkaConnectS2IAssembly>> {
    /**
     * Constructor
     * @param vertx The Vertx instance
     * @param client The Kubernetes client
     */
    public KafkaConnectS2IAssemblyCrdOperator(Vertx vertx, OpenShiftClient client) {
        super(vertx, client, KafkaConnectS2IAssembly.RESOURCE_KIND);
    }

    @Override
    protected MixedOperation<KafkaConnectS2IAssembly, KafkaConnectS2IAssemblyList, DoneableKafkaConnectS2IAssembly, Resource<KafkaConnectS2IAssembly, DoneableKafkaConnectS2IAssembly>> operation() {
        CustomResourceDefinition crd = getCustomResourceDefinition();
        return client.customResources(crd, KafkaConnectS2IAssembly.class, KafkaConnectS2IAssemblyList.class, DoneableKafkaConnectS2IAssembly.class);
    }

    public static CustomResourceDefinition getCustomResourceDefinition() {
        return new CustomResourceDefinitionBuilder()
                    .withApiVersion(KafkaConnectS2IAssembly.CRD_API_VERSION)
                    .withKind("CustomResourceDefinition")
                    .withNewSpec()
                        .withGroup(KafkaConnectS2IAssembly.RESOURCE_GROUP)
                        .withVersion(KafkaConnectS2IAssembly.VERSION)
                        .withNewNames()
                            .withKind(KafkaConnectS2IAssembly.RESOURCE_KIND)
                            .withListKind(KafkaConnectS2IAssembly.RESOURCE_LIST_KIND)
                            .withPlural(KafkaConnectS2IAssembly.RESOURCE_PLURAL)
                            .withSingular(KafkaConnectS2IAssembly.RESOURCE_SINGULAR)
                            .withShortNames()
                        .endNames()
                    .endSpec()
                    .withNewMetadata()
                        .withName(KafkaConnectS2IAssembly.CRD_NAME)
                    .endMetadata()
                .build();
    }

    // TODO Generalize/parameterize this class, so it works for all custom resources
}
