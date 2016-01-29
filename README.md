coherence-monitoring-extension
==============================

An AppDynamics extension to be used with a stand alone Java machine agent to provide metrics for Oracle's Coherence.


## Use Case ##

Oracle Coherence is the industry leading in-memory data grid solution that enables organizations to predictably scale mission-critical applications by providing fast access to frequently used data. As data volumes and customer expectations increase, driven by the “internet of things”, social, mobile, cloud and always-connected devices, so does the need to handle more data in real-time, offload over-burdened shared data services and provide availability guarantees.

## Prerequisites ##

This extension extracts the metrics from Coherence using the JMX protocol. 
By default, coherence starts with local or remote JMX disabled. Please follow the below link to enable JMX 

http://docs.oracle.com/middleware/1212/coherence/COHMG/jmx.htm#BABJCEFH

To know more about JMX, please follow the below link
 
 http://docs.oracle.com/javase/6/docs/technotes/guides/management/agent.html


## Troubleshooting steps ##
Before configuring the extension, please make sure to run the below steps to check if the set up is correct.

1. Telnet into your coherence server from the box where the extension is deployed.
       telnet <hostname> <port>

       <port> - It is the jmxremote.port specified.
        <hostname> - IP address

    If telnet works, it confirm the access to the coherence server.


2. Start jconsole. Jconsole comes as a utility with installed jdk. After giving the correct host and port , check if Coherence
mbean shows up.

## Metrics Provided ##

In addition to the metrics exposed by Coherence, we also add a metric called "Metrics Collection Successful" with a value -1 when an error occurs and 1 when the metrics collection is successful.

