/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.strimzi.api.kafka.model.JsonUtils;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaAssembly;
import io.strimzi.api.kafka.model.KafkaAssemblySpec;
import io.strimzi.api.kafka.model.KafkaConnectAssembly;
import io.strimzi.api.kafka.model.KafkaConnectAssemblyBuilder;
import io.strimzi.api.kafka.model.KafkaConnectS2IAssembly;
import io.strimzi.api.kafka.model.KafkaConnectS2IAssemblyBuilder;
import io.strimzi.api.kafka.model.Probe;
import io.strimzi.api.kafka.model.RackConfig;
import io.strimzi.api.kafka.model.Storage;
import io.strimzi.api.kafka.model.Zookeeper;
import io.strimzi.operator.cluster.model.AssemblyType;
import io.strimzi.operator.cluster.model.KafkaCluster;
import io.strimzi.operator.cluster.model.KafkaConnectCluster;
import io.strimzi.operator.cluster.model.KafkaConnectS2ICluster;
import io.strimzi.operator.cluster.model.Labels;
import io.strimzi.operator.cluster.model.TopicOperator;
import io.strimzi.operator.cluster.model.ZookeeperCluster;
import io.strimzi.operator.cluster.operator.assembly.AbstractAssemblyOperator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;

public class ResourceUtils {

    private ResourceUtils() {

    }

