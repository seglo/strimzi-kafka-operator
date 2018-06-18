/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.crdgenerator;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import static io.strimzi.test.TestUtils.assertResourceMatch;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class CrdGeneratorTest {

    private CrdGenerator crdGenerator = new CrdGenerator(new YAMLMapper());

    @Test
    public void simpleTest() throws IOException {
        StringWriter w = new StringWriter();
        crdGenerator.generate(ExampleCrd.class, w);
        String s = w.toString();
        assertResourceMatch(CrdGeneratorTest.class, "simpleTest.yaml", s);
    }
}
