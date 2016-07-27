package com.appdynamics.extensions.coherence.metrics.derived;

import com.appdynamics.extensions.coherence.metrics.Metric;
import com.appdynamics.extensions.coherence.metrics.MetricProperties;
import com.google.common.collect.Lists;

import javax.management.ObjectInstance;
import java.math.BigDecimal;
import java.util.List;

public class DerivedMetricFactory {

    private final List<DerivedMetric> derivedMetrics = Lists.newArrayList();

    public DerivedMetricFactory(DerivedMetricEnum... enums){
        for(DerivedMetricEnum metricEnum : enums){
            if(metricEnum == DerivedMetricEnum.CACHE_HIT_RATIO){
                derivedMetrics.add(new CacheHitRatio());
            }
            else if(metricEnum == DerivedMetricEnum.THREAD_UTILIZATION_RATIO){
                derivedMetrics.add(new ThreadUtilizationRatio());
            }
        }
    }

    public void compute(List<Metric> metrics, ObjectInstance instance,String memberInfo){
        if(metrics == null){
            return;
        }
        for(DerivedMetric derivedMetric : derivedMetrics){
            BigDecimal value = derivedMetric.getMetricValue(metrics);
            if(value != DerivedMetric.MINUS_ONE){
                String key = derivedMetric.getMetricKey(instance,memberInfo);
                MetricProperties props = derivedMetric.getMetricProperties(instance);
                Metric m = new Metric();
                m.setMetricName(derivedMetric.getMetricName());
                m.setMetricKey(key);
                m.setMetricValue(value);
                m.setProperties(props);
                metrics.add(m);
            }

        }
    }
}
