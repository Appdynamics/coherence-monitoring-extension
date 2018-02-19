/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence;


import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.yml.YmlReader;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.management.*;
import javax.management.remote.JMXConnector;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CoherenceMonitorTaskTest {

    @Mock
    JMXConnectionAdapter adapter;

    @Mock
    MetricWriteHelper writer;

    @Test
    public void whenConfigWithConverts_thenVerifyConvertedValue() throws IOException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        Map configMap = YmlReader.readFromFileAsMap(new File(this.getClass().getResource("/conf/config_with_converts.yml").getFile()));
        List<Map> servers = (List)configMap.get("instances");
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        for(Map server : servers){
            CoherenceMonitorTask monitorTask = new CoherenceMonitorTask.Builder()
                    .metricPrefix("Custom Metrics|Coherence")
                    .mbeans((List<Map>)configMap.get("mbeans"))
                    .metricWriter(writer)
                    .server(server)
                    .jmxConnectionAdapter(adapter)
                    .build();

            when(adapter.queryMBeans(any(JMXConnector.class),any(ObjectName.class))).thenReturn(JMXDataProviderUtil.getSetWithNodeObjInstances());
            when(adapter.getAttributes(any(JMXConnector.class),any(ObjectName.class),any(String[].class))).thenReturn(JMXDataProviderUtil.getAttributesForConvertTest().asList());
            monitorTask.run();
            verify(writer,times(5)).printMetric(stringCaptor.capture(),anyString(),anyString(),anyString(),anyString());
            Assert.assertTrue(stringCaptor.getAllValues().contains("Custom Metrics|Coherence|local|Metrics Collection Successful"));
          //  verify(writer,atLeastOnce()).printMetric("Custom Metrics|Coherence|local|Metrics Collection Successful", "1", MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        }


    }
}
