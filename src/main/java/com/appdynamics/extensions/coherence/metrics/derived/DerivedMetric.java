/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence.metrics.derived;


import com.appdynamics.extensions.coherence.metrics.Metric;
import com.appdynamics.extensions.coherence.metrics.MetricKeyFormatter;
import com.appdynamics.extensions.coherence.metrics.MetricProperties;

import javax.management.ObjectInstance;
import java.math.BigDecimal;
import java.util.List;

abstract class DerivedMetric {
    public static final BigDecimal MINUS_ONE = BigDecimal.valueOf(-1);
    final MetricKeyFormatter keyFormatter = new MetricKeyFormatter();

    abstract  String getMetricName();

    abstract BigDecimal getMetricValue(List<Metric> metrics);

    abstract MetricProperties getMetricProperties(ObjectInstance instance);

    String getMetricKey(ObjectInstance instance, String memberInfo) {
        String metricKeyPrefix = keyFormatter.getClusterKey(instance);
        return keyFormatter.getNodeKey(instance,getMetricName(),metricKeyPrefix,memberInfo);
    }
}
