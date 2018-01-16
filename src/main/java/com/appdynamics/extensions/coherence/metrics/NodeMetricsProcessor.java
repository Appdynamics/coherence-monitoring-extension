package com.appdynamics.extensions.coherence.metrics;

import com.appdynamics.extensions.coherence.CoherenceMBeanKeyPropertyEnum;
import com.appdynamics.extensions.coherence.CoherenceMember;
import com.appdynamics.extensions.coherence.JMXConnectionAdapter;
import com.appdynamics.extensions.coherence.filters.ExcludeFilter;
import com.appdynamics.extensions.coherence.filters.IncludeFilter;
import com.appdynamics.extensions.coherence.metrics.derived.DerivedMetricFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.appdynamics.extensions.coherence.ConfigConstants.*;
import static com.appdynamics.extensions.coherence.Util.convertToString;


public class NodeMetricsProcessor {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(NodeMetricsProcessor.class);

    private final JMXConnectionAdapter jmxAdapter;
    private final JMXConnector jmxConnection;
    private DerivedMetricFactory derivedMetricFactory;


    private final MetricKeyFormatter keyFormatter = new MetricKeyFormatter();
    private final MetricValueTransformer valueConverter = new MetricValueTransformer();

    public NodeMetricsProcessor(JMXConnectionAdapter jmxAdapter, JMXConnector jmxConnection,DerivedMetricFactory derivedMetricFactory) {
        this.jmxAdapter = jmxAdapter;
        this.jmxConnection = jmxConnection;
        this.derivedMetricFactory = derivedMetricFactory;
    }

    public List<Metric> getNodeMetrics(Map aConfigMBean,Map<String, CoherenceMember> membersMap, Map<String, MetricProperties> metricPropsMap) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException, MalformedObjectNameException {
        List<Metric> nodeMetrics = Lists.newArrayList();
        String configObjectName = convertToString(aConfigMBean.get(OBJECT_NAME),"");
        //Each mbean mentioned in the config.yaml can fetch multiple object instances. Metrics need to be extracted
        //from each object instance separately.
        Set<ObjectInstance> objectInstances = jmxAdapter.queryMBeans(jmxConnection,ObjectName.getInstance(configObjectName));
        for(ObjectInstance instance : objectInstances){
            List<String> metricNamesDictionary = jmxAdapter.getReadableAttributeNames(jmxConnection,instance);
            List<String> metricNamesToBeExtracted = applyFilters(aConfigMBean,metricNamesDictionary);
            List<Attribute> attributes = jmxAdapter.getAttributes(jmxConnection,instance.getObjectName(), metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
            String memberInfo = getNodeMemberInfo(instance,membersMap);
            //get node metrics
            collect(nodeMetrics,attributes,instance,metricPropsMap,memberInfo);
            if(derivedMetricFactory != null){
                derivedMetricFactory.compute(nodeMetrics,instance,memberInfo);
            }
        }
        return nodeMetrics;
    }

    private List<String> applyFilters(Map aConfigMBean, List<String> metricNamesDictionary) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        Set<String> filteredSet = Sets.newHashSet();
        Map configMetrics = (Map)aConfigMBean.get(METRICS);
        List includeDictionary = (List)configMetrics.get(INCLUDE);
        List excludeDictionary = (List)configMetrics.get(EXCLUDE);
        new ExcludeFilter(excludeDictionary).apply(filteredSet,metricNamesDictionary);
        new IncludeFilter(includeDictionary).apply(filteredSet,metricNamesDictionary);
        return Lists.newArrayList(filteredSet);
    }

    private String getNodeMemberInfo(ObjectInstance instance, java.util.Map<String,CoherenceMember> membersMap) {
        String nodeId = getNodeId(instance);
        if(nodeId == null){
            return "";
        }
        CoherenceMember nodeMember = membersMap.get(nodeId);
        if(nodeMember == null){
            return nodeId;
        }
        String memberInfo = nodeMember.getMemberInfo();
        if (Strings.isNullOrEmpty(memberInfo)) {
            return nodeId;
        }
        return memberInfo;
    }
// *****************
//   Old collect
// *****************

//    private void collect(List<Metric> nodeMetrics,List<Attribute> attributes,ObjectInstance instance, Map<String, MetricProperties> metricPropsPerMetricName, String memberInfo) {
//        for(Attribute attr : attributes) {
//            try {
//                String attrName = attr.getName();
//                MetricProperties props = metricPropsPerMetricName.get(attrName);
//                if(props == null){
//                    logger.error("Could not find metric props for {}",attrName);
//                    continue;
//                }
//                //get metric value by applying conversions if necessary
//                BigDecimal metricValue = valueConverter.transform(attrName, attr.getValue(),props);
//                if(metricValue != null){
//                    String clusterKey = keyFormatter.getClusterKey(instance);
//                    Metric nodeMetric = new Metric();
//                    nodeMetric.setMetricName(attrName);
//                    nodeMetric.setClusterKey(clusterKey);
//                    String metricName = nodeMetric.getMetricNameOrAlias();
//                    String nodeMetricKey = keyFormatter.getNodeKey(instance,metricName,clusterKey,memberInfo);
//                    nodeMetric.setProperties(props);
//                    nodeMetric.setMetricKey(nodeMetricKey);
//                    nodeMetric.setMetricValue(metricValue);
//                    nodeMetrics.add(nodeMetric);
//                }
//
//            }
//            catch(Exception e){
//                logger.error("Error collecting value for {} {}", instance.getObjectName(),attr.getName(),e);
//            }
//        }
//    }

    public String getNodeId(ObjectInstance instance){
        return keyFormatter.getKeyProperty(instance, CoherenceMBeanKeyPropertyEnum.NODEID.toString());
    }

    private void collect(List<Metric> nodeMetrics,List<Attribute> attributes,ObjectInstance instance, Map<String, MetricProperties> metricPropsPerMetricName, String memberInfo) {
        for(Attribute attr : attributes) {
            try {
                String attrName = attr.getName();
                MetricProperties props = metricPropsPerMetricName.get(attrName);
                if(props == null){
                    logger.error("Could not find metric props for {}",attrName);
                    continue;
                }
                //get metric value by applying conversions if necessary
                BigDecimal metricValue = valueConverter.transform(attrName, attr.getValue(),props);
                if(metricValue != null){
                    String clusterKey = keyFormatter.getClusterKey(instance);
                    Metric nodeMetric = new Metric();
                    nodeMetric.setMetricName(attrName);
                    nodeMetric.setClusterKey(clusterKey);
                    String metricName = nodeMetric.getMetricNameOrAlias();
                    String nodeMetricKey = keyFormatter.getNodeKey(instance,metricName,clusterKey,memberInfo);
                    nodeMetric.setProperties(props);
                    nodeMetric.setMetricKey(nodeMetricKey);
                    nodeMetric.setMetricValue(metricValue);
                    nodeMetrics.add(nodeMetric);
                }

            }
            catch(Exception e){
                logger.error("Error collecting value for {} {}", instance.getObjectName(),attr.getName(),e);
            }
        }
    }

    private boolean isCurrentObjectComposite(Attribute attribute) {
        return attribute.getValue().getClass().equals(CompositeDataSupport.class);
    }


}
