/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.assembly;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.strimzi.api.kafka.model.JsonUtils;
import io.strimzi.api.kafka.model.KafkaAssembly;
import io.strimzi.api.kafka.model.Storage;
import io.strimzi.operator.cluster.ClusterOperatorConfig;
import io.strimzi.operator.cluster.Reconciliation;
import io.strimzi.operator.cluster.ResourceUtils;
import io.strimzi.operator.cluster.model.AbstractModel;
import io.strimzi.operator.cluster.model.AssemblyType;
import io.strimzi.operator.cluster.model.KafkaCluster;
import io.strimzi.operator.cluster.model.Labels;
import io.strimzi.operator.cluster.model.TopicOperator;
import io.strimzi.operator.cluster.model.ZookeeperCluster;
import io.strimzi.operator.cluster.operator.resource.ConfigMapOperator;
import io.strimzi.operator.cluster.operator.resource.DeploymentOperator;
import io.strimzi.operator.cluster.operator.resource.CrdOperator;
import io.strimzi.operator.cluster.operator.resource.KafkaSetOperator;
import io.strimzi.operator.cluster.operator.resource.PvcOperator;
import io.strimzi.operator.cluster.operator.resource.ReconcileResult;
import io.strimzi.operator.cluster.operator.resource.SecretOperator;
import io.strimzi.operator.cluster.operator.resource.ServiceOperator;
import io.strimzi.operator.cluster.operator.resource.StatefulSetDiff;
import io.strimzi.operator.cluster.operator.resource.ZookeeperSetOperator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunnerWithParametersFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static io.strimzi.test.TestUtils.set;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(VertxUnitRunnerWithParametersFactory.class)
public class KafkaAssemblyOperatorTest {

    public static final String METRICS_CONFIG = "{\"foo\":\"bar\"}";
    private final boolean openShift;
    private final boolean metrics;
    private final String kafkaConfig;
    private final String zooConfig;
    private final String storage;
    private final String tcConfig;
    private final boolean deleteClaim;
    private MockCertManager certManager = new MockCertManager();

    public static class Params {
        private final boolean openShift;
        private final boolean metrics;
        private final String kafkaConfig;
        private final String zooConfig;
        private final String storage;
        private final String tcConfig;

        public Params(boolean openShift, boolean metrics, String kafkaConfig, String zooConfig, String storage, String tcConfig) {
            this.openShift = openShift;
            this.metrics = metrics;
            this.kafkaConfig = kafkaConfig;
            this.zooConfig = zooConfig;
            this.storage = storage;
            this.tcConfig = tcConfig;
        }

