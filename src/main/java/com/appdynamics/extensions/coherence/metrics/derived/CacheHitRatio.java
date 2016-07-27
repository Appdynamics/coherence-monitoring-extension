package com.appdynamics.extensions.coherence.metrics.derived;


import com.appdynamics.extensions.coherence.metrics.DefaultMetricProperties;
import com.appdynamics.extensions.coherence.metrics.Metric;
import com.appdynamics.extensions.coherence.metrics.MetricProperties;

import javax.management.ObjectInstance;
import java.math.BigDecimal;
import java.util.List;

class CacheHitRatio extends DerivedMetric{

    private static final String TOTAL_GETS = "TotalGets";
    private static final String CACHE_HITS = "CacheHits";
    private static final String CACHE_HIT_RATIO = "CacheHitRatio";
    private boolean aggregationForTotalGets, aggregationForCacheHits;

    String getMetricName() {
        return CACHE_HIT_RATIO;
    }

    public BigDecimal getMetricValue(List<Metric> metrics){
        BigDecimal totalGets = BigDecimal.ZERO,cacheHits = BigDecimal.ZERO;
        if(metrics == null){
            return MINUS_ONE;
        }
        for(Metric metric : metrics){
            if(metric.getMetricNameOrAlias().equalsIgnoreCase(TOTAL_GETS)){
                totalGets = metric.getMetricValue();
                aggregationForTotalGets = metric.getProperties().isAggregation();
            }
            if(metric.getMetricNameOrAlias().equalsIgnoreCase(CACHE_HITS)){
                cacheHits = metric.getMetricValue();
                aggregationForCacheHits = metric.getProperties().isAggregation();
            }
        }
        //calculating percentage
        return totalGets.equals(BigDecimal.ZERO) ? MINUS_ONE : cacheHits.divide(totalGets,2,BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.TEN.multiply(BigDecimal.TEN));
    }

    public MetricProperties getMetricProperties(ObjectInstance instance) {
        MetricProperties props = new DefaultMetricProperties();
        props.setAlias(getMetricName());
        props.setAggregation(aggregationForTotalGets && aggregationForCacheHits);
        return props;
    }
}
