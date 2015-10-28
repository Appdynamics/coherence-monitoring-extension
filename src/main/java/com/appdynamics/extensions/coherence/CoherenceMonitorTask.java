package com.appdynamics.extensions.coherence;


import com.appdynamics.extensions.jmx.JMXConnectionUtil;
import com.appdynamics.extensions.jmx.MBeanKeyPropertyEnum;
import com.appdynamics.extensions.util.metrics.Metric;
import com.appdynamics.extensions.util.metrics.MetricFactory;
import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.log4j.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import static com.appdynamics.extensions.util.metrics.MetricConstants.METRICS_SEPARATOR;


public class CoherenceMonitorTask implements Callable<Void> {

    public static final String COHERENCE_TYPE_CLUSTER = "Coherence:type=Cluster";
    public static final String MEMBERS_ATTRIB = "Members";
    private String metricPrefix;
    private String displayName;
    private AManagedMonitor monitor;
    private MetricOverride[] metricOverrides;
    private JMXConnectionUtil jmxConnector;
    public static final Logger logger = Logger.getLogger(CoherenceMonitorTask.class);

    public CoherenceMonitorTask(String metricPrefix,String displayName,MetricOverride[] metricOverrides,JMXConnectionUtil jmxConnector,AManagedMonitor monitor) {
        this.metricPrefix = metricPrefix;
        this.displayName = displayName;
        this.metricOverrides = metricOverrides;
        this.jmxConnector = jmxConnector;
        this.monitor = monitor;
    }


    public Void call() throws Exception {
        Map<String, Object> allMetrics = extractJMXMetrics();
        logger.debug("Total number of metrics extracted from server " + displayName + " " + allMetrics.size());
        // to get overridden properties for a metric.
        MetricFactory<Object> metricFactory = new MetricFactory<Object>(metricOverrides);
        List<Metric> decoratedMetrics = metricFactory.process(allMetrics);
        reportMetrics(decoratedMetrics);
        return null;
    }

    /**
     * Connects to a remote/local JMX server, applies exclusion filters and collects the metrics
     *
     * @return Void. In case of exception, the CoherenceMonitorConstants.METRICS_COLLECTION_SUCCESSFUL is set with CoherenceMonitorConstants.ERROR_VALUE.
     * @throws Exception
     */
    private Map<String, Object> extractJMXMetrics() throws IOException {
        Map<String, Object> allMetrics = new HashMap<String, Object>();
        long startTime = System.currentTimeMillis();
        logger.debug("Starting coherence monitor thread at " + startTime + " for server " + displayName);
        try{
            JMXConnector connector = jmxConnector.connect();
            if(connector != null){
                Object members = null;
                Map<String,CoherenceMember> membersMap = null;
                try {
                    members = jmxConnector.getMBeanAttribute(new ObjectName(COHERENCE_TYPE_CLUSTER), MEMBERS_ATTRIB);
                    membersMap = createMemberMap(members);
                } catch (MalformedObjectNameException e) {
                   logger.error("Cannot extract cluster members",e);
                }
                Set<ObjectInstance> allMbeans = jmxConnector.getAllMBeans();
                if(allMbeans != null) {
                    mapMetrics(allMbeans, allMetrics,membersMap);
                    allMetrics.put(CoherenceMonitorConstants.METRICS_COLLECTION_SUCCESSFUL, CoherenceMonitorConstants.SUCCESS_VALUE);
                }
            }
        }
        catch(Exception e){
            logger.error("Error JMX-ing into the server :: " + displayName, e);
            long diffTime = System.currentTimeMillis() - startTime;
            logger.debug("Error in coherence thread at " + diffTime);
            allMetrics.put(CoherenceMonitorConstants.METRICS_COLLECTION_SUCCESSFUL, CoherenceMonitorConstants.ERROR_VALUE);
        }
        finally{
            jmxConnector.close();
        }
        return allMetrics;
    }

    private void mapMetrics(Set<ObjectInstance> allMbeans, Map<String, Object> allMetrics,Map<String,CoherenceMember> membersMap) {
        for(ObjectInstance mbean : allMbeans){
            ObjectName objectName = mbean.getObjectName();
            MBeanAttributeInfo[] attributes = jmxConnector.fetchAllAttributesForMbean(objectName);
            if (attributes != null) {
                for (MBeanAttributeInfo attr : attributes) {
                    try {
                        // See we do not violate the security rules, i.e. only if the attribute is readable.
                        if (attr.isReadable()) {
                            Object attribute = jmxConnector.getMBeanAttribute(objectName, attr.getName());
                            //AppDynamics only considers number values
                            if (attribute != null && attribute instanceof Number) {
                                String metricKey = getMetricsKey(objectName, attr,membersMap);
                                allMetrics.put(metricKey, attribute);
                            }
                        }
                    }
                    catch(Exception e){
                        logger.warn("Error fetching attribute " + attr.getName(), e);
                    }
                }
            }
        }
    }

