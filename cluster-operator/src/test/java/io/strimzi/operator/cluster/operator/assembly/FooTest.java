/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly;

import io.strimzi.api.kafka.model.PersistentClaimStorageBuilder;
import io.strimzi.api.kafka.model.ResourcesBuilder;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class FooTest extends KafkaAssemblyOperatorMockTest {
    public FooTest() {
        super(new Params(1, new PersistentClaimStorageBuilder()
                .withSize("123")
                .withStorageClass("foo")
                .withDeleteClaim(true)
                .build(), 1, new PersistentClaimStorageBuilder()
                .withSize("123")
                .withStorageClass("foo")
                .withDeleteClaim(true)
                .build(),
                new ResourcesBuilder()
                        .withNewLimits()
                        .withMilliCpu("5000")
                        .withMemory("5000")
                        .endLimits()
                        .withNewRequests()
                        .withMilliCpu("5000")
                        .withMemory("5000")
                        .endRequests()
                        .build()));
    }
}
