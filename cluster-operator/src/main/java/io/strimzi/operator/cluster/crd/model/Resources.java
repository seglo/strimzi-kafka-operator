/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.crd.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strimzi.crdgenerator.annotations.Description;
import io.strimzi.crdgenerator.annotations.Pattern;
import io.strimzi.crdgenerator.annotations.Type;

/**
 * Representation for resource containts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Resources {

    public Resources() {
    }

    public Resources(CpuMemory limits, CpuMemory requests) {
        this.limits = limits;
        this.requests = requests;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class CpuMemory {

        private String memory;
        private String milliCpu;

        public CpuMemory() {
        }

        public CpuMemory(long memory, int milliCpu) {
            this.memory = MemoryDeserializer.format(memory);
            this.milliCpu = MilliCpuDeserializer.format(milliCpu);
        }

        /** The memory in bytes */
        @JsonIgnore
        public long getMemoryLong() {
            return memory == null ? 0 : MemoryDeserializer.parse(memory);
        }

        public void setMemoryLong(long memory) {
            this.memory = MemoryDeserializer.format(memory);
        }

        /** The memory in Kubernetes syntax. */
        @Description("Memory")
        @JsonProperty("memory")
        @Pattern("[0-9]+([kKmMgGtTpPeE]i?)?$")
        @Type("string")
        public String getMemoryString() {
            return memory;
        }

        public void setMemoryString(String memory) {
            this.memory = memory;
        }


        /** The CPUs in "millicpus". */
        @JsonIgnore
        public int getMilliCpuInt() {
            return MilliCpuDeserializer.parse(milliCpu);
        }

        public void setMilliCpuInt(int milliCpu) {
            this.milliCpu = MilliCpuDeserializer.format(milliCpu);
        }

        /** The CPUs formatted using Kubernetes syntax. */
        @Description("CPU")
        @JsonProperty("cpu")
        @Type("string")
        @Pattern("[0-9]+m?$")
        public String getMilliCpu() {
            return this.milliCpu;
        }

        public void setMilliCpu(String milliCpu) {
            this.milliCpu = milliCpu;
        }
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