Note : By default, a Machine agent or a AppServer agent can send a fixed number of metrics to the controller. To change this limit, please follow the instructions mentioned [here](http://docs.appdynamics.com/display/PRO14S/Metrics+Limits).
For eg.  
```    
    java -Dappdynamics.agent.maxMetrics=2500 -jar machineagent.jar
```


## Installation ##

1. Run "mvn clean install" and find the CoherenceMonitor.zip file in the "target" folder. You can also download the CoherenceMonitor.zip from [AppDynamics Exchange][].
2. Unzip as "CoherenceMonitor" and copy the "CoherenceMonitor" directory to `<MACHINE_AGENT_HOME>/monitors`


# Configuration ##

Note : Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the coherence instances by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/CoherenceMonitor/`.
2. Below is the default config.yml which has metrics configured already
   For eg. 
   
   ```
      ### ANY CHANGES TO THIS FILE DOES NOT REQUIRE A RESTART ###

      #This will create this metric in all the tiers, under this path
      metricPrefix: Custom Metrics|Coherence

      #This will create it in specific Tier. Replace <TIER_NAME>
      #metricPrefix: Server|Component:<TIER_NAME>|Custom Metrics|Coherence

      # List of Coherence Servers
      servers:
        - host: ""
          port:
          username: ""
          password: ""
          displayName: "" #displayName is a required field. This will be your server name that will show up in metric path.


      # number of concurrent tasks.
      # This doesn't need to be changed unless many servers are configured
      numberOfThreads: 10


      # The configuration of different metrics from various mbeans of coherence server
      # For most cases, the mbean configuration does not need to be changed.
      #
      mbeans:
        # This mbean is to get cluster related metrics.
        - objectName: "Coherence:type=Cluster"
          metrics:
            include:
              - Members : "Members"  # If this attribute is removed, nodeIds will be seen in the metric paths and not their corressponding names.
              - ClusterSize : "ClusterSize"

        - objectName: "Coherence:type=Cache,service=DistributedCache,name=*,nodeId=*,tier=*"
          metrics:
            include:
              - CacheHits : "CacheHits" #The rough number of cache hits since the last time statistics were reset. A cache hit is a read operation invocation (that is, get()) for which an entry exists in this map.
              - CacheMisses : "CacheMisses" #The rough number of cache misses since the last time statistics were reset.
              - CachePrunes : "CachePrunes" #The number of prune operations since the last time statistics were reset. A prune operation occurs every time the cache reaches its high watermark as specified by the HighUnits attribute.
              - TotalGets : "TotalGets" #The total number of get() operations since the last time statistics were reset.
              - TotalPuts : "TotalPuts" #The total number of put() operations since the last time statistics were reset.
              - UnitFactor : "UnitFactor" #The factor by which the Units, LowUnits and HighUnits properties are adjusted. Using a BINARY unit calculator, for example, the factor of 1048576 could be used to count megabytes instead of bytes.
              - Units : "Units" #The size of the cache measured in units. This value needs to be adjusted by the UnitFactor.
              - Size : "Size" #The number of entries in the cache.
              # A derived metric for CacheHitRatio=CacheHits/TotalGets is calculated for every cache.

        # This mbean will give cache node specific metrics.
        - objectName: "Coherence:type=Node,nodeId=*"
          metrics:
            include:
              - MemoryAvailableMB : "MemoryAvailableMB" #The total amount of memory in the JVM available for new objects in MB.
              - MemoryMaxMB : "MemoryMaxMB" #The maximum amount of memory that the JVM will attempt to use in MB.

        - objectName: "Coherence:type=Service,name=DistributedCache,nodeId=*"
          metrics:
            include:
              - TaskBacklog : "TaskBacklog" #The size of the backlog queue that holds tasks scheduled to be executed by one of the service pool threads.

        - objectName: "Coherence:type=StorageManager,service=DistributedCache,cache=*,nodeId=*"
          metrics:
            include:
              - EvictionCount : "EvictionCount" #The total number of evictions from the backing map managed by this Storage Manager.
              - EventsDispatched : "EventsDispatched" #The total number of events dispatched by the Storage Manager per minute.
              - NonOptimizedQueryCount : "NonOptimizedQueryCount" #The total number of queries that could not be resolved or were partially resolved against indexes since statistics were last reset.
              - NonOptimizedQueryAverageMillis : "NonOptimizedQueryAverageMillis" #The average duration in milliseconds per non-optimized query execution since the statistics were last reset.
              - OptimizedQueryAverageMillis : "OptimizedQueryAverageMillis"  #The average duration in milliseconds per optimized query execution since the statistics were last reset.
              - OptimizedQueryCount : "OptimizedQueryCount" #The total number of queries that were fully resolved using indexes since statistics were last reset.

        # This mbean will provide system/OS level metrics for every coherence node.
        - objectName: "Coherence:type=Platform,Domain=java.lang,subType=OperatingSystem,nodeId=*"
          metrics:
            include:
              - FreePhysicalMemorySize : "FreePhysicalMemorySize" #The amount of free physical memory available.
              - FreeSwapSpaceSize : "FreeSwapSpaceSize" #The amount of free swap space available.
              - OpenFileDescriptorCount : "OpenFileDescriptorCount" #The number of open file descriptors available.
              - ProcessCpuLoad : "ProcessCpuLoad"
              - SystemCpuLoad : "SystemCpuLoad"
              - TotalPhysicalMemorySize : "TotalPhysicalMemorySize"
              - TotalSwapSpaceSize : "TotalSwapSpaceSize"





       

   ```


The objectNames mentioned in the above yaml may not match your environment exactly. Please use jconsole to extract the objectName and configure it
accordingly in the config.yaml. For eg. you may not find




3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/CoherenceMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/CoherenceMonitor/config.yml" />
          ....
     </task-arguments>
    ```


## Contributing ##

Always feel free to fork and contribute any changes directly via [GitHub][].

## Community ##

Find out more in the [AppDynamics Exchange][].

## Support ##

For any questions or feature request, please contact [AppDynamics Center of Excellence][].

**Version:** 1.0.0
**Controller Compatibility:** 3.7+
**Coherence Versions Tested On:** 12c or 12.1.3

[Github]: https://github.com/Appdynamics/coherence-monitoring-extension
[AppDynamics Exchange]: http://community.appdynamics.com/t5/AppDynamics-eXchange/idb-p/extensions
[AppDynamics Center of Excellence]: mailto:ace-request@appdynamics.com
