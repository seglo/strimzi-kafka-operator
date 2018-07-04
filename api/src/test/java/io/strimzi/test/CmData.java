/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation is used in ({@link ConnectCluster}
 * to configure a cluster with custom parameters and values.
 * <p>
 * An example would be:
 * <pre>
 * &#064;Test
 * &#064;KafkaCluster(config = {
 * &#064;CmData(key = "foo", value = "bar")
 * })
 * public void test() {
 * }
 * </pre>
 * @deprecated This will be removed once Kafka Connect clusters are CRD enabled
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface CmData {

    String key();
    String value();
}
