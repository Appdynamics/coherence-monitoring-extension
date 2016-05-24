package com.appdynamics.extensions.coherence;


import com.appdynamics.extensions.coherence.config.MBean;
import com.appdynamics.extensions.coherence.config.Server;
import com.appdynamics.extensions.jmx.MBeanKeyPropertyEnum;
import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.appdynamics.extensions.coherence.CoherenceMonitorConstants.*;
import static com.appdynamics.extensions.util.metrics.MetricConstants.METRICS_SEPARATOR;

public class CoherenceMonitorTask implements Runnable {

    public static final double DEFAULT_MULTIPLIER = 1d;
    public static final String DEFAULT_METRIC_TYPE = MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE + " " + MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE + " " + MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL;
    public static final String MEMBERS_ATTRIB = "Members";
    public static final String COHERENCE_TYPE_CLUSTER = "Coherence:type=Cluster";
    public static final String TOTAL_GETS_ATTRIB = "TotalGets";
    public static final String CACHE_HITS_ATTRIB = "CacheHits";
    public static final String THREAD_COUNT = "ThreadCount";
    public static final String THREAD_IDLE_COUNT = "ThreadIdleCount";
    public static final String CACHE_HIT_RATIO_PERCENTAGE_ATTRIB = "CacheHitRatioPercentage";
    public static final String THREAD_UTILIZATION_PERCENTAGE_ATTRIB = "ThreadUtilizationPercentage";
    public static final String CLUSTER_AGGREGATION_IN_MA = "clusterAggregationInMA";
    public static final org.slf4j.Logger logger = LoggerFactory.getLogger(CoherenceMonitorTask.class);

    private String metricPrefix;
    private Server server;
    private AManagedMonitor metricWriter;
    private JMXServiceURL serviceURL;
    private List<MBean> mbeans;
    private Map<String,CoherenceMember> membersMap = Maps.newHashMap();
    private int totalMetricsReported;





