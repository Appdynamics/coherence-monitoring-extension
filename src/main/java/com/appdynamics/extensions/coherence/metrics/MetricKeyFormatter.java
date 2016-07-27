package com.appdynamics.extensions.coherence.metrics;


import com.appdynamics.extensions.coherence.CoherenceMBeanKeyPropertyEnum;
import com.google.common.base.Strings;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

public class MetricKeyFormatter {


    private ObjectName getObjectName(ObjectInstance instance) {
        return instance.getObjectName();
    }

    public String getClusterKey(ObjectInstance instance) {
        if(instance == null){
            return "";
        }
        // Standard jmx keys. {type, scope, name, keyspace, path etc.}
        String type = getKeyProperty(instance, CoherenceMBeanKeyPropertyEnum.TYPE.toString());
        String domain = getKeyProperty(instance, CoherenceMBeanKeyPropertyEnum.DOMAIN.toString());
        String subType = getKeyProperty(instance, CoherenceMBeanKeyPropertyEnum.SUBTYPE.toString());
        String name = getKeyProperty(instance, CoherenceMBeanKeyPropertyEnum.NAME.toString());
        String service = getKeyProperty(instance, CoherenceMBeanKeyPropertyEnum.SERVICE.toString());

        String cache = getKeyProperty(instance, CoherenceMBeanKeyPropertyEnum.CACHE.toString());
        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(Strings.isNullOrEmpty(type) ? "" : type + "|");
        metricsKey.append(Strings.isNullOrEmpty(domain) ? "" : domain + "|");
        metricsKey.append(Strings.isNullOrEmpty(subType) ? "" : subType + "|");
        metricsKey.append(Strings.isNullOrEmpty(service) ? "" : service + "|");
        metricsKey.append(Strings.isNullOrEmpty(name) ? "" : name + "|");
        metricsKey.append(Strings.isNullOrEmpty(cache) ? "" : cache + "|");
        return metricsKey.toString();
    }

    String getKeyProperty(ObjectInstance instance, String property) {
        if(instance == null){
            return "";
        }
        return getObjectName(instance).getKeyProperty(property);
    }

    public String getNodeKey(ObjectInstance instance, String metricName, String clusterKey, String memberInfo){
        StringBuilder metricKey = new StringBuilder(clusterKey);
        String tier = getKeyProperty(instance, CoherenceMBeanKeyPropertyEnum.TIER.toString());
        String responsibility = getKeyProperty(instance, CoherenceMBeanKeyPropertyEnum.RESPONSIBILITY.toString());
        metricKey.append(Strings.isNullOrEmpty(memberInfo) ? "" : ("Nodes" + "|" + memberInfo + "|"));
        metricKey.append(Strings.isNullOrEmpty(tier) ? "" : tier + "|");
        metricKey.append(Strings.isNullOrEmpty(responsibility) ? "" : responsibility + "|");
        metricKey.append(metricName);
        return metricKey.toString();
    }



}
