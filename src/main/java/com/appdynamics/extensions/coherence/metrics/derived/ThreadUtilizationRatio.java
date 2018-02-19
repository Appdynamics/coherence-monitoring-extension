/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence.metrics.derived;


import com.appdynamics.extensions.coherence.metrics.DefaultMetricProperties;
import com.appdynamics.extensions.coherence.metrics.Metric;
import com.appdynamics.extensions.coherence.metrics.MetricProperties;

import javax.management.ObjectInstance;
import java.math.BigDecimal;
import java.util.List;

class ThreadUtilizationRatio extends DerivedMetric{

    private static final String THREAD_COUNT = "ThreadCount";
    private static final String THREAD_IDLE_COUNT = "ThreadIdleCount";
    private static final String THREAD_UTIL_RATIO = "ThreadUtilRatio";
    private boolean aggregationForThreadCount, aggregationForThreadIdleCount;

    String getMetricName(){
        return THREAD_UTIL_RATIO;
    }

    BigDecimal getMetricValue(List<Metric> metrics) {
        BigDecimal threadCount = BigDecimal.ZERO,threadIdleCount = BigDecimal.ZERO;
        if(metrics == null){
            return MINUS_ONE;
        }
        for(Metric metric : metrics){
            if(metric.getMetricNameOrAlias().equalsIgnoreCase(THREAD_COUNT)){
                threadCount = metric.getMetricValue();
                aggregationForThreadCount = metric.getProperties().isAggregation();
            }
            if(metric.getMetricNameOrAlias().equalsIgnoreCase(THREAD_IDLE_COUNT)){
                threadIdleCount = metric.getMetricValue();
                aggregationForThreadIdleCount = metric.getProperties().isAggregation();
            }
        }
        //utilization = (threadCount-threadIdleCount/threadCount) * 100;
        return threadCount.equals(BigDecimal.ZERO) ? MINUS_ONE : (threadCount.subtract(threadIdleCount)).divide(threadCount,2,BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.TEN.multiply(BigDecimal.TEN));

    }

    MetricProperties getMetricProperties(ObjectInstance instance) {
        MetricProperties props = new DefaultMetricProperties();
        props.setAlias(getMetricName());
        props.setAggregation(aggregationForThreadCount && aggregationForThreadIdleCount);
        return props;
    }
}
