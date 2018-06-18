/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.strimzi.operator.cluster.crd.model.JsonUtils;
import io.strimzi.operator.cluster.crd.model.Kafka;
import io.strimzi.operator.cluster.crd.model.KafkaAssembly;
import io.strimzi.operator.cluster.crd.model.KafkaAssemblySpec;
import io.strimzi.operator.cluster.crd.model.Probe;
import io.strimzi.operator.cluster.crd.model.RackConfig;
import io.strimzi.operator.cluster.crd.model.Storage;
import io.strimzi.operator.cluster.model.AssemblyType;
import io.strimzi.operator.cluster.model.KafkaCluster;
import io.strimzi.operator.cluster.model.KafkaConnectCluster;
import io.strimzi.operator.cluster.model.KafkaConnectS2ICluster;
import io.strimzi.operator.cluster.model.Labels;
import io.strimzi.operator.cluster.model.TopicOperator;
import io.strimzi.operator.cluster.model.ZookeeperCluster;
import io.strimzi.operator.cluster.operator.assembly.AbstractAssemblyOperator;

import java.util.ArrayList;
import java.util.Base64;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

public class ResourceUtils {

    private ResourceUtils() {

    }

    /**
     * Creates a map of labels
     * @param pairs (key, value) pairs. There must be an even number, obviously.
     * @return a map of labels
     */
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
            result.setMetadata(meta);
            KafkaAssemblySpec spec = new KafkaAssemblySpec();
            Kafka kafka = new Kafka();
            meta.setNamespace(clusterCmNamespace);
            meta.setName(clusterCmName);
            meta.setLabels(Labels.userLabels(singletonMap("my-user-label", "cromulent")).withKind("cluster").withType(AssemblyType.KAFKA).toMap());
            kafka.setReplicas(replicas);
            kafka.setImage(image);
            Probe livenessProbe = new Probe();
            livenessProbe.setInitialDelaySeconds(healthDelay);
            livenessProbe.setTimeoutSeconds(healthTimeout);
            kafka.setLivenessProbe(livenessProbe);
            ObjectMapper om = new ObjectMapper();
            TypeReference<HashMap<String, Object>> typeRef
                    = new TypeReference<HashMap<String, Object>>() { };
            kafka.setMetrics(om.readValue(metricsCmJson, typeRef));
            kafka.setConfig(om.readValue(kafkaConfigurationJson, typeRef));
            kafka.setStorage(JsonUtils.fromJson(storageJson, Storage.class));
            kafka.setRackConfig(RackConfig.fromJson(rackJson));
            spec.setKafka(kafka);
            spec.setTopicOperator(io.strimzi.operator.cluster.crd.model.TopicOperator.fromJson(topicOperator));
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

    public static <T> Set<T> set(T... elements) {
        return new HashSet(asList(elements));
    }

    public static <T> Map<T, T> map(T... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        Map<T, T> result = new HashMap<>(pairs.length / 2);
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(pairs[i], pairs[i + 1]);
        }
        return result;
    }

    public static <T> T fromYaml(String resource, Class<T> c) {
        return fromYaml(resource, c, false);
    }

    public static <T> T fromYaml(String resource, Class<T> c, boolean ignoreUnknownProperties) {
        URL url = c.getResource(resource);
        if (url == null) {
            return null;
        }
        ObjectMapper mapper = new YAMLMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, !ignoreUnknownProperties);
        try {
            return mapper.readValue(url, c);
        } catch (InvalidFormatException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> String toYamlString(T instance) {
        ObjectMapper mapper = new YAMLMapper()
                .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writeValueAsString(instance);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
