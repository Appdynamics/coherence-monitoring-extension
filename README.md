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
2. Configure the MBeans in the config.yml. By default, "Coherence" is all that you may need. But you can add more mbeans as per your requirement.
   You can also add excludePatterns (regex) to exclude any metric tree from showing up in the AppDynamics controller.
   
   For eg. 
   
   ```
       # List of coherence servers
       servers:
         - host: "192.168.57.102"
           port: 1984
           username: ""
           password: ""
           displayName: "localhost"
       
       
       # coherence mbeans. Exclude patterns with regex can be used to exclude any unwanted metrics.
       mbeans:
         - domainName: "Coherence"
           excludePatterns: [
              "Service|ReplicatedCache|.*",
              "Reporter|.*"
       
           ]
       
       # number of concurrent tasks
       numberOfThreads: 10
       
       #timeout for the thread
       threadTimeout: 300000
       
       #prefix used to show up metrics in AppDynamics
       metricPrefix:  "Custom Metrics|Coherence|"
       

   ```
   
   In the above config file, metrics are being pulled from Coherence mbean domain. Note that the patterns mentioned in the "excludePatterns" will be excluded from showing up in the AppDynamics dashboard.

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