        public String toString() {
            return "openShift=" + openShift +
                    ",metrics=" + metrics +
                    ",kafkaConfig=" + kafkaConfig +
                    ",zooConfig=" + zooConfig +
                    ",storage=" + storage +
                    ",tcConfig=" + tcConfig;
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Params> data() {
        boolean[] shiftiness = {true, false};
        boolean[] metrics = {true, false};
        String[] storageConfigs = {
            "{\"type\": \"ephemeral\"}",
            "{\"type\": \"persistent-claim\", " +
                    "\"size\": \"123\", " +
                    "\"class\": \"foo\"," +
                    "\"deleteClaim\": true}"
        };
        String[] kafkaConfigs = {
            null,
            "{ }",
            "{\"foo\": \"bar\"}"
        };
        String[] zooConfigs = {
            null,
            "{ }",
            "{\"foo\": \"bar\"}"
        };
        String[] tcConfigs = {
            null,
            "{ }",
            "{\"reconciliationInterval\": \"10 minutes\", " +
                    "\"zookeeperSessionTimeout\": \"10 seconds\"}"
        };
        List<Params> result = new ArrayList();
        for (boolean shift: shiftiness) {
            for (boolean metric: metrics) {
                for (String kafkaConfig: kafkaConfigs) {
                    for (String zooConfig: zooConfigs) {
                        for (String storage : storageConfigs) {
                            for (String tcConfig : tcConfigs) {
                                result.add(new Params(shift, metric, kafkaConfig, zooConfig, storage, tcConfig));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public KafkaAssemblyOperatorTest(Params params) {
        this.openShift = params.openShift;
        this.metrics = params.metrics;
        this.kafkaConfig = params.kafkaConfig;
        this.zooConfig = params.zooConfig;
        this.storage = params.storage;
        this.tcConfig = params.tcConfig;
        this.deleteClaim = Storage.deleteClaim(JsonUtils.fromJson(params.storage, Storage.class));
    }

    protected static Vertx vertx;

    @BeforeClass
    public static void before() {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void after() {
        vertx.close();
    }

    @Test
    public void testCreateCluster(TestContext context) {
        createCluster(context, getKafkaAssembly("foo"), getInitialSecrets());
    }

    private void createCluster(TestContext context, KafkaAssembly clusterCm, List<Secret> secrets) {
        KafkaCluster kafkaCluster = KafkaCluster.fromCrd(certManager, clusterCm, secrets);
        ZookeeperCluster zookeeperCluster = ZookeeperCluster.fromCrd(clusterCm);
        TopicOperator topicOperator = TopicOperator.fromCrd(clusterCm);

        // create CM, Service, headless service, statefulset and so on
        CrdOperator mockKafkaOps = mock(CrdOperator.class);
        ConfigMapOperator mockCmOps = mock(ConfigMapOperator.class);
        ServiceOperator mockServiceOps = mock(ServiceOperator.class);
        ZookeeperSetOperator mockZsOps = mock(ZookeeperSetOperator.class);
        KafkaSetOperator mockKsOps = mock(KafkaSetOperator.class);
        PvcOperator mockPvcOps = mock(PvcOperator.class);
        DeploymentOperator mockDepOps = mock(DeploymentOperator.class);
        SecretOperator mockSecretOps = mock(SecretOperator.class);

        // Create a CM
        String clusterCmName = clusterCm.getMetadata().getName();
        String clusterCmNamespace = clusterCm.getMetadata().getNamespace();
        //when(mockCmOps.get(clusterCmNamespace, clusterCmName)).thenReturn(clusterCm);
        when(mockKafkaOps.get(clusterCmNamespace, clusterCmName)).thenReturn(null);
        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);
        when(mockServiceOps.reconcile(anyString(), anyString(), serviceCaptor.capture())).thenReturn(Future.succeededFuture(ReconcileResult.created(null)));
        when(mockServiceOps.endpointReadiness(anyString(), any(), anyLong(), anyLong())).thenReturn(Future.succeededFuture());
        ArgumentCaptor<StatefulSet> ssCaptor = ArgumentCaptor.forClass(StatefulSet.class);
        when(mockZsOps.reconcile(anyString(), anyString(), ssCaptor.capture())).thenReturn(Future.succeededFuture(ReconcileResult.created(null)));
        when(mockZsOps.scaleDown(anyString(), anyString(), anyInt())).thenReturn(Future.succeededFuture(null));
        when(mockZsOps.maybeRollingUpdate(any())).thenReturn(Future.succeededFuture());
        when(mockZsOps.scaleUp(anyString(), anyString(), anyInt())).thenReturn(Future.succeededFuture(42));
        when(mockKsOps.reconcile(anyString(), anyString(), ssCaptor.capture())).thenReturn(Future.succeededFuture(ReconcileResult.created(null)));
        when(mockKsOps.scaleDown(anyString(), anyString(), anyInt())).thenReturn(Future.succeededFuture(null));
        when(mockKsOps.maybeRollingUpdate(any())).thenReturn(Future.succeededFuture());
        when(mockKsOps.scaleUp(anyString(), anyString(), anyInt())).thenReturn(Future.succeededFuture(42));
        ArgumentCaptor<String> secretsCaptor = ArgumentCaptor.forClass(String.class);
        when(mockSecretOps.reconcile(anyString(), secretsCaptor.capture(), any())).thenReturn(Future.succeededFuture(ReconcileResult.created(null)));

        when(mockDepOps.reconcile(anyString(), anyString(), any())).thenAnswer(invocation -> {
            Deployment desired = invocation.getArgument(2);
            if (desired != null) {
                context.assertEquals(TopicOperator.topicOperatorName(clusterCmName), desired.getMetadata().getName());
            }
            return Future.succeededFuture(ReconcileResult.created(desired));
        }); //Return(Future.succeededFuture(ReconcileResult.created(depCaptor.getValue())));

        //when(mockSsOps.readiness(any(), any(), anyLong(), anyLong())).thenReturn(Future.succeededFuture());
        //when(mockPodOps.readiness(any(), any(), anyLong(), anyLong())).thenReturn(Future.succeededFuture());
        //when(mockEndpointOps.readiness(any(), any(), anyLong(), anyLong())).thenReturn(Future.succeededFuture());

        ArgumentCaptor<ConfigMap> metricsCaptor = ArgumentCaptor.forClass(ConfigMap.class);
        ArgumentCaptor<String> metricsNameCaptor = ArgumentCaptor.forClass(String.class);
        when(mockCmOps.reconcile(anyString(), metricsNameCaptor.capture(), metricsCaptor.capture())).thenReturn(Future.succeededFuture(ReconcileResult.created(null)));

        KafkaAssemblyOperator ops = new KafkaAssemblyOperator(vertx, openShift,
                ClusterOperatorConfig.DEFAULT_OPERATION_TIMEOUT_MS,
                certManager,
                mockKafkaOps,
                mockCmOps,
                mockServiceOps, mockZsOps, mockKsOps,
                mockPvcOps, mockDepOps, mockSecretOps);

        // Now try to create a KafkaCluster based on this CM
        Async async = context.async();
        ops.createOrUpdate(new Reconciliation("test-trigger", AssemblyType.KAFKA, clusterCmNamespace, clusterCmName), clusterCm, secrets, createResult -> {
            if (createResult.failed()) {
                createResult.cause().printStackTrace();
            }
            context.assertTrue(createResult.succeeded());

            // No metrics config  => no CMs created
            Set<String> metricsNames = new HashSet<>();
            if (kafkaCluster.isMetricsEnabled()) {
                metricsNames.add(KafkaCluster.metricConfigsName(clusterCmName));
            }
            if (zookeeperCluster.isMetricsEnabled()) {
                metricsNames.add(ZookeeperCluster.zookeeperMetricsName(clusterCmName));
            }
            /*
            Map<String, ConfigMap> cmsByName = new HashMap<>();
            Iterator<ConfigMap> it2 = metricsCaptor.getAllValues().iterator();
            for (Iterator<String> it = metricsNameCaptor.getAllValues().iterator(); it.hasNext(); ) {
                cmsByName.put(it.next(), it2.next());
            }
            context.assertEquals(metricsNames, cmsByName.keySet(),
                    "Unexpected metrics ConfigMaps");
            if (kafkaCluster.isMetricsEnabled()) {
                ConfigMap kafkaMetricsCm = cmsByName.get(KafkaCluster.metricConfigsName(clusterCmName));
                context.assertEquals(ResourceUtils.labels(Labels.STRIMZI_TYPE_LABEL, "kafka",
                        Labels.STRIMZI_CLUSTER_LABEL, clusterCmName,
                        "my-user-label", "cromulent"), kafkaMetricsCm.getMetadata().getLabels());
            }
            if (zookeeperCluster.isMetricsEnabled()) {
                ConfigMap zookeeperMetricsCm = cmsByName.get(ZookeeperCluster.zookeeperMetricsName(clusterCmName));
                context.assertEquals(ResourceUtils.labels(Labels.STRIMZI_TYPE_LABEL, "zookeeper",
                        Labels.STRIMZI_CLUSTER_LABEL, clusterCmName,
                        "my-user-label", "cromulent"), zookeeperMetricsCm.getMetadata().getLabels());
            }*/


            // We expect a headless and headful service
            List<Service> capturedServices = serviceCaptor.getAllValues();
            context.assertEquals(4, capturedServices.size());
            context.assertEquals(set(KafkaCluster.kafkaClusterName(clusterCmName),
                    KafkaCluster.headlessName(clusterCmName),
                    ZookeeperCluster.zookeeperClusterName(clusterCmName),
                    ZookeeperCluster.zookeeperHeadlessName(clusterCmName)), capturedServices.stream().map(svc -> svc.getMetadata().getName()).collect(Collectors.toSet()));

            // Assertions on the statefulset
            List<StatefulSet> capturedSs = ssCaptor.getAllValues();
            // We expect a statefulSet for kafka and zookeeper...
            context.assertEquals(set(KafkaCluster.kafkaClusterName(clusterCmName), ZookeeperCluster.zookeeperClusterName(clusterCmName)),
                    capturedSs.stream().map(ss -> ss.getMetadata().getName()).collect(Collectors.toSet()));

            // expected Secrets with certificates
            context.assertEquals(set(
                    KafkaCluster.clientsCASecretName(clusterCmName),
                    KafkaCluster.clientsPublicKeyName(clusterCmName),
                    KafkaCluster.brokersClientsSecret(clusterCmName),
                    KafkaCluster.brokersInternalSecretName(clusterCmName)),
                    captured(secretsCaptor));

            // Verify that we wait for readiness
            //verify(mockEndpointOps, times(4)).readiness(any(), any(), anyLong(), anyLong());
            //verify(mockSsOps, times(1)).readiness(any(), any(), anyLong(), anyLong());
            //verify(mockPodOps, times(zookeeperCluster.getReplicas() + kafkaCluster.getReplicas()))
            //        .readiness(any(), any(), anyLong(), anyLong());

            // PvcOperations only used for deletion
            verifyNoMoreInteractions(mockPvcOps);
            async.complete();
        });
    }

    private void deleteCluster(TestContext context, KafkaAssembly clusterCm, List<Secret> secrets) {

        ZookeeperCluster zookeeperCluster = ZookeeperCluster.fromCrd(clusterCm);
        KafkaCluster kafkaCluster = KafkaCluster.fromCrd(certManager, clusterCm, secrets);
        TopicOperator topicOperator = TopicOperator.fromCrd(clusterCm);

        // create CM, Service, headless service, statefulset
        CrdOperator mockKafkaOps = mock(CrdOperator.class);
        ConfigMapOperator mockCmOps = mock(ConfigMapOperator.class);
        ServiceOperator mockServiceOps = mock(ServiceOperator.class);
        ZookeeperSetOperator mockZsOps = mock(ZookeeperSetOperator.class);
        KafkaSetOperator mockKsOps = mock(KafkaSetOperator.class);
        PvcOperator mockPvcOps = mock(PvcOperator.class);
        DeploymentOperator mockDepOps = mock(DeploymentOperator.class);
        SecretOperator mockSecretOps = mock(SecretOperator.class);

        String assemblyName = clusterCm.getMetadata().getName();
        String assemblyNamespace = clusterCm.getMetadata().getNamespace();
        StatefulSet kafkaSs = kafkaCluster.generateStatefulSet(true);

        StatefulSet zkSs = zookeeperCluster.generateStatefulSet(true);
        when(mockKsOps.get(assemblyNamespace, KafkaCluster.kafkaClusterName(assemblyName))).thenReturn(kafkaSs);
        when(mockZsOps.get(assemblyNamespace, ZookeeperCluster.zookeeperClusterName(assemblyName))).thenReturn(zkSs);

        Secret clientsCASecret = kafkaCluster.generateClientsCASecret();
        Secret clientsPublicKeySecret = kafkaCluster.generateClientsPublicKeySecret();
        Secret brokersInternalSecret = kafkaCluster.generateBrokersInternalSecret();
        Secret brokersClientsSecret = kafkaCluster.generateBrokersClientsSecret();
        when(mockSecretOps.get(assemblyNamespace, KafkaCluster.clientsCASecretName(assemblyName))).thenReturn(clientsCASecret);
        when(mockSecretOps.get(assemblyNamespace, KafkaCluster.clientsPublicKeyName(assemblyName))).thenReturn(clientsPublicKeySecret);
        when(mockSecretOps.get(assemblyNamespace, KafkaCluster.brokersInternalSecretName(assemblyName))).thenReturn(brokersInternalSecret);
        when(mockSecretOps.get(assemblyNamespace, KafkaCluster.brokersClientsSecret(assemblyName))).thenReturn(brokersClientsSecret);

        ArgumentCaptor<String> secretsCaptor = ArgumentCaptor.forClass(String.class);
        when(mockSecretOps.reconcile(eq(assemblyNamespace), secretsCaptor.capture(), isNull())).thenReturn(Future.succeededFuture());

        when(mockKafkaOps.get(assemblyNamespace, assemblyName)).thenReturn(clusterCm);

        ArgumentCaptor<String> serviceCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ssCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> metricsCaptor = ArgumentCaptor.forClass(String.class);
        when(mockCmOps.reconcile(eq(assemblyNamespace), metricsCaptor.capture(), isNull())).thenReturn(Future.succeededFuture());

        when(mockServiceOps.reconcile(eq(assemblyNamespace), serviceCaptor.capture(), isNull())).thenReturn(Future.succeededFuture());
        when(mockKsOps.reconcile(anyString(), ssCaptor.capture(), isNull())).thenReturn(Future.succeededFuture());
        when(mockZsOps.reconcile(anyString(), ssCaptor.capture(), isNull())).thenReturn(Future.succeededFuture());

        ArgumentCaptor<String> pvcCaptor = ArgumentCaptor.forClass(String.class);
        when(mockPvcOps.reconcile(eq(assemblyNamespace), pvcCaptor.capture(), isNull())).thenReturn(Future.succeededFuture());

        ArgumentCaptor<String> depCaptor = ArgumentCaptor.forClass(String.class);
        when(mockDepOps.reconcile(eq(assemblyNamespace), depCaptor.capture(), isNull())).thenReturn(Future.succeededFuture());
        if (topicOperator != null) {
            Deployment tcDep = topicOperator.generateDeployment();
            when(mockDepOps.get(assemblyNamespace, TopicOperator.topicOperatorName(assemblyName))).thenReturn(tcDep);
        }

        KafkaAssemblyOperator ops = new KafkaAssemblyOperator(vertx, openShift,
                ClusterOperatorConfig.DEFAULT_OPERATION_TIMEOUT_MS,
                certManager,
                mockKafkaOps,
                mockCmOps,
                mockServiceOps, mockZsOps, mockKsOps,
                mockPvcOps,
                mockDepOps, mockSecretOps);

        // Now try to delete a KafkaCluster based on this CM
        Async async = context.async();
        ops.delete(new Reconciliation("test-trigger", AssemblyType.KAFKA, assemblyNamespace, assemblyName), createResult -> {
            if (createResult.failed()) {
                createResult.cause().printStackTrace();
            }
            context.assertTrue(createResult.succeeded());

            /*Set<String> metricsNames = new HashSet<>();
            if (kafkaCluster.isMetricsEnabled()) {
                metricsNames.add(KafkaCluster.metricConfigsName(assemblyName));
            }
            if (zookeeperCluster.isMetricsEnabled()) {
                metricsNames.add(ZookeeperCluster.zookeeperMetricsName(assemblyName));
            }
            context.assertEquals(metricsNames, captured(metricsCaptor));*/
            verify(mockZsOps).reconcile(eq(assemblyNamespace), eq(ZookeeperCluster.zookeeperClusterName(assemblyName)), isNull());

            context.assertEquals(set(
                    ZookeeperCluster.zookeeperHeadlessName(assemblyName),
                    ZookeeperCluster.zookeeperClusterName(assemblyName),
                    KafkaCluster.kafkaClusterName(assemblyName),
                    KafkaCluster.headlessName(assemblyName)),
                    captured(serviceCaptor));

            // verify deleted Statefulsets
            context.assertEquals(set(zookeeperCluster.getName(), kafkaCluster.getName()), captured(ssCaptor));

            // verify deleted Secrets
            context.assertEquals(set(
                    KafkaCluster.clientsCASecretName(assemblyName),
                    KafkaCluster.clientsPublicKeyName(assemblyName),
                    KafkaCluster.brokersInternalSecretName(assemblyName),
                    KafkaCluster.brokersClientsSecret(assemblyName)),
                    captured(secretsCaptor));

            // PvcOperations only used for deletion
            Set<String> expectedPvcDeletions = new HashSet<>();
            for (int i = 0; deleteClaim && i < kafkaCluster.getReplicas(); i++) {
                expectedPvcDeletions.add("data-" + assemblyName + "-kafka-" + i);
            }
            for (int i = 0; deleteClaim && i < zookeeperCluster.getReplicas(); i++) {
                expectedPvcDeletions.add("data-" + assemblyName + "-zookeeper-" + i);
            }
            context.assertEquals(expectedPvcDeletions, captured(pvcCaptor));

            // if topic operator configuration was defined in the CM
            if (topicOperator != null) {
                Set<String> expectedDepNames = new HashSet<>();
                expectedDepNames.add(TopicOperator.topicOperatorName(assemblyName));
                context.assertEquals(expectedDepNames, captured(depCaptor));
            }

            async.complete();
        });
    }

    private KafkaAssembly getKafkaAssembly(String clusterName) {
        String clusterNamespace = "test";
        int replicas = 3;
        String image = "bar";
        int healthDelay = 120;
        int healthTimeout = 30;
        String metricsCmJson = metrics ? METRICS_CONFIG : null;
        return ResourceUtils.createKafkaCluster(clusterNamespace, clusterName, replicas, image, healthDelay, healthTimeout, metricsCmJson, kafkaConfig, zooConfig, storage, tcConfig, null);
    }

    private List<Secret> getInitialSecrets() {
        String clusterCmNamespace = "test";
        return ResourceUtils.createKafkaClusterInitialSecrets(clusterCmNamespace);
    }

    private List<Secret> getClusterSecrets(String clusterCmName, int replicas) {
        String clusterCmNamespace = "test";
        return ResourceUtils.createKafkaClusterSecretsWithReplicas(clusterCmNamespace, clusterCmName, replicas);
    }

    private static <T> Set<T> captured(ArgumentCaptor<T> captor) {
        return new HashSet<>(captor.getAllValues());
    }

    @Test
    public void testDeleteCluster(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("baz");
        List<Secret> secrets = getClusterSecrets("baz",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        deleteCluster(context, kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateClusterNoop(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("bar");
        List<Secret> secrets = getClusterSecrets("bar",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        updateCluster(context, getKafkaAssembly("bar"), kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateKafkaClusterChangeImage(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("bar");
        kafkaAssembly.getSpec().getKafka().setImage("a-changed-image");
        List<Secret> secrets = getClusterSecrets("bar",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        updateCluster(context, getKafkaAssembly("bar"), kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateZookeeperClusterChangeImage(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("bar");
        kafkaAssembly.getSpec().getZookeeper().setImage("a-changed-image");
        List<Secret> secrets = getClusterSecrets("bar",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        updateCluster(context, getKafkaAssembly("bar"), kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateKafkaClusterScaleUp(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("bar");
        kafkaAssembly.getSpec().getKafka().setReplicas(4);
        List<Secret> secrets = getClusterSecrets("bar",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        updateCluster(context, getKafkaAssembly("bar"), kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateKafkaClusterScaleDown(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("bar");
        kafkaAssembly.getSpec().getKafka().setReplicas(2);
        List<Secret> secrets = getClusterSecrets("bar",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        updateCluster(context, getKafkaAssembly("bar"), kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateZookeeperClusterScaleUp(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("bar");
        kafkaAssembly.getSpec().getZookeeper().setReplicas(4);
        List<Secret> secrets = getClusterSecrets("bar",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        updateCluster(context, getKafkaAssembly("bar"), kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateZookeeperClusterScaleDown(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("bar");
        kafkaAssembly.getSpec().getZookeeper().setReplicas(2);
        List<Secret> secrets = getClusterSecrets("bar",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        updateCluster(context, getKafkaAssembly("bar"), kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateClusterMetricsConfig(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("bar");
        kafkaAssembly.getSpec().getKafka().setMetrics(singletonMap("something", "changed"));
        List<Secret> secrets = getClusterSecrets("bar",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        updateCluster(context, getKafkaAssembly("bar"), kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateZkClusterMetricsConfig(TestContext context) {
        KafkaAssembly kafkaAssembly = getKafkaAssembly("bar");
        kafkaAssembly.getSpec().getZookeeper().setMetrics(singletonMap("something", "changed"));
        List<Secret> secrets = getClusterSecrets("bar",
                kafkaAssembly.getSpec().getKafka().getReplicas());
        updateCluster(context, getKafkaAssembly("bar"), kafkaAssembly, secrets);
    }

    @Test
    public void testUpdateTopicOperatorConfig(TestContext context) {
        KafkaAssembly clusterCm = getKafkaAssembly("bar");
        if (tcConfig != null) {
            //clusterCm.getSpec().getTopicOperator().set;getData().put(TopicOperator.KEY_CONFIG, "{\"something\":\"changed\"}");
            //List<Secret> secrets = getClusterSecrets("bar",
            //        kafkaAssembly.getSpec().getKafka().getReplicas());
            //updateCluster(context, getKafkaAssembly("bar"), clusterCm, secrets);
            context.fail("TODO");
        }
    }


    private void updateCluster(TestContext context, KafkaAssembly originalAssembly, KafkaAssembly updatedAssembly, List<Secret> secrets) {
        KafkaCluster originalKafkaCluster = KafkaCluster.fromCrd(certManager, originalAssembly, secrets);
        KafkaCluster updatedKafkaCluster = KafkaCluster.fromCrd(certManager, updatedAssembly, secrets);
        ZookeeperCluster originalZookeeperCluster = ZookeeperCluster.fromCrd(originalAssembly);
        ZookeeperCluster updatedZookeeperCluster = ZookeeperCluster.fromCrd(updatedAssembly);
        TopicOperator originalTopicOperator = TopicOperator.fromCrd(originalAssembly);

        // create CM, Service, headless service, statefulset and so on
        CrdOperator mockKafkaOps = mock(CrdOperator.class);
        ConfigMapOperator mockCmOps = mock(ConfigMapOperator.class);
        ServiceOperator mockServiceOps = mock(ServiceOperator.class);
        ZookeeperSetOperator mockZsOps = mock(ZookeeperSetOperator.class);
        KafkaSetOperator mockKsOps = mock(KafkaSetOperator.class);
        PvcOperator mockPvcOps = mock(PvcOperator.class);
        DeploymentOperator mockDepOps = mock(DeploymentOperator.class);
        SecretOperator mockSecretOps = mock(SecretOperator.class);

        String clusterName = updatedAssembly.getMetadata().getName();
        String clusterNamespace = updatedAssembly.getMetadata().getNamespace();

        // Mock CM get
        when(mockKafkaOps.get(clusterNamespace, clusterName)).thenReturn(updatedAssembly);
        ConfigMap metricsCm = new ConfigMapBuilder().withNewMetadata()
                    .withName(KafkaCluster.metricConfigsName(clusterName))
                    .withNamespace(clusterNamespace)
                .endMetadata()
                .withData(Collections.singletonMap(AbstractModel.METRICS_CONFIG_FILE, METRICS_CONFIG))
                .build();
        when(mockCmOps.get(clusterNamespace, KafkaCluster.metricConfigsName(clusterName))).thenReturn(metricsCm);
        ConfigMap zkMetricsCm = new ConfigMapBuilder().withNewMetadata()
                .withName(ZookeeperCluster.zookeeperMetricsName(clusterName))
                .withNamespace(clusterNamespace)
                .endMetadata()
                .withData(Collections.singletonMap(AbstractModel.METRICS_CONFIG_FILE, METRICS_CONFIG))
                .build();
        when(mockCmOps.get(clusterNamespace, ZookeeperCluster.zookeeperMetricsName(clusterName))).thenReturn(zkMetricsCm);


        // Mock Service gets
        when(mockServiceOps.get(clusterNamespace, KafkaCluster.kafkaClusterName(clusterName))).thenReturn(
                originalKafkaCluster.generateService()
        );
        when(mockServiceOps.get(clusterNamespace, KafkaCluster.headlessName(clusterName))).thenReturn(
                originalKafkaCluster.generateHeadlessService()
        );
        when(mockServiceOps.get(clusterNamespace, ZookeeperCluster.zookeeperClusterName(clusterName))).thenReturn(
                originalKafkaCluster.generateService()
        );
        when(mockServiceOps.get(clusterNamespace, ZookeeperCluster.zookeeperHeadlessName(clusterName))).thenReturn(
                originalZookeeperCluster.generateHeadlessService()
        );
        when(mockServiceOps.endpointReadiness(eq(clusterNamespace), any(), anyLong(), anyLong())).thenReturn(
                Future.succeededFuture()
        );

        // Mock Secret gets
        when(mockSecretOps.get(clusterNamespace, KafkaCluster.clientsCASecretName(clusterName))).thenReturn(
                originalKafkaCluster.generateClientsCASecret()
        );
        when(mockSecretOps.get(clusterNamespace, KafkaCluster.clientsPublicKeyName(clusterName))).thenReturn(
                originalKafkaCluster.generateClientsPublicKeySecret()
        );
        when(mockSecretOps.get(clusterNamespace, KafkaCluster.brokersClientsSecret(clusterName))).thenReturn(
                originalKafkaCluster.generateBrokersClientsSecret()
        );
        when(mockSecretOps.get(clusterNamespace, KafkaCluster.brokersInternalSecretName(clusterName))).thenReturn(
                originalKafkaCluster.generateBrokersInternalSecret()
        );

        // Mock StatefulSet get
        when(mockKsOps.get(clusterNamespace, KafkaCluster.kafkaClusterName(clusterName))).thenReturn(
                originalKafkaCluster.generateStatefulSet(openShift)
        );
        when(mockZsOps.get(clusterNamespace, ZookeeperCluster.zookeeperClusterName(clusterName))).thenReturn(
                originalZookeeperCluster.generateStatefulSet(openShift)
        );
        // Mock Deployment get
        if (originalTopicOperator != null) {
            when(mockDepOps.get(clusterNamespace, TopicOperator.topicOperatorName(clusterName))).thenReturn(
                    originalTopicOperator.generateDeployment()
            );
        }

        // Mock CM patch
        Set<String> metricsCms = set();
        doAnswer(invocation -> {
            metricsCms.add(invocation.getArgument(1));
            return Future.succeededFuture();
        }).when(mockCmOps).reconcile(eq(clusterNamespace), anyString(), any());

        // Mock Service patch (both service and headless service
        ArgumentCaptor<String> patchedServicesCaptor = ArgumentCaptor.forClass(String.class);
        when(mockServiceOps.reconcile(eq(clusterNamespace), patchedServicesCaptor.capture(), any())).thenReturn(Future.succeededFuture());
        // Mock Secrets patch
        when(mockSecretOps.reconcile(eq(clusterNamespace), any(), any())).thenReturn(Future.succeededFuture());

        // Mock StatefulSet patch
        when(mockZsOps.reconcile(anyString(), anyString(), any())).thenAnswer(invocation -> {
            StatefulSet ss = invocation.getArgument(2);
            return Future.succeededFuture(ReconcileResult.patched(ss));
        });
        when(mockKsOps.reconcile(anyString(), anyString(), any())).thenAnswer(invocation -> {
            StatefulSet ss = invocation.getArgument(2);
            return Future.succeededFuture(ReconcileResult.patched(ss));
        });
        when(mockZsOps.maybeRollingUpdate(any())).thenReturn(Future.succeededFuture());
        when(mockKsOps.maybeRollingUpdate(any())).thenReturn(Future.succeededFuture());

        // Mock StatefulSet scaleUp
        ArgumentCaptor<String> scaledUpCaptor = ArgumentCaptor.forClass(String.class);
        when(mockZsOps.scaleUp(anyString(), scaledUpCaptor.capture(), anyInt())).thenReturn(
                Future.succeededFuture(42)
        );
        // Mock StatefulSet scaleDown
        ArgumentCaptor<String> scaledDownCaptor = ArgumentCaptor.forClass(String.class);
        when(mockZsOps.scaleDown(anyString(), scaledDownCaptor.capture(), anyInt())).thenReturn(
                Future.succeededFuture(42)
        );
        //ArgumentCaptor<String> scaledUpCaptor = ArgumentCaptor.forClass(String.class);
        when(mockKsOps.scaleUp(anyString(), scaledUpCaptor.capture(), anyInt())).thenReturn(
                Future.succeededFuture(42)
        );
        // Mock StatefulSet scaleDown
        //ArgumentCaptor<String> scaledDownCaptor = ArgumentCaptor.forClass(String.class);
        when(mockKsOps.scaleDown(anyString(), scaledDownCaptor.capture(), anyInt())).thenReturn(
                Future.succeededFuture(42)
        );

        // Mock Deployment patch
        ArgumentCaptor<String> depCaptor = ArgumentCaptor.forClass(String.class);
        when(mockDepOps.reconcile(anyString(), depCaptor.capture(), any())).thenReturn(Future.succeededFuture());

        KafkaAssemblyOperator ops = new KafkaAssemblyOperator(vertx, openShift,
                ClusterOperatorConfig.DEFAULT_OPERATION_TIMEOUT_MS,
                certManager,
                mockKafkaOps,
                mockCmOps,
                mockServiceOps, mockZsOps, mockKsOps,
                mockPvcOps, mockDepOps, mockSecretOps);

        // Now try to update a KafkaCluster based on this CM
        Async async = context.async();
        ops.createOrUpdate(new Reconciliation("test-trigger", AssemblyType.KAFKA, clusterNamespace, clusterName), updatedAssembly, secrets, createResult -> {
            if (createResult.failed()) createResult.cause().printStackTrace();
            context.assertTrue(createResult.succeeded());

            // rolling restart
            Set<String> expectedRollingRestarts = set();
            if (KafkaSetOperator.needsRollingUpdate(
                    new StatefulSetDiff(originalKafkaCluster.generateStatefulSet(openShift),
                    updatedKafkaCluster.generateStatefulSet(openShift)))) {
                expectedRollingRestarts.add(originalKafkaCluster.getName());
            }
            if (ZookeeperSetOperator.needsRollingUpdate(
                    new StatefulSetDiff(originalZookeeperCluster.generateStatefulSet(openShift),
                            updatedZookeeperCluster.generateStatefulSet(openShift)))) {
                expectedRollingRestarts.add(originalZookeeperCluster.getName());
            }

            // No metrics config  => no CMs created
            verify(mockCmOps, never()).createOrUpdate(any());
            verifyNoMoreInteractions(mockPvcOps);
            async.complete();
        });
    }

    @Test
    public void testReconcile(TestContext context) throws InterruptedException {
        Async async = context.async(3);

        // create CM, Service, headless service, statefulset
        CrdOperator mockKafkaOps = mock(CrdOperator.class);
        ConfigMapOperator mockCmOps = mock(ConfigMapOperator.class);
        ServiceOperator mockServiceOps = mock(ServiceOperator.class);
        ZookeeperSetOperator mockZsOps = mock(ZookeeperSetOperator.class);
        KafkaSetOperator mockKsOps = mock(KafkaSetOperator.class);
        PvcOperator mockPvcOps = mock(PvcOperator.class);
        DeploymentOperator mockDepOps = mock(DeploymentOperator.class);
        SecretOperator mockSecretOps = mock(SecretOperator.class);

        String clusterCmNamespace = "myNamespace";

        KafkaAssembly foo = getKafkaAssembly("foo");
        KafkaAssembly bar = getKafkaAssembly("bar");
        KafkaAssembly baz = getKafkaAssembly("baz");
        when(mockKafkaOps.list(eq(clusterCmNamespace), any())).thenReturn(
            asList(foo, bar)
        );
        // when requested ConfigMap for a specific Kafka cluster
        when(mockKafkaOps.get(eq(clusterCmNamespace), eq("foo"))).thenReturn(foo);
        when(mockKafkaOps.get(eq(clusterCmNamespace), eq("bar"))).thenReturn(bar);

        // providing certificates Secrets for existing clusters
        List<Secret> barSecrets = ResourceUtils.createKafkaClusterSecretsWithReplicas(clusterCmNamespace, "bar",
                bar.getSpec().getKafka().getReplicas());
        List<Secret> bazSecrets = ResourceUtils.createKafkaClusterSecretsWithReplicas(clusterCmNamespace, "baz",
                baz.getSpec().getKafka().getReplicas());

        // providing the list of ALL StatefulSets for all the Kafka clusters
        Labels newLabels = Labels.forType(AssemblyType.KAFKA);
        when(mockKsOps.list(eq(clusterCmNamespace), eq(newLabels))).thenReturn(
                asList(KafkaCluster.fromCrd(certManager, bar, barSecrets).generateStatefulSet(openShift),
                        KafkaCluster.fromCrd(certManager, baz, bazSecrets).generateStatefulSet(openShift))
        );

        // providing the list StatefulSets for already "existing" Kafka clusters
        Labels barLabels = Labels.forCluster("bar");
        KafkaCluster barCluster = KafkaCluster.fromCrd(certManager, bar, barSecrets);
        when(mockKsOps.list(eq(clusterCmNamespace), eq(barLabels))).thenReturn(
                asList(barCluster.generateStatefulSet(openShift))
        );
        when(mockSecretOps.list(eq(clusterCmNamespace), eq(barLabels))).thenReturn(
                new ArrayList<>(asList(barCluster.generateClientsCASecret(), barCluster.generateClientsPublicKeySecret(),
                        barCluster.generateBrokersInternalSecret(), barCluster.generateBrokersClientsSecret()))
        );
        when(mockSecretOps.get(eq(clusterCmNamespace), eq(AbstractAssemblyOperator.INTERNAL_CA_NAME))).thenReturn(barSecrets.get(0));

        Labels bazLabels = Labels.forCluster("baz");
        KafkaCluster bazCluster = KafkaCluster.fromCrd(certManager, baz, bazSecrets);
        when(mockKsOps.list(eq(clusterCmNamespace), eq(bazLabels))).thenReturn(
                asList(bazCluster.generateStatefulSet(openShift))
        );
        when(mockSecretOps.list(eq(clusterCmNamespace), eq(bazLabels))).thenReturn(
                new ArrayList<>(asList(bazCluster.generateClientsCASecret(), bazCluster.generateClientsPublicKeySecret(),
                        bazCluster.generateBrokersInternalSecret(), bazCluster.generateBrokersClientsSecret()))
        );
        when(mockSecretOps.get(eq(clusterCmNamespace), eq(AbstractAssemblyOperator.INTERNAL_CA_NAME))).thenReturn(bazSecrets.get(0));

        Set<String> createdOrUpdated = new CopyOnWriteArraySet<>();
        Set<String> deleted = new CopyOnWriteArraySet<>();

        KafkaAssemblyOperator ops = new KafkaAssemblyOperator(vertx, openShift,
                ClusterOperatorConfig.DEFAULT_OPERATION_TIMEOUT_MS,
                certManager,
                mockKafkaOps,
                mockCmOps,
                mockServiceOps, mockZsOps, mockKsOps,
                mockPvcOps, mockDepOps, mockSecretOps) {
            @Override
            public void createOrUpdate(Reconciliation reconciliation, KafkaAssembly assemblyCm, List<Secret> assemblySecrets, Handler<AsyncResult<Void>> h) {
                createdOrUpdated.add(assemblyCm.getMetadata().getName());
                async.countDown();
                h.handle(Future.succeededFuture());
            }
            @Override
            public void delete(Reconciliation reconciliation, Handler h) {
                deleted.add(reconciliation.assemblyName());
                async.countDown();
                h.handle(Future.succeededFuture());
            }
        };

        // Now try to reconcile all the Kafka clusters
        ops.reconcileAll("test", clusterCmNamespace, Labels.EMPTY).await();

        async.await();

        context.assertEquals(new HashSet(asList("foo", "bar")), createdOrUpdated);
        context.assertEquals(singleton("baz"), deleted);
    }
}
