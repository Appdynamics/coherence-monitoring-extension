package com.appdynamics.extensions.coherence.config;


import com.google.common.collect.Maps;

import java.util.Map;

public class MBean {

    String objectName;
    Map<String,?> metrics;
    boolean clusterLevelReporting;

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Map<String, ?> getMetrics() {
        if(metrics == null){
            metrics = Maps.newHashMap();
        }
        return metrics;
    }

    public void setMetrics(Map<String, ?> metrics) {
        this.metrics = metrics;
    }

    public boolean isClusterLevelReporting() {
        return clusterLevelReporting;
    }

    public void setClusterLevelReporting(boolean clusterLevelReporting) {
        this.clusterLevelReporting = clusterLevelReporting;
    }
}
