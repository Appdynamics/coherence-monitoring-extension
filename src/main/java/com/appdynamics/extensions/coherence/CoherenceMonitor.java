package com.appdynamics.extensions.coherence;

import com.appdynamics.extensions.coherence.config.ConfigUtil;
import com.appdynamics.extensions.coherence.config.Configuration;
import com.appdynamics.extensions.coherence.config.Server;
import com.appdynamics.extensions.jmx.JMXConnectionConfig;
import com.appdynamics.extensions.jmx.JMXConnectionUtil;
import com.appdynamics.extensions.util.metrics.MetricOverride;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This extension will extract out metrics from Coherence through the JMX protocol.
 */
public class CoherenceMonitor extends AManagedMonitor {

    public static final Logger logger = Logger.getLogger(CoherenceMonitor.class);
    public static final String CONFIG_ARG = "config-file";
    private static final int DEFAULT_NUMBER_OF_THREADS = 10;
    public static final int DEFAULT_THREAD_TIMEOUT = 10;

    private ExecutorService threadPool;

    //To load the config files
    private final static ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();


    public CoherenceMonitor() {
        System.out.println(logVersion());

    }



    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        logVersion();
        if (taskArgs != null) {
            logger.info("Starting the Coherence Monitoring task.");
            if (logger.isDebugEnabled()) {
                logger.debug("Task Arguments Passed ::" + taskArgs);
            }

            try {
                //read the config.
                Configuration config = YmlReader.readFromFile(new File(taskArgs.get(CONFIG_ARG)), Configuration.class);
                threadPool = Executors.newFixedThreadPool(config.getNumberOfThreads() == 0 ? DEFAULT_NUMBER_OF_THREADS : config.getNumberOfThreads());
                //parallel execution for each server.
                runConcurrentTasks(config);
                logger.info("Coherence monitoring task completed successfully.");
                return new TaskOutput("Coherence monitoring task completed successfully.");
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Metrics collection failed", e);
            } finally {
                if(!threadPool.isShutdown()){
                    threadPool.shutdown();
                }
            }

        }
        throw new TaskExecutionException("Coherence monitoring task completed with failures.");
    }


    /**
     * Executes concurrent tasks
     *
     * @param config
     * @return Handles to concurrent tasks.
     */
    private void runConcurrentTasks(Configuration config) {
        List<Future<Void>> parallelTasks = new ArrayList<Future<Void>>();
        if (config != null && config.getServers() != null) {
            for (Server server : config.getServers()) {
                MetricOverride[] metricOverrides = (server.getMetricOverrides() != null) ? server.getMetricOverrides() : config.getMetricOverrides();
                JMXConnectionUtil jmxConnector = createJMXConnector(server);
                //passing the context to the task.
                CoherenceMonitorTask coherenceTask = new CoherenceMonitorTask(config.getMetricPrefix(),server.getDisplayName(),metricOverrides,jmxConnector,this);
                parallelTasks.add(threadPool.submit(coherenceTask));
            }
        }
        for (Future<Void> aTask : parallelTasks) {
            try {
                aTask.get(config.getThreadTimeout(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Task interrupted.", e);
            } catch (ExecutionException e) {
                logger.error("Task execution failed.", e);
            } catch (TimeoutException e) {
                logger.error("Task timed out.",e);
            }
        }
    }

    private JMXConnectionUtil createJMXConnector(Server server) {
        JMXConnectionUtil jmxConnector = null;
        if(Strings.isNullOrEmpty(server.getServiceUrl())) {
            jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getHost(), server.getPort(), server.getUsername(), server.getPassword()));
        }
        else {
            jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getServiceUrl(), server.getUsername(), server.getPassword()));
        }
        return jmxConnector;
    }

    private String logVersion() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        return msg;
    }

    public static String getImplementationVersion() {
        return CoherenceMonitor.class.getPackage().getImplementationTitle();
    }


}
