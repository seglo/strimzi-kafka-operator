/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.Watchable;
import io.strimzi.api.kafka.DoneableKafkaAssembly;
import io.strimzi.api.kafka.KafkaAssemblyList;
import io.strimzi.api.kafka.model.KafkaAssembly;
import io.strimzi.operator.cluster.model.AssemblyType;
import io.strimzi.operator.cluster.model.Labels;
import io.strimzi.operator.cluster.operator.assembly.AbstractAssemblyOperator;
import io.strimzi.operator.cluster.operator.assembly.KafkaAssemblyOperator;
import io.strimzi.operator.cluster.operator.assembly.KafkaConnectAssemblyOperator;
import io.strimzi.operator.cluster.operator.assembly.KafkaConnectS2IAssemblyOperator;
import io.strimzi.operator.cluster.operator.resource.KafkaAssemblyCrdOperator;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An "operator" for managing assemblies of various types <em>in a particular namespace</em>.
 * The Cluster Operator's multiple namespace support is achieved by deploying multiple
 * {@link ClusterOperator}'s in Vertx.
 */
public class ClusterOperator extends AbstractVerticle {

    private static final Logger log = LogManager.getLogger(ClusterOperator.class.getName());

    public static final String STRIMZI_CLUSTER_OPERATOR_DOMAIN = "cluster.operator.strimzi.io";
    public static final String STRIMZI_CLUSTER_OPERATOR_SERVICE_ACCOUNT = "strimzi-cluster-operator";

    private static final int HEALTH_SERVER_PORT = 8080;

    private final KubernetesClient client;
    private final Labels selector;
    private final String namespace;
    private final long reconciliationInterval;

    private final Map<String, Watch> watchByKind = new ConcurrentHashMap();

    private long reconcileTimer;
    private final KafkaAssemblyOperator kafkaAssemblyOperator;
    private final KafkaConnectAssemblyOperator kafkaConnectAssemblyOperator;
    private final KafkaConnectS2IAssemblyOperator kafkaConnectS2IAssemblyOperator;

    public ClusterOperator(String namespace,
                           long reconciliationInterval,
                           KubernetesClient client,
                           KafkaAssemblyOperator kafkaAssemblyOperator,
                           KafkaConnectAssemblyOperator kafkaConnectAssemblyOperator,
                           KafkaConnectS2IAssemblyOperator kafkaConnectS2IAssemblyOperator) {
        log.info("Creating ClusterOperator for namespace {}", namespace);
        this.namespace = namespace;
        this.selector = Labels.forKind("cluster");
        this.reconciliationInterval = reconciliationInterval;
        this.client = client;
        this.kafkaAssemblyOperator = kafkaAssemblyOperator;
        this.kafkaConnectAssemblyOperator = kafkaConnectAssemblyOperator;
        this.kafkaConnectS2IAssemblyOperator = kafkaConnectS2IAssemblyOperator;
    }

    @Override
    public void start(Future<Void> start) {
        log.info("Starting ClusterOperator for namespace {}", namespace);

        // Configure the executor here, but it is used only in other places
        getVertx().createSharedWorkerExecutor("kubernetes-ops-pool", 10, TimeUnit.SECONDS.toNanos(120));

        createConfigMapWatch(cmResult -> {
            if (cmResult.succeeded()) {
                watchByKind.put("ConfigMap", cmResult.result());
                createKafkaWatch(res -> {
                    if (res.succeeded()) {
                        watchByKind.put("Kafka", res.result());

                        log.info("Setting up periodical reconciliation for namespace {}", namespace);
                        this.reconcileTimer = vertx.setPeriodic(this.reconciliationInterval, res2 -> {
                            log.info("Triggering periodic reconciliation for namespace {}...", namespace);
                            reconcileAll("timer");
                        });

                        log.info("ClusterOperator running for namespace {}", namespace);

                        // start the HTTP server for healthchecks
                        this.startHealthServer();

                        start.complete();
                    } else {
                        log.error("ClusterOperator startup failed for namespace {}", namespace, res.cause());
                        start.fail("ClusterOperator startup failed for namespace " + namespace);
                    }
                });
            } else {
                log.error("ClusterOperator startup failed for namespace {}", namespace, cmResult.cause());
                start.fail("ClusterOperator startup failed for namespace " + namespace);
            }
        });

    }

