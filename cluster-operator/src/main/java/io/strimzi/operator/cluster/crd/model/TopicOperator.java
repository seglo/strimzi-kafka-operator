/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Affinity;
import io.strimzi.crdgenerator.annotations.Description;
import io.strimzi.crdgenerator.annotations.KubeLink;
import io.strimzi.crdgenerator.annotations.OmitFromSchema;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation of a Strimzi-managed topic operator deployment..
 */
@Buildable(editableEnabled = false, validationEnabled = true, generateBuilderPackage = true, builderPackage = "io.strimzi.operator.cluster.crd.model"/*, inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")*/)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicOperator {
    private String watchedNamespace;
    private String image = io.strimzi.operator.cluster.model.TopicOperator.DEFAULT_IMAGE;
    private String reconciliationIntervalSeconds = io.strimzi.operator.cluster.model.TopicOperator.DEFAULT_FULL_RECONCILIATION_INTERVAL_MS;
    private String zookeeperSessionTimeoutSeconds = io.strimzi.operator.cluster.model.TopicOperator.DEFAULT_ZOOKEEPER_SESSION_TIMEOUT_MS;
    private int topicMetadataMaxAttempts = io.strimzi.operator.cluster.model.TopicOperator.DEFAULT_TOPIC_METADATA_MAX_ATTEMPTS;
    private Resources resources;
    private Affinity affinity;

    @Description("The namespace the Topic Operator should watch.")
    public String getWatchedNamespace() {
        return watchedNamespace;
    }

    public void setWatchedNamespace(String watchedNamespace) {
        this.watchedNamespace = watchedNamespace;
    }

    @Description("The image to use for the topic operator")
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Deprecated
    public String getReconciliationInterval() {
        return reconciliationIntervalSeconds;
    }

    @Deprecated
    @OmitFromSchema
    public void setReconciliationInterval(String reconciliationIntervalSeconds) {
        this.reconciliationIntervalSeconds = reconciliationIntervalSeconds;
    }

    @Description("Interval between periodic reconciliations.")
    public String getReconciliationIntervalSeconds() {
        return reconciliationIntervalSeconds;
    }

    public void setReconciliationIntervalSeconds(String reconciliationIntervalSeconds) {
        this.reconciliationIntervalSeconds = reconciliationIntervalSeconds;
    }

    @Deprecated
    @OmitFromSchema
    public String getZookeeperSessionTimeout() {
        return zookeeperSessionTimeoutSeconds;
    }

    @Deprecated
    public void setZookeeperSessionTimeout(String zookeeperSessionTimeoutSeconds) {
        this.zookeeperSessionTimeoutSeconds = zookeeperSessionTimeoutSeconds;
    }

    @Description("Timeout for the Zookeeper session")
    public String getZookeeperSessionTimeoutSeconds() {
        return zookeeperSessionTimeoutSeconds;
    }

    public void setZookeeperSessionTimeoutSeconds(String zookeeperSessionTimeoutSeconds) {
        this.zookeeperSessionTimeoutSeconds = zookeeperSessionTimeoutSeconds;
    }

    @Description("The number of attempts at getting topic metadata")
    public int getTopicMetadataMaxAttempts() {
        return topicMetadataMaxAttempts;
    }

    public void setTopicMetadataMaxAttempts(int topicMetadataMaxAttempts) {
        this.topicMetadataMaxAttempts = topicMetadataMaxAttempts;
    }

    public Resources getResources() {
        return resources;
    }

    @Description("Resource constraints (limits and requests).")
    public void setResources(Resources resources) {
        this.resources = resources;
    }

    @Description("Pod affinity rules.")
    @KubeLink(group = "core", version = "v1", kind = "affinity")
    public Affinity getAffinity() {
        return affinity;
    }

    public void setAffinity(Affinity affinity) {
        this.affinity = affinity;
    }

    public static TopicOperator fromJson(String json) {
        return JsonUtils.fromJson(json, TopicOperator.class);
    }
}