    private void reportMetrics(List<Metric> decoratedMetrics) {
        StringBuffer pathPrefixBuffer = new StringBuffer();
        pathPrefixBuffer.append(metricPrefix);
        if(!metricPrefix.endsWith("|")){
            pathPrefixBuffer.append(METRICS_SEPARATOR);
        }
        pathPrefixBuffer.append(displayName).append(METRICS_SEPARATOR);
        String pathPrefix = pathPrefixBuffer.toString();
        for(Metric aMetric:decoratedMetrics){
            printMetric(pathPrefix + aMetric.getMetricPath(),aMetric.getMetricValue().toString(),aMetric.getAggregator(),aMetric.getTimeRollup(),aMetric.getClusterRollup());
        }
    }

    private void printMetric(String metricPath,String metricValue,String aggType,String timeRollupType,String clusterRollupType) {
        MetricWriter metricWriter = monitor.getMetricWriter(metricPath,
                aggType,
                timeRollupType,
                clusterRollupType
        );
        System.out.println("Sending [" + aggType + METRICS_SEPARATOR + timeRollupType + METRICS_SEPARATOR + clusterRollupType
                    + "] metric = " + metricPath + " = " + metricValue);
        if (logger.isDebugEnabled()) {
            logger.debug("Sending [" + aggType + METRICS_SEPARATOR + timeRollupType + METRICS_SEPARATOR + clusterRollupType
                    + "] metric = " + metricPath + " = " + metricValue);
        }
        metricWriter.printMetric(metricValue);
    }

    private String getMetricsKey(ObjectName objectName,MBeanAttributeInfo attr,Map<String,CoherenceMember> membersMap) {
        // Standard jmx keys. {type, scope, name, keyspace, path etc.}
        String type = objectName.getKeyProperty(MBeanKeyPropertyEnum.TYPE.toString());
        String domain = objectName.getKeyProperty(CoherenceMBeanKeyPropertyEnum.DOMAIN.toString());
        String subType = objectName.getKeyProperty(CoherenceMBeanKeyPropertyEnum.SUBTYPE.toString());
        String name = objectName.getKeyProperty(MBeanKeyPropertyEnum.NAME.toString());
        String nodeId = objectName.getKeyProperty(CoherenceMBeanKeyPropertyEnum.NODEID.toString());
        String service = objectName.getKeyProperty(CoherenceMBeanKeyPropertyEnum.SERVICE.toString());
        String responsibility = objectName.getKeyProperty(CoherenceMBeanKeyPropertyEnum.RESPONSIBILITY.toString());
        String cache = objectName.getKeyProperty(CoherenceMBeanKeyPropertyEnum.CACHE.toString());
        String tier = objectName.getKeyProperty(CoherenceMBeanKeyPropertyEnum.TIER.toString());
        StringBuilder metricsKey = new StringBuilder();
        metricsKey.append(Strings.isNullOrEmpty(type) ? "" : type + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(domain) ? "" : domain + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(subType) ? "" : subType + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(service) ? "" : service + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(name) ? "" : name + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(cache) ? "" : cache + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(nodeId) ? "" : getMemberInfo(membersMap.get(nodeId)));
        metricsKey.append(Strings.isNullOrEmpty(tier) ? "" : tier + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(responsibility) ? "" : responsibility + METRICS_SEPARATOR);
        metricsKey.append(attr.getName());
        return metricsKey.toString();
    }

    private Map<String,CoherenceMember> createMemberMap(Object members) {
        Map<String,CoherenceMember> memberMap = new HashMap<String, CoherenceMember>();
        if(members != null ){
            String[] membersArr = (String[]) members;
            for(String member : membersArr){
                CoherenceMember node = new CoherenceMember(member);
                if(node.getId() != null){
                    memberMap.put(node.getId(),node);
                }
            }
        }
        return memberMap;
    }

    private String getMemberInfo(CoherenceMember node) {
        StringBuilder memberInfo = new StringBuilder();
        memberInfo.append(Strings.isNullOrEmpty(node.getMachineName()) ? "" : node.getMachineName() + METRICS_SEPARATOR);
        memberInfo.append(Strings.isNullOrEmpty(node.getMemberName()) ? "" : node.getMemberName() + METRICS_SEPARATOR);
        return memberInfo.toString();
    }
}
