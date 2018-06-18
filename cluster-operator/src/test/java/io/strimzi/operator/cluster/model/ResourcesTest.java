/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;

import io.strimzi.operator.cluster.crd.model.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResourcesTest {

    @Test
    public void testDeserializeSuffixes() {
        Resources opts = Resources.fromJson("{\"limits\": {\"memory\": \"10Gi\", \"cpu\": \"1\"}, \"requests\": {\"memory\": \"5G\", \"cpu\": 1}}");
        assertEquals(10737418240L, opts.getLimits().getMemoryLong());
        assertEquals(1000, opts.getLimits().getMilliCpuInt());
        assertEquals("1", opts.getLimits().getMilliCpu());
        assertEquals(5000000000L, opts.getRequests().getMemoryLong());
        assertEquals(1000, opts.getLimits().getMilliCpuInt());
        assertEquals("1", opts.getLimits().getMilliCpu());
        AbstractModel abstractModel = new AbstractModel("", "", Labels.forCluster("")) {
        };
        abstractModel.setResources(opts);
        assertEquals("1", abstractModel.resources().getLimits().get("cpu").getAmount());
    }

    @Test
    public void testDeserializeInts() {
        Resources opts = Resources.fromJson("{\"limits\": {\"memory\": 10737418240}, \"requests\": {\"memory\": 5000000000}}");
        assertEquals(10737418240L, opts.getLimits().getMemoryLong());
        assertEquals(5000000000L, opts.getRequests().getMemoryLong());
    }

    @Test
    public void testDeserializeDefaults() {
        Resources opts = Resources.fromJson("{\"limits\": {\"memory\": 10737418240}, \"requests\": {} }");
        assertEquals(10737418240L, opts.getLimits().getMemoryLong());
        assertEquals(0, opts.getRequests().getMemoryLong());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeserializeInvalidMemory() {
        Resources.fromJson("{\"limits\": {\"memory\": \"foo\"}, \"requests\": {\"memory\": bar}}");
    }
}
