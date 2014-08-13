package com.appdynamics.extensions.coherence;


import com.appdynamics.extensions.coherence.config.MBeanData;
import com.appdynamics.extensions.coherence.config.Server;
import com.appdynamics.extensions.jmx.JMXConnectionConfig;
import com.appdynamics.extensions.jmx.JMXConnectionUtil;
import com.appdynamics.extensions.jmx.MBeanKeyPropertyEnum;
import com.appdynamics.extensions.util.MetricUtils;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class CoherenceMonitorTask implements Callable<CoherenceMetrics> {

    public static final String METRICS_SEPARATOR = "|";
    public static final String COHERENCE_TYPE_CLUSTER = "Coherence:type=Cluster";
    public static final String MEMBERS_ATTRIB = "Members";
    private Server server;
    private Map<String,MBeanData> mbeanLookup;
    private JMXConnectionUtil jmxConnector;
    public static final Logger logger = Logger.getLogger(CoherenceMonitorTask.class);

    public CoherenceMonitorTask(Server server, MBeanData[] mbeansData) {
        this.server = server;
        createMBeansLookup(mbeansData);
    }



    private void createMBeansLookup(MBeanData[] mbeansData) {
        mbeanLookup = new HashMap<String, MBeanData>();
        if(mbeansData != null){
            for(MBeanData mBeanData : mbeansData){
                mbeanLookup.put(mBeanData.getDomainName(),mBeanData);
            }
        }
    }


    /**
     * Connects to a remote/local JMX server, applies exclusion filters and collects the metrics
     *
     * @return CassandraMetrics. In case of exception, the CassandraMonitorConstants.METRICS_COLLECTION_SUCCESSFUL is set with CassandraMonitorConstants.ERROR_VALUE.
     * @throws Exception
     */
    public CoherenceMetrics call() throws Exception {
        CoherenceMetrics coherenceMetrics = new CoherenceMetrics();
        coherenceMetrics.setDisplayName(server.getDisplayName());
        try{
            jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getHost(),server.getPort(),server.getUsername(),server.getPassword()));
            JMXConnector connector = jmxConnector.connect();
            if(connector != null){
                Object members =  jmxConnector.getMBeanAttribute(new ObjectName(COHERENCE_TYPE_CLUSTER), MEMBERS_ATTRIB);
                Map<String,CoherenceMember> membersMap = createMemberMap(members);
                Set<ObjectInstance> allMbeans = jmxConnector.getAllMBeans();
                if(allMbeans != null) {
                    Map<String, String> filteredMetrics = applyExcludePatternsAndExtractMetrics(allMbeans,membersMap);
                    filteredMetrics.put(CoherenceMonitorConstants.METRICS_COLLECTION_SUCCESSFUL, CoherenceMonitorConstants.SUCCESS_VALUE);
                    coherenceMetrics.setMetrics(filteredMetrics);
                }
            }
        }
        catch(Exception e){
            logger.error("Error JMX-ing into the server :: " +coherenceMetrics.getDisplayName() + e);
            coherenceMetrics.getMetrics().put(CoherenceMonitorConstants.METRICS_COLLECTION_SUCCESSFUL, CoherenceMonitorConstants.ERROR_VALUE);
        }
        finally{
            jmxConnector.close();
        }
        return coherenceMetrics;
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

    private Map<String, String> applyExcludePatternsAndExtractMetrics(Set<ObjectInstance> allMbeans,Map<String,CoherenceMember> membersMap) {
        Map<String,String> filteredMetrics = new HashMap<String, String>();
        for(ObjectInstance mbean : allMbeans){
            ObjectName objectName = mbean.getObjectName();
            if(isDomainConfigured(objectName)){
                MBeanData mBeanData = mbeanLookup.get(objectName.getDomain());
                Set<String> excludePatterns = mBeanData.getExcludePatterns();
                MBeanAttributeInfo[] attributes = jmxConnector.fetchAllAttributesForMbean(objectName);
                if(attributes != null) {
                    for (MBeanAttributeInfo attr : attributes) {
                        // See we do not violate the security rules, i.e. only if the attribute is readable.
                        if (attr.isReadable()) {
                            Object attribute = jmxConnector.getMBeanAttribute(objectName, attr.getName());
                            //AppDynamics only considers number values
                            if (isMetricValueValid(attribute)) {
                                String metricKey = getMetricsKey(objectName,attr,membersMap);
                                if (!isKeyExcluded(metricKey, excludePatterns)) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Metric key:value before ceiling = "+ metricKey + ":" + String.valueOf(attribute));
                                    }
                                    String attribStr = MetricUtils.toWholeNumberString(attribute);
                                    filteredMetrics.put(metricKey, attribStr);
                                } else {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug(metricKey + " is excluded");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return filteredMetrics;
    }


    private boolean isMetricValueValid(Object value) {
        boolean valid =  (value != null && value instanceof Number);
        if(valid){
            if(value instanceof Double){
                return (Double)value >= 0;
            }
            if(value instanceof Long){
                return (Long)value >= 0;
            }
            if(value instanceof Integer){
                return (Integer)value >= 0;
            }
        }
        return false;
    }

    /**
     * Checks if the given metric key matches any exclude patterns
     *
     * @param metricKey
     * @param excludePatterns
     * @return true if match, false otherwise
     */
    private boolean isKeyExcluded(String metricKey, Set<String> excludePatterns) {
        for(String excludePattern : excludePatterns){
            if(metricKey.matches(escapeText(excludePattern))){
                return true;
            }
        }
        return false;
    }

    private String escapeText(String excludePattern) {
        return excludePattern.replaceAll("\\|","\\\\|");
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

    private String getMemberInfo(CoherenceMember node) {
        StringBuilder memberInfo = new StringBuilder();
        memberInfo.append(Strings.isNullOrEmpty(node.getMachineName()) ? "" : node.getMachineName() + METRICS_SEPARATOR);
        memberInfo.append(Strings.isNullOrEmpty(node.getMemberName()) ? "" : node.getMemberName() + METRICS_SEPARATOR);
        return memberInfo.toString();
    }


    private boolean isDomainConfigured(ObjectName objectName) {
        return (mbeanLookup.get(objectName.getDomain()) != null);
    }


}