    public void run() {
        long startTime = System.currentTimeMillis();
        try {
            logger.debug("Coherence monitor thread for server {} started.",server.getDisplayName());
            extractAndReportMetrics();
            printMetric(formMetricPath(METRICS_COLLECTION_SUCCESSFUL), SUCCESS_VALUE
                    , MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        } catch (Exception e) {
            logger.error("Error in Coherence Monitor thread for server {}", server.getDisplayName(), e);
            printMetric(formMetricPath(METRICS_COLLECTION_SUCCESSFUL), ERROR_VALUE
                    , MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

           // System.out.println(e);
        }
        finally{
            long endTime = System.currentTimeMillis() - startTime;
            logger.debug("Coherence monitor thread for server {} ended. Time taken = {} and Total metrics reported = {}",server.getDisplayName(),endTime,totalMetricsReported);
        }
    }


    private void extractAndReportMetrics() throws Exception {
        JMXConnector connector = null;

        try{
            connector = createJMXConnector();
            if(connector == null){
                throw new IOException("Unable to connect to Mbean server");
            }
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            for(MBean mBean : mbeans){
                try {
                    Map<String,MetricAggregator> clusterAggregatorForEachBean = Maps.newHashMap();
                    ObjectName objectName = ObjectName.getInstance(mBean.getObjectName());
                    Set<ObjectInstance> objectInstances = connection.queryMBeans(objectName, null);
                    for(ObjectInstance instance : objectInstances){

                        //gathering metric names by applying exclude filter if present.
                        List excludeMetrics = (List)mBean.getMetrics().get("exclude");
                        Set<String> metricsToBeReported = Sets.newHashSet();
                        if(excludeMetrics != null){
                            gatherMetricNamesByApplyingExcludeFilter(connection, instance, excludeMetrics, metricsToBeReported);
                        }
                        //gathering metric names by applying include filter if present.
                        List includeMetrics = (List)mBean.getMetrics().get("include");
                        Map<String,MetricOverride> overrideMap = Maps.newHashMap();
                        if(includeMetrics != null){
                            gatherMetricNamesByApplyingIncludeFilter(includeMetrics,metricsToBeReported);
                            populateOverridesMap(includeMetrics, overrideMap);
                        }
                        //getting all the metrics from MBean server and overriding them if
                        AttributeList attributeList = connection.getAttributes(instance.getObjectName(), metricsToBeReported.toArray(new String[metricsToBeReported.size()]));
                        List<Attribute> list = attributeList.asList();

                        Map<MetricOverride,BigInteger> nodeMetrics = Maps.newHashMap();
                        collectMetrics(list,nodeMetrics,mBean.isClusterLevelReporting(), clusterAggregatorForEachBean, instance.getObjectName(), overrideMap);

                        //reporting metrics at node level.
                        reportNodeMetrics(nodeMetrics);

                    }
                    reportClusterLevelMetrics(clusterAggregatorForEachBean);
                }
                catch(MalformedObjectNameException e){
                    logger.error("Illegal object name {}" + mBean.getObjectName(),e);
                    throw e;
                }
                catch (Exception e){
                    //System.out.print("" + e);
                    logger.error("Error fetching JMX metrics for {} and mbean={}", server.getDisplayName(), mBean.getObjectName(), e);
                    throw e;
                }
            }

        }
        finally{
            if(connector != null) {
                connector.close();
            }
        }
    }

    private void reportNodeMetrics(Map<MetricOverride, BigInteger> nodeMetrics) {
        Map<String,BigInteger> derivedMetrics = Maps.newHashMap();
        Set<String> derivedMetricSlugs = Sets.newHashSet();
        for (Map.Entry<MetricOverride, BigInteger> entry : nodeMetrics.entrySet()) {
            MetricOverride nodeMetric = entry.getKey();
            BigInteger value = entry.getValue();
            String key = nodeMetric.getMetricKey();
            printMetric(formMetricPath(key), value.toString(),nodeMetric.getAggregator(),nodeMetric.getTimeRollup(),nodeMetric.getClusterRollup());
            computeDerivedMetrics(nodeMetric,value,derivedMetrics,derivedMetricSlugs);
        }
        //report cache hit ratio
        for(String derivedSlug : derivedMetricSlugs) {
            reportCacheHitRatio(derivedMetrics, derivedSlug);
            reportThreadUtilization(derivedMetrics,derivedSlug);
        }
    }

    private void reportThreadUtilization(Map<String, BigInteger> derivedMetrics, String derivedSlug) {
        BigInteger threadCount = derivedMetrics.get(derivedSlug + THREAD_COUNT);
        BigInteger threadIdleCount = derivedMetrics.get(derivedSlug + THREAD_IDLE_COUNT);
        if(threadCount != null && threadIdleCount != null){
            //utilization = (threadCount-threadIdleCount/threadCount) * 100;
            String value = computeRatioPercentage(threadCount, threadCount.subtract(threadIdleCount));
            if (!Strings.isNullOrEmpty(value)) {
                printMetric(formMetricPath(derivedSlug + THREAD_UTILIZATION_PERCENTAGE_ATTRIB), value, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            }
        }
    }



    private void reportCacheHitRatio(Map<String, BigInteger> derivedMetrics, String derivedSlug) {
        BigInteger totalGets = derivedMetrics.get(derivedSlug + TOTAL_GETS_ATTRIB);
        BigInteger totalHits = derivedMetrics.get(derivedSlug + CACHE_HITS_ATTRIB);
        if(totalGets != null && totalHits != null) {
            String value = computeRatioPercentage(totalGets, totalHits);
            if (!Strings.isNullOrEmpty(value)) {
                printMetric(formMetricPath(derivedSlug + CACHE_HIT_RATIO_PERCENTAGE_ATTRIB), value, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

            }
        }
    }

    private void computeDerivedMetrics(MetricOverride nodeMetric, BigInteger value, Map<String, BigInteger> derivedMetrics, Set<String> derivedMetricSlugs) {
        String metricKey = nodeMetric.getMetricKey();
        String metricSlug = metricKey.substring(0, metricKey.lastIndexOf(METRICS_SEPARATOR) + 1);
        if(metricKey.endsWith(TOTAL_GETS_ATTRIB) || metricKey.endsWith(CACHE_HITS_ATTRIB) || metricKey.endsWith(THREAD_COUNT) || metricKey.endsWith(THREAD_IDLE_COUNT)){
            derivedMetrics.put(metricKey,value);
            derivedMetricSlugs.add(metricSlug);
        }
    }

    private void collectMetrics(List<Attribute> list, Map<MetricOverride, BigInteger> nodeMetrics, boolean clusterLevelReporting, Map<String, MetricAggregator> clusterAggregatorForEachBean,  ObjectName objectName, Map<String, MetricOverride> overrideMap) {


        for (Attribute attr : list) {
            //creating a member map
            if(matchObjectName(objectName,COHERENCE_TYPE_CLUSTER) && matchAttributeName(attr,MEMBERS_ATTRIB)){
                createMemberMap(attr.getValue());
            }else if(matchAttributeName(attr,"StatusHA")){
                //Converting the StatusHA String output to BigDecimal Values
                try {
                    BigInteger bigVal = BigInteger.valueOf(CoherenceStatusHAEnum.getStatusHACode(attr.getValue().toString()));
                    String[] metricTypes = getMetricTypes(overrideMap, attr.getName());

                    //node metrics
                    String nodeMetricKey = getMetricsKey(objectName, getMetricName(overrideMap, attr.getName()), true);
                    MetricOverride nodeMetric = createMetricOverride(nodeMetricKey, metricTypes);
                    nodeMetrics.put(nodeMetric, bigVal);
                }catch(Exception e){
                    logger.error("Cannot report statusHA",e);
                }

            }
            else if (isMetricValueValid(attr.getValue())) {
                BigInteger bigVal = toBigInteger(attr.getValue(), getMultiplier(overrideMap, attr.getName()));
                String[] metricTypes = getMetricTypes(overrideMap,attr.getName());

                //node metrics
                String nodeMetricKey = getMetricsKey(objectName, getMetricName(overrideMap, attr.getName()), true);
                MetricOverride nodeMetric = createMetricOverride(nodeMetricKey,metricTypes);
                nodeMetrics.put(nodeMetric,bigVal);

                //cluster metrics
                //reporting metrics at cluster level. This is different than the cluster-level aggregation which happens in the controller.
                // This may sound has a hack but customers want to install coherence extension on only 1 coherence management node
                // in the cluster and not on each node in the cluster. Because of this, cluster-level or tier-level aggregation becomes meaningless in
                //controller. Here, the aggregation of AVERAGE or SUM for cluster level metrics is done in the MA itself. The cluster level metrics
                // are nothing but all the metrics without the node information. By default the cluster level aggregation is AVG but it can be
                //overridden by configuring "clusterAggregationInMA" in config file.
                if(clusterLevelReporting) {
                    String clusterMetricKey = getMetricsKey(objectName, getMetricName(overrideMap, attr.getName()), false);
                    String clusterAggregationInMA = getClusterAggregationInMA(overrideMap,attr.getName());
                    MetricAggregator aggregator = clusterAggregatorForEachBean.get(clusterMetricKey);
                    if (aggregator == null) {
                        aggregator = new MetricAggregator(clusterAggregationInMA,metricTypes[0],metricTypes[1],metricTypes[2]);
                        clusterAggregatorForEachBean.put(clusterMetricKey, aggregator);
                    }
                    aggregator.report(bigVal.longValue());
                }

            }
        }
    }






    private MetricOverride createMetricOverride(String nodeMetricKey, String[] metricTypes) {
        MetricOverride override = new MetricOverride();
        override.setMetricKey(nodeMetricKey);
        override.setAggregator(metricTypes[0]);
        override.setTimeRollup(metricTypes[1]);
        override.setClusterRollup(metricTypes[2]);
        return override;
    }


    private void reportClusterLevelMetrics(Map<String, MetricAggregator> clusterAggregatorMap) {
        for (Map.Entry<String, MetricAggregator> entry : clusterAggregatorMap.entrySet()) {
            String key = entry.getKey();
            MetricAggregator value = entry.getValue();
            printMetric(formMetricPath(key), Long.toString(value.aggregate()),value.aggregationType,value.timeRollup,value.clusterRollup);
        }
    }



    private String getClusterAggregationInMA(Map<String, MetricOverride> overrideMap, String name) {
        if(overrideMap.get(name) == null || overrideMap.get(name).getOtherProps().get(CLUSTER_AGGREGATION_IN_MA) == null){
            return MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE;
        }
        return overrideMap.get(name).getOtherProps().get(CLUSTER_AGGREGATION_IN_MA);
    }


    private String computeRatioPercentage(BigInteger divisor, BigInteger dividend) {
        if(divisor == BigInteger.ZERO){
            return null;
        }
        return dividend.divide(divisor).multiply(BigInteger.valueOf(100)).toString() ;
    }

    private boolean matchObjectName(ObjectName objectName,String matchedWith) {
        return objectName.toString().equalsIgnoreCase(matchedWith);
    }

    private boolean matchAttributeName(Attribute attribute,String matchedWith){
        return attribute.getName().equalsIgnoreCase(matchedWith);
    }

    private String[] getMetricTypes(Map<String, MetricOverride> overrideMap, String name) {
        if(overrideMap.get(name) == null){
            return DEFAULT_METRIC_TYPE.split(" ");
        }
        MetricOverride override = overrideMap.get(name);
        return new String[]{override.getAggregator(),override.getTimeRollup(),override.getClusterRollup()};
    }

    private Double getMultiplier(Map<String, MetricOverride> overrideMap,String name) {
        if(overrideMap.get(name) == null){
            return DEFAULT_MULTIPLIER;
        }
        return overrideMap.get(name).getMultiplier();
    }

    private String getMetricName(Map<String, MetricOverride> overrideMap, String name) {
        if(overrideMap.get(name) == null){
            return name;
        }
        return overrideMap.get(name).getAlias();
    }

    private JMXConnector createJMXConnector() throws IOException {
        JMXConnector jmxConnector;
        final Map<String, Object> env = new HashMap<String, Object>();
        if(!Strings.isNullOrEmpty(server.getUsername())){
            env.put(JMXConnector.CREDENTIALS,new String[]{server.getUsername(),server.getPassword()});
            jmxConnector = JMXConnectorFactory.connect(serviceURL, env);
        }
        else{
            jmxConnector = JMXConnectorFactory.connect(serviceURL);
        }
        return jmxConnector;
    }


    private void gatherMetricNamesByApplyingExcludeFilter(MBeanServerConnection connection, ObjectInstance instance, List excludeMetrics, Set<String> metrics) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
        MBeanAttributeInfo[] attributes = connection.getMBeanInfo(instance.getObjectName()).getAttributes();
        for(MBeanAttributeInfo attr : attributes){
            if (!excludeMetrics.contains(attr.getName())) {
                if (attr.isReadable()) {
                    metrics.add(attr.getName());
                }
            }
        }
    }

    private void gatherMetricNamesByApplyingIncludeFilter(List includeMetrics,Set<String> metrics) {
        for(Object inc : includeMetrics){
            Map metric = (Map) inc;
            //Get the First Entry which is the metric
            Map.Entry firstEntry = (Map.Entry) metric.entrySet().iterator().next();
            String metricName = firstEntry.getKey().toString();
            metrics.add(metricName); //to get jmx metrics
        }
    }


    private String formMetricPath(String metricKey) {
        if(!Strings.isNullOrEmpty(server.getDisplayName())){
            return metricPrefix + server.getDisplayName() + METRICS_SEPARATOR + metricKey;
        }
        return metricPrefix + metricKey;
    }



    private void populateOverridesMap(List includeMetrics, Map<String, MetricOverride> overrideMap) {
        for(Object inc : includeMetrics){
            Map metric = (Map) inc;
            //Get the First Entry which is the metric
            Map.Entry firstEntry = (Map.Entry) metric.entrySet().iterator().next();
            String metricName = firstEntry.getKey().toString();
            MetricOverride override = new MetricOverride();
            override.setAlias(firstEntry.getValue().toString());
            override.setMultiplier(metric.get("multiplier") != null ? Double.parseDouble(metric.get("multiplier").toString()) : DEFAULT_MULTIPLIER);
            String metricType = metric.get("metricType") != null ? metric.get("metricType").toString() : DEFAULT_METRIC_TYPE;
            String[] metricTypes = metricType.split(" ");
            override.setAggregator(metricTypes[0]);
            override.setTimeRollup(metricTypes[1]);
            override.setClusterRollup(metricTypes[2]);
            Object clusterAggregationInMA = metric.get(CLUSTER_AGGREGATION_IN_MA);
            if(clusterAggregationInMA == null){
                override.getOtherProps().put(CLUSTER_AGGREGATION_IN_MA,MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE);
            }
            else{
                override.getOtherProps().put(CLUSTER_AGGREGATION_IN_MA,clusterAggregationInMA.toString());
            }

            overrideMap.put(metricName, override);
        }
    }



    private BigInteger toBigInteger(Object value,Double multiplier) {
        try {
            BigDecimal bigD = new BigDecimal(value.toString());
            if(multiplier != null && multiplier != DEFAULT_MULTIPLIER) {

                bigD = bigD.multiply(new BigDecimal(multiplier));
            }
            return bigD.setScale(0, RoundingMode.HALF_UP).toBigInteger();
        }
        catch(NumberFormatException nfe){
        }
        return BigInteger.ZERO;
    }



    private void printMetric(String metricPath,String metricValue,String aggType,String timeRollupType,String clusterRollupType) {
        MetricWriter writer = metricWriter.getMetricWriter(metricPath,
                aggType,
                timeRollupType,
                clusterRollupType
        );
      //  System.out.println("Sending [" + aggType + METRICS_SEPARATOR + timeRollupType + METRICS_SEPARATOR + clusterRollupType
      //  		+ "] metric = " + metricPath + " = " + metricValue);
        logger.debug("Sending [{}|{}|{}] metric= {},value={}", aggType, timeRollupType, clusterRollupType, metricPath, metricValue);
        writer.printMetric(metricValue);
        totalMetricsReported++;
    }





    private boolean isMetricValueValid(Object metricValue) {
        if(metricValue == null){
            return false;
        }
        if(metricValue instanceof String){
            try {
                Double.valueOf((String) metricValue);
                return true;
            }
            catch(NumberFormatException nfe){
            }
        }
        else if(metricValue instanceof Number){
            return true;
        }
        return false;
    }

    private String getMetricsKey(ObjectName objectName, String attribName, boolean nodeLevel) {
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
        if(nodeLevel) {
            metricsKey.append(Strings.isNullOrEmpty(nodeId) ? "" : ("Nodes" + METRICS_SEPARATOR + getNodeMember(nodeId)));
        }
        else{
            metricsKey.append("Cluster").append(METRICS_SEPARATOR);
        }
        metricsKey.append(Strings.isNullOrEmpty(tier) ? "" : tier + METRICS_SEPARATOR);
        metricsKey.append(Strings.isNullOrEmpty(responsibility) ? "" : responsibility + METRICS_SEPARATOR);
        metricsKey.append(attribName);
        return metricsKey.toString();
    }

    private String getNodeMember(String nodeId) {
        if(membersMap.get(nodeId) == null){
            return nodeId + METRICS_SEPARATOR;
        }
        String memberInfo = getMemberInfo(membersMap.get(nodeId));
        if (Strings.isNullOrEmpty(memberInfo)) {
            return nodeId + METRICS_SEPARATOR;
        }
        return memberInfo;
    }

    private void createMemberMap(Object members) {
        if(members != null ){
            String[] membersArr = (String[]) members;
            for(String member : membersArr){
                CoherenceMember node = new CoherenceMember(member);
                if(node.getId() != null){
                    membersMap.put(node.getId(),node);
                }
            }
        }
    }

    private String getMemberInfo(CoherenceMember node) {
        StringBuilder memberInfo = new StringBuilder();
        memberInfo.append(Strings.isNullOrEmpty(node.getMachineName()) ? "" : node.getMachineName() + METRICS_SEPARATOR);
        memberInfo.append(Strings.isNullOrEmpty(node.getMemberName()) ? "" : node.getMemberName() + METRICS_SEPARATOR);
        return memberInfo.toString();
    }


    public static class Builder {
        private CoherenceMonitorTask task = new CoherenceMonitorTask();

        public Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        public Builder metricWriter(CoherenceMonitor metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        public Builder server(Server server){
            task.server = server;
            return this;
        }

        public Builder serviceURL(JMXServiceURL serviceURL){
            task.serviceURL = serviceURL;
            return this;
        }

        public Builder mbeans(List<MBean> mBeans){
            task.mbeans = mBeans;
            return this;
        }

        public CoherenceMonitorTask build() {
            return task;
        }
    }
}
