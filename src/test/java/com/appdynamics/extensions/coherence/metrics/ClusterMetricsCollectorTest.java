/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence.metrics;


import com.appdynamics.extensions.util.Aggregator;
import com.appdynamics.extensions.util.AggregatorFactory;
import com.appdynamics.extensions.util.AggregatorKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Set;

public class ClusterMetricsCollectorTest {

    ClusterMetricsProcessor cmc = new ClusterMetricsProcessor();

    @Test
    public void whenNodeMetricsSetForAggregation_thenCalculateAvg(){
        AggregatorFactory factory = new AggregatorFactory();
        cmc.collect(factory,MetricsGenerator.generateNodeMetricsForClusterAggregationAsIndividual());
        Collection<Aggregator<AggregatorKey>> aggregators = factory.getAggregators();
        for (Aggregator<AggregatorKey> aggregator : aggregators) {
            Set<AggregatorKey> keys = aggregator.keys();
            for (AggregatorKey key : keys) {
                Assert.assertTrue("ClusterA|CacheHits".equals(key.getMetricPath()));
                Assert.assertTrue(aggregator.getAggregatedValue(key).intValue() == 33);
            }
        }
    }

    @Test
    public void whenNodeMetricsSetForAggregation_thenCalculateSum(){
        AggregatorFactory factory = new AggregatorFactory();
        cmc.collect(factory,MetricsGenerator.generateNodeMetricsForClusterAggregationAsCollective());
        Collection<Aggregator<AggregatorKey>> aggregators = factory.getAggregators();
        for (Aggregator<AggregatorKey> aggregator : aggregators) {
            Set<AggregatorKey> keys = aggregator.keys();
            for (AggregatorKey key : keys) {
                Assert.assertTrue("ClusterA|CacheHits".equals(key.getMetricPath()));
                Assert.assertTrue(aggregator.getAggregatedValue(key).intValue() == 100);
            }
        }
    }

}