    private void createConfigMapWatch(Handler<AsyncResult<Watch>> timer) {
        createWatch(
            "ConfigMap",
            () -> client.configMaps().inNamespace(namespace).withLabels(selector.toMap()),
            cm -> {
                Labels labels = Labels.fromResource(cm);
                AssemblyType type;
                try {
                    type = labels.type();
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown {} label {} received in ConfigMap {} in namespace {}",
                            Labels.STRIMZI_TYPE_LABEL,
                            cm.getMetadata().getLabels().get(Labels.STRIMZI_TYPE_LABEL),
                            cm.getMetadata().getName(), namespace);
                    type = null;
                }
                return type;
            },
            (cm, type) -> {
                final AbstractAssemblyOperator<?, ?, ?, ?, ?> cluster;
                if (type == null) {
                    log.warn("Missing label {} in ConfigMap {} in namespace {}", Labels.STRIMZI_TYPE_LABEL, cm.getMetadata().getName(), namespace);
                    return null;
                } else {
                    switch (type) {
                        case CONNECT:
                            cluster = kafkaConnectAssemblyOperator;
                            break;
                        case CONNECT_S2I:
                            cluster = kafkaConnectS2IAssemblyOperator;
                            break;
                        default:
                            return null;
                    }
                }
                return cluster;
            },
            () -> {
                recreateConfigMapWatch();
                return null;
            },
            timer
        );
    }

    private void createKafkaWatch(Handler<AsyncResult<Watch>> timer) {
        // TODO watch creation should perhaps be a method in the assembly operator
        createWatch(
            "Kafka",
            () -> client.customResources(KafkaAssemblyCrdOperator.getCustomResourceDefinition(),
                        KafkaAssembly.class, KafkaAssemblyList.class, DoneableKafkaAssembly.class)
                    .inNamespace(namespace),
            cm -> AssemblyType.KAFKA,
            (cm, type) -> kafkaAssemblyOperator,
            () -> {
                recreateKafkaWatch();
                return null;
            },
            timer
        );
    }

    @Override
    public void stop(Future<Void> stop) {
        log.info("Stopping ClusterOperator for namespace {}", namespace);
        vertx.cancelTimer(reconcileTimer);
        for (Watch watch : watchByKind.values()) {
            watch.close();
        }
        client.close();

        stop.complete();
    }

    private <C extends HasMetadata> void createWatch(String kind,
                                                     Supplier<Watchable<Watch, Watcher<C>>> fn,
                                                     Function<C, AssemblyType> fn2,
                                                     BiFunction<C, AssemblyType, AbstractAssemblyOperator<?, ?, ?, ?, ?>> fn3,
                                                     Supplier<Void> recreate,
                                                     Handler<AsyncResult<Watch>> handler) {
        getVertx().executeBlocking(
            future -> {

                Watchable<Watch, Watcher<C>> watchable = fn.get();
                Watch watch = watchable.watch(new Watcher<C>() {
                    @Override
                    public void eventReceived(Action action, C cm) {

                        AssemblyType type = fn2.apply(cm);
                        AbstractAssemblyOperator<?, ?, ?, ?, ?> cluster = fn3.apply(cm, type);

                        String name = cm.getMetadata().getName();
                        switch (action) {
                            case ADDED:
                            case DELETED:
                            case MODIFIED:
                                Reconciliation reconciliation = new Reconciliation("watch", type, namespace, name);
                                log.info("{}: {} {} in namespace {} was {}", reconciliation, kind, name, namespace, action);
                                cluster.reconcileAssembly(reconciliation, result -> {
                                    if (result.succeeded()) {
                                        log.info("{}: Assembly reconciled", reconciliation);
                                    } else {
                                        Throwable cause = result.cause();
                                        if (cause instanceof InvalidConfigMapException) {
                                            log.warn("{}: Failed to reconcile {}", reconciliation, cause.getMessage());
                                        } else {
                                            log.warn("{}: Failed to reconcile {}", reconciliation, cause);
                                        }
                                    }
                                });
                                break;
                            case ERROR:
                                log.error("Failed {} {} in namespace{} ", kind, name, namespace);
                                reconcileAll("watch error");
                                break;
                            default:
                                log.error("Unknown action: {} in namespace {}", name, namespace);
                                reconcileAll("watch unknown");
                        }
                    }

                    @Override
                    public void onClose(KubernetesClientException e) {
                        if (e != null) {
                            log.error("Watcher closed with exception in namespace {}", namespace, e);
                            recreate.get();
                        } else {
                            log.info("Watcher closed in namespace {}", namespace);
                        }
                    }
                });
                future.complete(watch);
            }, res -> {
                if (res.succeeded())    {
                    log.info("{} watcher running for labels {}", kind, selector);
                    handler.handle(Future.succeededFuture((Watch) res.result()));
                } else {
                    log.info("{} watcher failed to start", kind, res.cause());
                    handler.handle(Future.failedFuture(kind + " watcher failed to start"));
                }
            }
        );
    }

    private void recreateConfigMapWatch() {
        createConfigMapWatch(recreateWatchHandler("ConfigMap"));
    }

    private void recreateKafkaWatch() {
        createKafkaWatch(recreateWatchHandler("Kafka"));
    }

    private Handler<AsyncResult<Watch>> recreateWatchHandler(String kind) {
        return res -> {
            if (res.succeeded())    {
                log.info("{} watch recreated in namespace {}", kind, namespace);
                watchByKind.put(kind, res.result());
            } else {
                log.error("Failed to recreate {} watch in namespace {}", kind, namespace);
                // We failed to recreate the Watch. We cannot continue without it. Lets close Vert.x and exit.
                vertx.close();
            }
        };
    }

    /**
      Periodical reconciliation (in case we lost some event)
     */
    private void reconcileAll(String trigger) {
        kafkaAssemblyOperator.reconcileAll(trigger, namespace, selector);
        kafkaConnectAssemblyOperator.reconcileAll(trigger, namespace, selector);

        if (kafkaConnectS2IAssemblyOperator != null) {
            kafkaConnectS2IAssemblyOperator.reconcileAll(trigger, namespace, selector);
        }
    }

    /**
     * Start an HTTP health server
     */
    private void startHealthServer() {

        this.vertx.createHttpServer()
                .requestHandler(request -> {

                    if (request.path().equals("/healthy")) {
                        request.response().setStatusCode(200).end();
                    } else if (request.path().equals("/ready")) {
                        request.response().setStatusCode(200).end();
                    }
                })
                .listen(HEALTH_SERVER_PORT);
    }

}
