/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.strimzi.test.TestUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public abstract class AbstractCrdTest<R extends HasMetadata> {

    private final Class<R> crdClass;

    protected AbstractCrdTest(Class<R> crdClass) {
        this.crdClass = crdClass;
    }

    protected void assertDesiredResource(R k, String resource) throws IOException {
        //assertNotNull("The resource " + resourceName + " does not exist", model);
        String content = TestUtils.readResource(getClass(), resource);
        if (content != null) {
            String ssStr = TestUtils.toYamlString(k);
            assertEquals(content.trim(), ssStr.trim());
        } else {
            fail("The resource " + resource + " does not exist");
        }
    }

    @Test
    public void roundTrip() throws IOException {
        String resourceName = crdClass.getSimpleName() + ".yaml";
        R model = TestUtils.fromYaml(resourceName, crdClass);
        assertNotNull("The classpath resource " + resourceName + " does not exist", model);
        ObjectMeta metadata = model.getMetadata();
        assertNotNull(metadata);
        assertDesiredResource(model, crdClass.getSimpleName() + ".out.yaml");
    }
}
