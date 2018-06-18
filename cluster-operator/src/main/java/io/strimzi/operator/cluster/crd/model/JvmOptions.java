/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strimzi.crdgenerator.annotations.Description;
import io.strimzi.crdgenerator.annotations.Pattern;

import java.util.Map;

/**
 * Representation for options to be passed to a JVM.
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JvmOptions {

    private String xmx;
    private String xms;
    private boolean server = false;
    private Map<String, String> xx;

    @JsonProperty("-Xmx")
    @Pattern("[0-9]+[mMgG]?")
    @Description("-Xmx option to to the JVM")
    public String getXmx() {
        return xmx;
    }

    public void setXmx(String xmx) {
        this.xmx = xmx;
    }

    @JsonProperty("-Xms")
    @Pattern("[0-9]+[mMgG]?")
    @Description("-Xms option to to the JVM")
    public String getXms() {
        return xms;
    }

    public void setXms(String xms) {
        this.xms = xms;
    }

    @JsonProperty("-server")
    @Description("-server option to to the JVM")
    public boolean getServer() {
        return server;
    }

    public void setServer(boolean server) {
        this.server = server;
    }

    @JsonProperty("-XX")
    @Description("A map of -XX options to the JVM")
    public Map<String, String> getXx() {
        return xx;
    }

    public void setXx(Map<String, String> xx) {
        this.xx = xx;
    }

    public static JvmOptions fromJson(String json) {
        return JsonUtils.fromJson(json, JvmOptions.class);
    }
}

