/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(VertxUnitRunner.class)
public class ClusterOperatorTest {

    public static final String STRIMZI_IO_KIND_CLUSTER = "strimzi.io/kind=cluster";
    private Vertx vertx;

    @Before
    public void createClient(TestContext context) {
        vertx = Vertx.vertx();
    }

    @After
    public void closeClient() {
        vertx.close();
    }

    @Test
    public void startStopSingleNamespace(TestContext context) {
        startStop(context, "namespace");
    }

    @Test
    public void startStopMultiNamespace(TestContext context) {
        startStop(context, "namespace1, namespace2");
    }

    /**
     * Does the CC start and then stop a verticle per namespace?
     * @param context
     * @param namespaces
     */
    private void startStop(TestContext context, String namespaces) {
        AtomicInteger numWatchers = new AtomicInteger(0);
        KubernetesClient client = mock(KubernetesClient.class);
        MixedOperation mockCms = mock(MixedOperation.class);
        when(client.configMaps()).thenReturn(mockCms);
        when(client.customResources(any(), any(), any(), any())).thenReturn(mockCms);
        List<String> namespaceList = asList(namespaces.split(" *,+ *"));
        for (String namespace: namespaceList) {

            MixedOperation mockNamespacedCms = mock(MixedOperation.class);
            when(mockNamespacedCms.watch(any())).thenAnswer(invo -> {
                numWatchers.incrementAndGet();
                Watch mockWatch = mock(Watch.class);
                doAnswer(invo2 -> {
                    ((Watcher) invo.getArgument(0)).onClose(null);
                    return null;
                }).when(mockWatch).close();
                return mockWatch;
            });

            when(mockNamespacedCms.withLabels(any())).thenReturn(mockNamespacedCms);
            when(mockCms.inNamespace(namespace)).thenReturn(mockNamespacedCms);
        }
        Async async = context.async();

        Map<String, String> env = new HashMap<>();
        env.put(ClusterOperatorConfig.STRIMZI_NAMESPACE, namespaces);
        env.put(ClusterOperatorConfig.STRIMZI_CONFIGMAP_LABELS, STRIMZI_IO_KIND_CLUSTER);
        env.put(ClusterOperatorConfig.STRIMZI_FULL_RECONCILIATION_INTERVAL_MS, "120000");
        Main.run(vertx, client, true, ClusterOperatorConfig.fromMap(env)).setHandler(ar -> {
            context.assertNull(ar.cause(), "Expected all verticles to start OK");
            async.complete();
        });
        async.await();

        context.assertEquals(namespaceList.size(), vertx.deploymentIDs().size(), "A verticle per namespace");

        List<Async> asyncs = new ArrayList<>();
        for (String deploymentId: vertx.deploymentIDs()) {
            Async async2 = context.async();
            asyncs.add(async2);
            vertx.undeploy(deploymentId, ar -> {
                context.assertNull(ar.cause(), "Didn't expect error when undeploying verticle " + deploymentId);
                async2.complete();
            });
        }

        for (Async async2: asyncs) {
            async2.await();
        }

        if (numWatchers.get() > 2 * namespaceList.size()) {
            context.fail("Looks like there were more watchers than namespaces");
        }
    }

}
