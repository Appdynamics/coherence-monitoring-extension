package com.appdynamics.extensions.coherence;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.appdynamics.TaskInputArgs.PASSWORD_ENCRYPTED;
import static com.appdynamics.extensions.coherence.ConfigConstants.*;
import static com.appdynamics.extensions.coherence.Util.convertToString;

/**
 * This extension will extract out metrics from Coherence through the JMX protocol.
 */
public class CoherenceMonitor extends AManagedMonitor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CoherenceMonitor.class);
    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_PREFIX = "Custom Metrics|Coherence|";


    private boolean initialized;
    private MonitorConfiguration configuration;

    public CoherenceMonitor() {
        System.out.println(logVersion());
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext out) throws TaskExecutionException {
        logVersion();
        if(!initialized){
            initialize(taskArgs);
        }
        logger.debug("The raw arguments are {}", taskArgs);
        configuration.executeTask();
        logger.info("Coherence monitor run completed successfully.");
        return new TaskOutput("Coherence monitor run completed successfully.");

    }

    private void initialize(Map<String, String> taskArgs) {
        if(!initialized){
            //read the config.
            final String configFilePath = taskArgs.get(CONFIG_ARG);
            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);
            conf.setConfigYml(configFilePath);
            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE,
                    MonitorConfiguration.ConfItem.METRIC_PREFIX,MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable{

        public void run() {
            Map<String, ?> config = configuration.getConfigYml();
            if (config != null) {
                List<Map> servers = (List<Map>) config.get(INSTANCES);
                if (servers != null && !servers.isEmpty()) {
                    for (Map server : servers) {
                        try {
                            CoherenceMonitorTask task = createTask(server);
                            configuration.getExecutorService().execute(task);
                            /*try {
                                Thread.sleep(100000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }*/
                        } catch (IOException e) {
                            logger.error("Cannot construct JMX uri for {}",convertToString(server.get(DISPLAY_NAME),""));
                        }

                    }
                } else {
                    logger.error("There are no servers configured");
                }
            } else {
                logger.error("The config.yml is not loaded due to previous errors.The task will not run");
            }
        }
    }

    private CoherenceMonitorTask createTask(Map server) throws IOException {
        String serviceUrl = convertToString(server.get(SERVICE_URL),"");
        String host = convertToString(server.get(HOST),"");
        String portStr = convertToString(server.get(PORT),"");
        int port = portStr != null ? Integer.parseInt(portStr) : -1;
        String username = convertToString(server.get(USERNAME),"");
        String password = getPassword(server);

        JMXConnectionAdapter adapter = JMXConnectionAdapter.create(serviceUrl,host,port,username,password);
        return new CoherenceMonitorTask.Builder()
                .metricPrefix(configuration.getMetricPrefix())
                .metricWriter(configuration.getMetricWriter())
                .jmxConnectionAdapter(adapter)
                .server(server)
                .mbeans((List<Map>)configuration.getConfigYml().get(MBEANS))
                .build();
    }

    private String getPassword(Map server) {
        String password = convertToString(server.get(PASSWORD),"");
        if(!Strings.isNullOrEmpty(password)){
            return password;
        }
        String encryptionKey = convertToString(configuration.getConfigYml().get(ConfigConstants.ENCRYPTION_KEY),"");
        String encryptedPassword = convertToString(server.get(ENCRYPTED_PASSWORD),"");
        if(!Strings.isNullOrEmpty(encryptionKey) && !Strings.isNullOrEmpty(encryptedPassword)){
            java.util.Map<String,String> cryptoMap = Maps.newHashMap();
            cryptoMap.put(PASSWORD_ENCRYPTED,encryptedPassword);
            cryptoMap.put(TaskInputArgs.ENCRYPTION_KEY,encryptionKey);
            return CryptoUtil.getPassword(cryptoMap);
        }
        return null;
    }

    private static String getImplementationVersion() {
        return CoherenceMonitor.class.getPackage().getImplementationTitle();
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }
}
