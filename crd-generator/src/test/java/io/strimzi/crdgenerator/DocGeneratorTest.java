/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.crdgenerator;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static io.strimzi.crdgenerator.DocGenerator.classInherits;
import static io.strimzi.test.TestUtils.assertResourceMatch;
import static org.junit.Assert.assertTrue;

public class DocGeneratorTest {

    @Test
    public void simpleTest() throws IOException, ClassNotFoundException {
        assertTrue(classInherits(Class.forName("io.strimzi.crdgenerator.KubeLinker"), Linker.class) != null);
        StringWriter w = new StringWriter();
        DocGenerator crdGenerator = new DocGenerator(1, ExampleCrd.class, w, new KubeLinker("{KubeApiReferenceBase}"));
        crdGenerator.generate();
        String s = w.toString();
        assertResourceMatch(DocGeneratorTest.class, "simpleTest.adoc", s);
    }
}