    /**
     * Creates a map of labels
     * @param pairs (key, value) pairs. There must be an even number, obviously.
     * @return a map of labels
     * @deprecated Use method method in TestUtils
     */
    @Deprecated
    public static Map<String, String> labels(String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    /**
     * Creates a cluster ConfigMap
     * @param clusterCmNamespace
     * @param clusterCmName
     * @param replicas
     * @param image
     * @param healthDelay
     * @param healthTimeout
     * @return
     */
    public static ConfigMap createKafkaClusterConfigMap(String clusterCmNamespace, String clusterCmName, int replicas,
                                                        String image, int healthDelay, int healthTimeout,
                                                        String metricsCmJson) {
        return createKafkaClusterConfigMap(clusterCmNamespace, clusterCmName, replicas, image, healthDelay,
                healthTimeout, metricsCmJson, "{}", "{}",
                "{\"type\": \"ephemeral\"}", null, null);
    }
    public static ConfigMap createKafkaClusterConfigMap(String clusterCmNamespace, String clusterCmName, int replicas,
                                                        String image, int healthDelay, int healthTimeout,
                                                        String metricsCmJson, String kafkaConfigurationJson) {
        return createKafkaClusterConfigMap(clusterCmNamespace, clusterCmName, replicas, image, healthDelay,
                healthTimeout, metricsCmJson, kafkaConfigurationJson, "{}",
                "{\"type\": \"ephemeral\"}", null, null);
    }
    public static ConfigMap createKafkaClusterConfigMap(String clusterCmNamespace, String clusterCmName, int replicas,
                                                        String image, int healthDelay, int healthTimeout,
                                                        String metricsCmJson, String kafkaConfigurationJson,
                                                        String zooConfigurationJson) {
        return createKafkaClusterConfigMap(clusterCmNamespace, clusterCmName, replicas, image, healthDelay,
                healthTimeout, metricsCmJson, kafkaConfigurationJson, zooConfigurationJson,
                "{\"type\": \"ephemeral\"}", null, null);
    }

    public static KafkaAssembly createKafkaCluster(String clusterCmNamespace, String clusterCmName, int replicas,
                                                   String image, int healthDelay, int healthTimeout,
                                                   String metricsCmJson) {
        return createKafkaCluster(clusterCmNamespace, clusterCmName, replicas, image, healthDelay,
                healthTimeout, metricsCmJson, "{}", "{}",
                "{\"type\": \"ephemeral\"}", null, null);
    }

    public static KafkaAssembly createKafkaCluster(String clusterCmNamespace, String clusterCmName, int replicas,
                                                        String image, int healthDelay, int healthTimeout,
                                                        String metricsCmJson, String kafkaConfigurationJson,
                                                        String zooConfigurationJson) {
        return createKafkaCluster(clusterCmNamespace, clusterCmName, replicas, image, healthDelay,
                healthTimeout, metricsCmJson, kafkaConfigurationJson, zooConfigurationJson,
                "{\"type\": \"ephemeral\"}", null, null);
    }
    public static ConfigMap createKafkaClusterConfigMap(String clusterCmNamespace, String clusterCmName, int replicas,
                                                        String image, int healthDelay, int healthTimeout,
                                                        String metricsCmJson, String kafkaConfigurationJson,
                                                        String zooConfigurationJson, String storage) {
        return createKafkaClusterConfigMap(clusterCmNamespace, clusterCmName, replicas, image, healthDelay,
                healthTimeout, metricsCmJson, kafkaConfigurationJson, zooConfigurationJson,
                storage, null, null);
    }

    public static ConfigMap createKafkaClusterConfigMap(String clusterCmNamespace, String clusterCmName, int replicas,
                                                        String image, int healthDelay, int healthTimeout, String metricsCmJson,
                                                        String kafkaConfigurationJson, String zooConfigurationJson,
                                                        String storage, String topicOperator, String rackJson) {
        Map<String, String> cmData = new HashMap<>();
        cmData.put(KafkaCluster.KEY_REPLICAS, Integer.toString(replicas));
        cmData.put(KafkaCluster.KEY_IMAGE, image);
        cmData.put(KafkaCluster.KEY_HEALTHCHECK_DELAY, Integer.toString(healthDelay));
        cmData.put(KafkaCluster.KEY_HEALTHCHECK_TIMEOUT, Integer.toString(healthTimeout));
        cmData.put(KafkaCluster.KEY_STORAGE, storage);
        cmData.put(KafkaCluster.KEY_METRICS_CONFIG, metricsCmJson);
        if (kafkaConfigurationJson != null) {
            cmData.put(KafkaCluster.KEY_KAFKA_CONFIG, kafkaConfigurationJson);
        }
        cmData.put(ZookeeperCluster.KEY_REPLICAS, Integer.toString(replicas));
        cmData.put(ZookeeperCluster.KEY_IMAGE, image + "-zk");
        cmData.put(ZookeeperCluster.KEY_HEALTHCHECK_DELAY, Integer.toString(healthDelay));
        cmData.put(ZookeeperCluster.KEY_HEALTHCHECK_TIMEOUT, Integer.toString(healthTimeout));
        if (zooConfigurationJson != null) {
            cmData.put(ZookeeperCluster.KEY_ZOOKEEPER_CONFIG, zooConfigurationJson);
        }
        cmData.put(ZookeeperCluster.KEY_STORAGE, storage);
        cmData.put(ZookeeperCluster.KEY_METRICS_CONFIG, metricsCmJson);
        if (topicOperator != null) {
            cmData.put(TopicOperator.KEY_CONFIG, topicOperator);
        }
        if (rackJson != null) {
            cmData.put(KafkaCluster.KEY_RACK, rackJson);
        }
        return new ConfigMapBuilder()
                .withNewMetadata()
                    .withName(clusterCmName)
                    .withNamespace(clusterCmNamespace)
                    .withLabels(Labels.userLabels(singletonMap("my-user-label", "cromulent")).withKind("cluster").withType(AssemblyType.KAFKA).toMap())
                .endMetadata()
                .withData(cmData)
                .build();
    }

    public static List<Secret> createKafkaClusterInitialSecrets(String clusterCmNamespace) {

        List<Secret> secrets = new ArrayList<>();

        Map<String, String> data = new HashMap<>();
        data.put("internal-ca.key", Base64.getEncoder().encodeToString("internal-ca-base64key".getBytes()));
        data.put("internal-ca.crt", Base64.getEncoder().encodeToString("internal-ca-base64crt".getBytes()));
        secrets.add(
                new SecretBuilder()
                .withNewMetadata()
                    .withName(AbstractAssemblyOperator.INTERNAL_CA_NAME)
                    .withNamespace(clusterCmNamespace)
                .endMetadata()
                .withData(data)
                .build()
        );
        return secrets;
    }

    public static List<Secret> createKafkaClusterSecretsWithReplicas(String clusterCmNamespace, String clusterCmName, int replicas) {

        List<Secret> secrets = new ArrayList<>();

        Map<String, String> data = new HashMap<>();
        data.put("internal-ca.key", Base64.getEncoder().encodeToString("internal-ca-base64key".getBytes()));
        data.put("internal-ca.crt", Base64.getEncoder().encodeToString("internal-ca-base64crt".getBytes()));
        secrets.add(
                new SecretBuilder()
                        .withNewMetadata()
                        .withName(AbstractAssemblyOperator.INTERNAL_CA_NAME)
                        .withNamespace(clusterCmNamespace)
                        .endMetadata()
                        .withData(data)
                        .build()
        );

        data = new HashMap<>();
        data.put("clients-ca.key", Base64.getEncoder().encodeToString("clients-ca-base64key".getBytes()));
        data.put("clients-ca.crt", Base64.getEncoder().encodeToString("clients-ca-base64crt".getBytes()));
        secrets.add(
                new SecretBuilder()
                        .withNewMetadata()
                        .withName(KafkaCluster.clientsCASecretName(clusterCmName))
                        .withNamespace(clusterCmNamespace)
                        .withLabels(Labels.forCluster(clusterCmName).withType(AssemblyType.KAFKA).toMap())
                        .endMetadata()
                        .withData(data)
                        .build()
        );

        data = new HashMap<>();
        data.put("clients-ca.crt", Base64.getEncoder().encodeToString("clients-ca-base64crt".getBytes()));
        secrets.add(
                new SecretBuilder()
                        .withNewMetadata()
                        .withName(KafkaCluster.clientsPublicKeyName(clusterCmName))
                        .withNamespace(clusterCmNamespace)
                        .withLabels(Labels.forCluster(clusterCmName).withType(AssemblyType.KAFKA).toMap())
                        .endMetadata()
                        .withData(data)
                        .build()
        );

        data = new HashMap<>();
        data.put("internal-ca.crt", Base64.getEncoder().encodeToString("internal-ca-base64crt".getBytes()));
        for (int i = 0; i < replicas; i++) {
            data.put(KafkaCluster.kafkaPodName(clusterCmName, i) + ".key", Base64.getEncoder().encodeToString("brokers-internal-base64key".getBytes()));
            data.put(KafkaCluster.kafkaPodName(clusterCmName, i) + ".crt", Base64.getEncoder().encodeToString("brokers-internal-base64crt".getBytes()));
        }
        secrets.add(
                new SecretBuilder()
                        .withNewMetadata()
                        .withName(KafkaCluster.brokersInternalSecretName(clusterCmName))
                        .withNamespace(clusterCmNamespace)
                        .withLabels(Labels.forCluster(clusterCmName).withType(AssemblyType.KAFKA).toMap())
                        .endMetadata()
                        .withData(data)
                        .build()
        );

        data = new HashMap<>();
        data.put("internal-ca.crt", Base64.getEncoder().encodeToString("internal-ca-base64crt".getBytes()));
        data.put("clients-ca.crt", Base64.getEncoder().encodeToString("clients-ca-base64crt".getBytes()));
        for (int i = 0; i < replicas; i++) {
            data.put(KafkaCluster.kafkaPodName(clusterCmName, i) + ".key", Base64.getEncoder().encodeToString("brokers-clients-base64key".getBytes()));
            data.put(KafkaCluster.kafkaPodName(clusterCmName, i) + ".crt", Base64.getEncoder().encodeToString("brokers-clients-base64crt".getBytes()));
        }
        secrets.add(
                new SecretBuilder()
                        .withNewMetadata()
                        .withName(KafkaCluster.brokersClientsSecret(clusterCmName))
                        .withNamespace(clusterCmNamespace)
                        .withLabels(Labels.forCluster(clusterCmName).withType(AssemblyType.KAFKA).toMap())
                        .endMetadata()
                        .withData(data)
                        .build()
        );
        return secrets;
    }

    public static KafkaAssembly createKafkaCluster(String clusterCmNamespace, String clusterCmName, int replicas,
                                                   String image, int healthDelay, int healthTimeout,
                                                   String metricsCmJson, String kafkaConfigurationJson) {
        return createKafkaCluster(clusterCmNamespace, clusterCmName, replicas, image, healthDelay,
                healthTimeout, metricsCmJson, kafkaConfigurationJson, "{}",
                "{\"type\": \"ephemeral\"}", null, null);
    }

    public static KafkaAssembly createKafkaCluster(String clusterCmNamespace, String clusterCmName, int replicas,
                                                    String image, int healthDelay, int healthTimeout, String metricsCmJson,
                                                    String kafkaConfigurationJson, String zooConfigurationJson,
                                                    String storageJson, String topicOperator, String rackJson) {
        try {
            KafkaAssembly result = new KafkaAssembly();
            ObjectMeta meta = new ObjectMeta();
            meta.setNamespace(clusterCmNamespace);
            meta.setName(clusterCmName);
            meta.setLabels(Labels.userLabels(singletonMap("my-user-label", "cromulent")).toMap());
            result.setMetadata(meta);

            KafkaAssemblySpec spec = new KafkaAssemblySpec();

            Kafka kafka = new Kafka();
            kafka.setReplicas(replicas);
            kafka.setImage(image);
            Probe livenessProbe = new Probe();
            livenessProbe.setInitialDelaySeconds(healthDelay);
            livenessProbe.setTimeoutSeconds(healthTimeout);
            kafka.setLivenessProbe(livenessProbe);
            kafka.setReadinessProbe(livenessProbe);
            ObjectMapper om = new ObjectMapper();
            TypeReference<HashMap<String, Object>> typeRef
                    = new TypeReference<HashMap<String, Object>>() { };
            if (metricsCmJson != null) {
                kafka.setMetrics(om.readValue(metricsCmJson, typeRef));
            }
            if (kafkaConfigurationJson != null) {
                kafka.setConfig(om.readValue(kafkaConfigurationJson, typeRef));
            }
            kafka.setStorage(JsonUtils.fromJson(storageJson, Storage.class));
            kafka.setRackConfig(RackConfig.fromJson(rackJson));
            spec.setKafka(kafka);

            Zookeeper zk = new Zookeeper();
            zk.setReplicas(replicas);
            zk.setImage(image + "-zk");
            zk.setLivenessProbe(livenessProbe);
            zk.setReadinessProbe(livenessProbe);
            if (zooConfigurationJson != null) {
                zk.setConfig(om.readValue(zooConfigurationJson, typeRef));
            }
            zk.setStorage(JsonUtils.fromJson(storageJson, Storage.class));
            if (metricsCmJson != null) {
                zk.setMetrics(om.readValue(metricsCmJson, typeRef));
            }

            spec.setTopicOperator(io.strimzi.api.kafka.model.TopicOperator.fromJson(topicOperator));

            spec.setZookeeper(zk);
            result.setSpec(spec);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate ConfigMap for Kafka Connect S2I cluster
     */
    public static ConfigMap createKafkaConnectS2IClusterConfigMap(String clusterCmNamespace, String clusterCmName, int replicas,
                                                                  String image, int healthDelay, int healthTimeout, String metricsCmJson,
                                                                  String connectConfig, boolean insecureSourceRepo) {
        Map<String, String> cmData = new HashMap<>();
        cmData.put(KafkaConnectS2ICluster.KEY_IMAGE, image);
        cmData.put(KafkaConnectS2ICluster.KEY_REPLICAS, Integer.toString(replicas));
        cmData.put(KafkaConnectS2ICluster.KEY_HEALTHCHECK_DELAY, Integer.toString(healthDelay));
        cmData.put(KafkaConnectS2ICluster.KEY_HEALTHCHECK_TIMEOUT, Integer.toString(healthTimeout));
        cmData.put(KafkaConnectCluster.KEY_METRICS_CONFIG, metricsCmJson);
        if (connectConfig != null) {
            cmData.put(KafkaConnectS2ICluster.KEY_CONNECT_CONFIG, connectConfig);
        }
        cmData.put(KafkaConnectS2ICluster.KEY_INSECURE_SOURCE_REPO, String.valueOf(insecureSourceRepo));

        ConfigMap cm = createEmptyKafkaConnectS2IClusterConfigMap(clusterCmNamespace, clusterCmName);
        cm.setData(cmData);

        return cm;
    }

    /**
     * Generate empty Kafka Connect S2I ConfigMap
     */
    public static ConfigMap createEmptyKafkaConnectS2IClusterConfigMap(String clusterCmNamespace, String clusterCmName) {
        Map<String, String> cmData = new HashMap<>();

        return new ConfigMapBuilder()
                .withNewMetadata()
                .withName(clusterCmName)
                .withNamespace(clusterCmNamespace)
                .withLabels(labels(Labels.STRIMZI_KIND_LABEL, "cluster",
                        Labels.STRIMZI_TYPE_LABEL, "kafka-connect-s2i",
                        "my-user-label", "cromulent"))
                .endMetadata()
                .withData(cmData)
                .build();
    }

    /**
     * Generate ConfigMap for Kafka Connect S2I cluster
     */
    public static KafkaConnectS2IAssembly createKafkaConnectS2ICluster(String clusterCmNamespace, String clusterCmName, int replicas,
                                                                       String image, int healthDelay, int healthTimeout, String metricsCmJson,
                                                                       String connectConfig, boolean insecureSourceRepo) {

        return new KafkaConnectS2IAssemblyBuilder(createEmptyKafkaConnectS2ICluster(clusterCmNamespace, clusterCmName))
                .withNewSpec()
                    .withImage(image)
                    .withReplicas(replicas)
                    .withLivenessProbe(new Probe(healthDelay, healthTimeout))
                    .withReadinessProbe(new Probe(healthDelay, healthTimeout))
                    .withMetrics((Map<String, Object>) JsonUtils.fromJson(metricsCmJson, Map.class))
                    .withConfig((Map<String, Object>) JsonUtils.fromJson(connectConfig, Map.class))
                    .withInsecureSourceRepository(insecureSourceRepo)
                .endSpec().build();
    }

    /**
     * Generate empty Kafka Connect S2I ConfigMap
     */
    public static KafkaConnectS2IAssembly createEmptyKafkaConnectS2ICluster(String clusterCmNamespace, String clusterCmName) {
        return new KafkaConnectS2IAssemblyBuilder()
                .withMetadata(new ObjectMetaBuilder()
                .withName(clusterCmName)
                .withNamespace(clusterCmNamespace)
                .withLabels(labels(Labels.STRIMZI_KIND_LABEL, "cluster",
                        Labels.STRIMZI_TYPE_LABEL, "kafka-connect-s2i",
                        "my-user-label", "cromulent"))
                .build())
                .withNewSpec().endSpec()
                .build();
    }

    /**
     * Generate ConfigMap for Kafka Connect cluster
     */
    public static ConfigMap createKafkaConnectClusterConfigMap(String clusterCmNamespace, String clusterCmName, int replicas,
                                                                  String image, int healthDelay, int healthTimeout, String metricsCmJson, String connectConfig) {
        Map<String, String> cmData = new HashMap<>();
        cmData.put(KafkaConnectCluster.KEY_IMAGE, image);
        cmData.put(KafkaConnectCluster.KEY_REPLICAS, Integer.toString(replicas));
        cmData.put(KafkaConnectCluster.KEY_HEALTHCHECK_DELAY, Integer.toString(healthDelay));
        cmData.put(KafkaConnectCluster.KEY_HEALTHCHECK_TIMEOUT, Integer.toString(healthTimeout));
        cmData.put(KafkaConnectCluster.KEY_METRICS_CONFIG, metricsCmJson);
        if (connectConfig != null) {
            cmData.put(KafkaConnectCluster.KEY_CONNECT_CONFIG, connectConfig);
        }

        ConfigMap cm = createEmptyKafkaConnectClusterConfigMap(clusterCmNamespace, clusterCmName);
        cm.setData(cmData);

        return cm;
    }

    /**
     * Generate empty Kafka Connect ConfigMap
     */
    public static ConfigMap createEmptyKafkaConnectClusterConfigMap(String clusterCmNamespace, String clusterCmName) {
        Map<String, String> cmData = new HashMap<>();

        return new ConfigMapBuilder()
                .withNewMetadata()
                .withName(clusterCmName)
                .withNamespace(clusterCmNamespace)
                .withLabels(labels(Labels.STRIMZI_KIND_LABEL, "cluster",
                        Labels.STRIMZI_TYPE_LABEL, "kafka-connect",
                        "my-user-label", "cromulent"))
                .endMetadata()
                .withData(cmData)
                .build();
    }


    public static KafkaConnectAssembly createKafkaConnectCluster(String clusterCmNamespace, String clusterCmName, int replicas,
                                                                 String image, int healthDelay, int healthTimeout, String metricsCmJson, String connectConfig) {

        KafkaConnectAssembly cm = createEmptyKafkaConnectCluster(clusterCmNamespace, clusterCmName);
        return new KafkaConnectAssemblyBuilder(cm)
                .withNewSpec()
                    .withMetrics((Map<String, Object>) JsonUtils.fromJson(metricsCmJson, Map.class))
                    .withConfig((Map<String, Object>) JsonUtils.fromJson(connectConfig, Map.class))
                    .withImage(image)
                    .withReplicas(replicas)
                    .withReadinessProbe(new Probe(healthDelay, healthTimeout))
                    .withLivenessProbe(new Probe(healthDelay, healthTimeout))
                .endSpec()
            .build();

    }

    /**
     * Generate empty Kafka Connect ConfigMap
     */
    public static KafkaConnectAssembly createEmptyKafkaConnectCluster(String clusterCmNamespace, String clusterCmName) {
        return new KafkaConnectAssemblyBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(clusterCmName)
                        .withNamespace(clusterCmNamespace)
                        .withLabels(labels(Labels.STRIMZI_KIND_LABEL, "cluster",
                                Labels.STRIMZI_TYPE_LABEL, "kafka-connect",
                                "my-user-label", "cromulent"))
                        .build())
                .withNewSpec().endSpec()
                .build();
    }
}
