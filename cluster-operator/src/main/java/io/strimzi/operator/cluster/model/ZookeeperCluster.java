/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.strimzi.api.kafka.model.EphemeralStorage;
import io.strimzi.api.kafka.model.KafkaAssembly;
import io.strimzi.api.kafka.model.PersistentClaimStorage;
import io.strimzi.api.kafka.model.Zookeeper;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ZookeeperCluster extends AbstractModel {

    protected static final int CLIENT_PORT = 2181;
    protected static final String CLIENT_PORT_NAME = "clients";
    protected static final int CLUSTERING_PORT = 2888;
    protected static final String CLUSTERING_PORT_NAME = "clustering";
    protected static final int LEADER_ELECTION_PORT = 3888;
    protected static final String LEADER_ELECTION_PORT_NAME = "leader-election";
    protected static final int METRICS_PORT = 9404;
    protected static final String METRICS_PORT_NAME = "metrics";

    private static final String NAME_SUFFIX = "-zookeeper";
    private static final String HEADLESS_NAME_SUFFIX = NAME_SUFFIX + "-headless";
    private static final String METRICS_AND_LOG_CONFIG_SUFFIX = NAME_SUFFIX + "-config";
    private static final String LOG_CONFIG_SUFFIX = NAME_SUFFIX + "-logging";

    // Zookeeper configuration
    // N/A

    // Configuration defaults

    private static final int DEFAULT_HEALTHCHECK_DELAY = 15;
    private static final int DEFAULT_HEALTHCHECK_TIMEOUT = 5;
    private static final boolean DEFAULT_ZOOKEEPER_METRICS_ENABLED = false;

    // Configuration keys (ConfigMap)
    public static final String KEY_IMAGE = "zookeeper-image";
    public static final String KEY_REPLICAS = "zookeeper-nodes";
    public static final String KEY_HEALTHCHECK_DELAY = "zookeeper-healthcheck-delay";
    public static final String KEY_HEALTHCHECK_TIMEOUT = "zookeeper-healthcheck-timeout";
    public static final String KEY_METRICS_CONFIG = "zookeeper-metrics-config";
    public static final String KEY_STORAGE = "zookeeper-storage";
    public static final String KEY_JVM_OPTIONS = "zookeeper-jvmOptions";
    public static final String KEY_RESOURCES = "zookeeper-resources";
    public static final String KEY_ZOOKEEPER_CONFIG = "zookeeper-config";
    public static final String KEY_AFFINITY = "zookeeper-affinity";
    public static final String KEY_ZOOKEEPER_LOG_CONFIG = "zookeeper-logging";

    // Zookeeper configuration keys (EnvVariables)
    public static final String ENV_VAR_ZOOKEEPER_NODE_COUNT = "ZOOKEEPER_NODE_COUNT";
    public static final String ENV_VAR_ZOOKEEPER_METRICS_ENABLED = "ZOOKEEPER_METRICS_ENABLED";
    public static final String ENV_VAR_ZOOKEEPER_CONFIGURATION = "ZOOKEEPER_CONFIGURATION";
    public static final String ENV_VAR_ZOOKEEPER_LOG_CONFIGURATION = "ZOOKEEPER_LOG_CONFIGURATION";

    public static String zookeeperClusterName(String cluster) {
        return cluster + ZookeeperCluster.NAME_SUFFIX;
    }

    public static String zookeeperMetricAndLogConfigsName(String cluster) {
        return cluster + ZookeeperCluster.METRICS_AND_LOG_CONFIG_SUFFIX;
    }

    public static String logConfigsName(String cluster) {
        return cluster + ZookeeperCluster.LOG_CONFIG_SUFFIX;
    }

    public static String zookeeperHeadlessName(String cluster) {
        return cluster + ZookeeperCluster.HEADLESS_NAME_SUFFIX;
    }

    public static String zookeeperPodName(String cluster, int pod) {
        return zookeeperClusterName(cluster) + "-" + pod;
    }

    public static String getPersistentVolumeClaimName(String clusterName, int podId) {
        return VOLUME_NAME + "-" + clusterName + "-" + podId;
    }

    /**
     * Constructor
     *
     * @param namespace Kubernetes/OpenShift namespace where Zookeeper cluster resources are going to be created
     * @param cluster   overall cluster name
     */
    private ZookeeperCluster(String namespace, String cluster, Labels labels) {

        super(namespace, cluster, labels);
        this.name = zookeeperClusterName(cluster);
        this.headlessName = zookeeperHeadlessName(cluster);
        this.ancillaryConfigName = zookeeperMetricAndLogConfigsName(cluster);
        this.image = Zookeeper.DEFAULT_IMAGE;
        this.replicas = Zookeeper.DEFAULT_REPLICAS;
        this.readinessPath = "/opt/kafka/zookeeper_healthcheck.sh";
        this.readinessTimeout = DEFAULT_HEALTHCHECK_TIMEOUT;
        this.readinessInitialDelay = DEFAULT_HEALTHCHECK_DELAY;
        this.livenessPath = "/opt/kafka/zookeeper_healthcheck.sh";
        this.livenessTimeout = DEFAULT_HEALTHCHECK_TIMEOUT;
        this.livenessInitialDelay = DEFAULT_HEALTHCHECK_DELAY;
        this.isMetricsEnabled = DEFAULT_ZOOKEEPER_METRICS_ENABLED;

        this.mountPath = "/var/lib/zookeeper";

        this.logAndMetricsConfigVolumeName = "zookeeper-metrics-and-logging";
        this.logAndMetricsConfigMountPath = "/opt/kafka/config/";
        this.validLoggerFields = getDefaultLogConfig();
    }

    public static ZookeeperCluster fromCrd(KafkaAssembly kafkaAssembly) {
        ZookeeperCluster zk = new ZookeeperCluster(kafkaAssembly.getMetadata().getNamespace(), kafkaAssembly.getMetadata().getName(),
                Labels.fromResource(kafkaAssembly));
        Zookeeper zookeeper = kafkaAssembly.getSpec().getZookeeper();
        int replicas = zookeeper.getReplicas();
        if (replicas <= 0) {
            replicas = Zookeeper.DEFAULT_REPLICAS;
        }
        zk.setReplicas(replicas);
        String image = zookeeper.getImage();
        if (image == null) {
            image = Zookeeper.DEFAULT_IMAGE;
        }
        zk.setImage(image);
        if (zookeeper.getReadinessProbe() != null) {
            zk.setReadinessInitialDelay(zookeeper.getReadinessProbe().getInitialDelaySeconds());
            zk.setReadinessTimeout(zookeeper.getReadinessProbe().getTimeoutSeconds());
        }
        if (zookeeper.getLivenessProbe() != null) {
            zk.setLivenessInitialDelay(zookeeper.getLivenessProbe().getInitialDelaySeconds());
            zk.setLivenessTimeout(zookeeper.getLivenessProbe().getTimeoutSeconds());
        }
        zk.setLogging(zookeeper.getLogging());
        Map<String, Object> metrics = zookeeper.getMetrics();
        if (metrics != null && !metrics.isEmpty()) {
            zk.setMetricsEnabled(true);
            zk.setMetricsConfig(metrics.entrySet());
        }
        zk.setStorage(zookeeper.getStorage());
        zk.setConfiguration(new ZookeeperConfiguration(zookeeper.getConfig().entrySet()));
        zk.setResources(zookeeper.getResources());
        zk.setJvmOptions(zookeeper.getJvmOptions());
        zk.setUserAffinity(zookeeper.getAffinity());
        return zk;
    }

    public Service generateService() {
        List<ServicePort> ports = new ArrayList<>(2);
        if (isMetricsEnabled()) {
            ports.add(createServicePort(METRICS_PORT_NAME, METRICS_PORT, METRICS_PORT, "TCP"));
        }
        ports.add(createServicePort(CLIENT_PORT_NAME, CLIENT_PORT, CLIENT_PORT, "TCP"));

        return createService("ClusterIP", ports);
    }

    public Service generateHeadlessService() {
        Map<String, String> annotations = Collections.singletonMap("service.alpha.kubernetes.io/tolerate-unready-endpoints", "true");
        return createHeadlessService(headlessName, getServicePortList(), annotations);
    }

    public StatefulSet generateStatefulSet(boolean isOpenShift) {

        return createStatefulSet(
                getContainerPortList(),
                getVolumes(),
                getVolumeClaims(),
                getVolumeMounts(),
                createExecProbe(livenessPath, livenessInitialDelay, livenessTimeout),
                createExecProbe(readinessPath, readinessInitialDelay, readinessTimeout),
                resources(),
                getMergedAffinity(),
                getInitContainers(),
                isOpenShift);
    }

    public ConfigMap generateMetricsAndLogConfigMap(ConfigMap externalLoggingCm) {
        ConfigMap result = createConfigMap(getAncillaryConfigName(), getAncillaryCm(externalLoggingCm));
        if (getLogging() != null) {
            getLogging().setCm(result);
        }
        return result;
    }

    private Map<String, String> getAncillaryCm(ConfigMap cm) {
        Map<String, String> data = new HashMap<>();
        data.put(ANCILLARY_CM_KEY_LOG_CONFIG, parseLogging(getLogging(), cm));
        if (isMetricsEnabled()) {
            JsonObject ffs = new JsonObject();
            for (Map.Entry<String, Object> entry : getMetricsConfig()) {
                ffs.put(entry.getKey(), entry.getValue());
            }
            data.put(ANCILLARY_CM_KEY_METRICS, ffs.toString());

        }
        return data;
    }

    @Override
    protected List<EnvVar> getEnvVars() {
        List<EnvVar> varList = new ArrayList<>();
        varList.add(buildEnvVar(ENV_VAR_ZOOKEEPER_NODE_COUNT, Integer.toString(replicas)));
        varList.add(buildEnvVar(ENV_VAR_ZOOKEEPER_METRICS_ENABLED, String.valueOf(isMetricsEnabled)));
        heapOptions(varList, 0.75, 2L * 1024L * 1024L * 1024L);
        jvmPerformanceOptions(varList);
        varList.add(buildEnvVar(ENV_VAR_ZOOKEEPER_CONFIGURATION, configuration.getConfiguration()));
        if (getLogging() != null && getLogging().getCm() != null) {
            varList.add(buildEnvVar(ENV_VAR_ZOOKEEPER_LOG_CONFIGURATION, getLogging().getCm().toString()));
        }
        return varList;
    }

    private List<ServicePort> getServicePortList() {
        List<ServicePort> portList = new ArrayList<>();
        portList.add(createServicePort(CLIENT_PORT_NAME, CLIENT_PORT, CLIENT_PORT, "TCP"));
        portList.add(createServicePort(CLUSTERING_PORT_NAME, CLUSTERING_PORT, CLUSTERING_PORT, "TCP"));
        portList.add(createServicePort(LEADER_ELECTION_PORT_NAME, LEADER_ELECTION_PORT, LEADER_ELECTION_PORT, "TCP"));

        return portList;
    }

    private List<ContainerPort> getContainerPortList() {
        List<ContainerPort> portList = new ArrayList<>();
        portList.add(createContainerPort(CLIENT_PORT_NAME, CLIENT_PORT, "TCP"));
        portList.add(createContainerPort(CLUSTERING_PORT_NAME, CLUSTERING_PORT, "TCP"));
        portList.add(createContainerPort(LEADER_ELECTION_PORT_NAME, LEADER_ELECTION_PORT, "TCP"));
        if (isMetricsEnabled) {
            portList.add(createContainerPort(metricsPortName, metricsPort, "TCP"));
        }

        return portList;
    }

    private List<Volume> getVolumes() {
        List<Volume> volumeList = new ArrayList<>();
        if (storage instanceof EphemeralStorage) {
            volumeList.add(createEmptyDirVolume(VOLUME_NAME));
        }
        volumeList.add(createConfigMapVolume(logAndMetricsConfigVolumeName, ancillaryConfigName));
        return volumeList;
    }

    private List<PersistentVolumeClaim> getVolumeClaims() {
        List<PersistentVolumeClaim> pvcList = new ArrayList<>();
        if (storage instanceof PersistentClaimStorage) {
            pvcList.add(createPersistentVolumeClaim(VOLUME_NAME));
        }
        return pvcList;
    }

    private List<VolumeMount> getVolumeMounts() {
        List<VolumeMount> volumeMountList = new ArrayList<>();
        volumeMountList.add(createVolumeMount(VOLUME_NAME, mountPath));
        volumeMountList.add(createVolumeMount(logAndMetricsConfigVolumeName, logAndMetricsConfigMountPath));

        return volumeMountList;
    }

    @Override
    protected Properties getDefaultLogConfig() {
        Properties properties = new Properties();
        try {
            properties = getDefaultLoggingProperties("zookeeperDefaultLoggingProperties");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
