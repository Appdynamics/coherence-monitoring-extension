/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence.metrics;

import org.junit.Assert;
import org.junit.Test;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

public class MetricKeyFormatterTest {

    MetricKeyFormatter formatter = new MetricKeyFormatter();

    @Test
    public void whenObjectInstanceIsNull_thenReturnEmpty(){
        Assert.assertTrue(formatter.getClusterKey(null).isEmpty());
    }

    @Test
    public void whenValidObjectInstance_thenReturnValidPrefix() throws MalformedObjectNameException {
        ObjectInstance instance = new ObjectInstance(new ObjectName("Coherence:type=Cache,service=DistributedCache,name=Java,nodeId=1,tier=back"),this.getClass().getName());
        String prefix = formatter.getClusterKey(instance);
        Assert.assertTrue(prefix.equals("Cache|DistributedCache|Java|"));
    }

    @Test
    public void whenAllArgsValid_thenReturnNodeKey() throws MalformedObjectNameException {
        ObjectInstance instance = new ObjectInstance(new ObjectName("Coherence:type=Cache,service=DistributedCache,name=Java,nodeId=1,tier=back"),this.getClass().getName());
        String prefix = formatter.getClusterKey(instance);
        String nodeKey = formatter.getNodeKey(instance,"CacheHits",prefix,"MachineA");
        Assert.assertTrue(nodeKey.equals("Cache|DistributedCache|Java|Nodes|MachineA|back|CacheHits"));
    }

    @Test
    public void whenSomeArgsValid_thenShouldNotThrowExceptions() throws MalformedObjectNameException {
        ObjectInstance instance = new ObjectInstance(new ObjectName("Coherence:type=Cache,service=DistributedCache,name=Java,nodeId=1,tier=back"),this.getClass().getName());
        String prefix = formatter.getClusterKey(instance);
        String nodeKey = formatter.getNodeKey(instance,"CacheHits",prefix,null);
        Assert.assertTrue(!nodeKey.isEmpty());
    }
}
