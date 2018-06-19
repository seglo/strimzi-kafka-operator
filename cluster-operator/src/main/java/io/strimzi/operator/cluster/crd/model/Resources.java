/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.strimzi.crdgenerator.annotations.Description;
import io.sundr.builder.annotations.Buildable;

/**
 * Representation for resource containts.
 */
@Buildable(editableEnabled = false, validationEnabled = true, generateBuilderPackage = true, builderPackage = "io.strimzi.operator.cluster.crd.model"/*, inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")*/)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Resources {

    public Resources() {
    }

    public Resources(CpuMemory limits, CpuMemory requests) {
        this.limits = limits;
        this.requests = requests;
    }

    private CpuMemory limits;

    private CpuMemory requests;

    @Description("Resource limits applied at runtime.")
    public CpuMemory getLimits() {
        return limits;
    }

    public void setLimits(CpuMemory limits) {
        this.limits = limits;
    }

    @Description("Resource requests applied during pod scheduling.")
    public CpuMemory getRequests() {
        return requests;
    }

    public void setRequests(CpuMemory requests) {
        this.requests = requests;
    }

    public static Resources fromJson(String json) {
        return JsonUtils.fromJson(json, Resources.class);
    }

}
