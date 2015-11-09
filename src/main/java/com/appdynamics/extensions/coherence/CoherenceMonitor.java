package com.appdynamics.extensions.coherence;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.coherence.config.Configuration;
import com.appdynamics.extensions.coherence.config.Server;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.appdynamics.extensions.file.FileLoader;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.*;

import static com.appdynamics.TaskInputArgs.ENCRYPTION_KEY;
import static com.appdynamics.TaskInputArgs.PASSWORD_ENCRYPTED;

/**
 * This extension will extract out metrics from Coherence through the JMX protocol.
 */
public class CoherenceMonitor extends AManagedMonitor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CoherenceMonitor.class);
    public static final String CONFIG_ARG = "config-file";

    private ExecutorService executorService;
    private int executorServiceSize;
    private volatile boolean initialized;
    private Configuration config;

    public CoherenceMonitor() {
        System.out.println(logVersion());
    }


    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext out) throws TaskExecutionException {
        logVersion();
        try {
            initialize(taskArgs);
            //parallel execution for each server.
            runConcurrentTasks();
            logger.info("Coherence monitor run completed successfully.");
            return new TaskOutput("Coherence monitor run completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Metrics collection failed", e);
        }
        throw new TaskExecutionException("Coherence monitoring run completed with failures.");
    }

    private void initialize(Map<String, String> taskArgs) {
        if(!initialized){
            //read the config.
            final String configFilePath = taskArgs.get(CONFIG_ARG);
            File configFile = PathResolver.getFile(configFilePath, AManagedMonitor.class);
            if(configFile != null && configFile.exists()){
                FileLoader.load(new FileLoader.Listener() {
                    public void load(File file) {
                        String path = file.getAbsolutePath();
                        try {
                            if (path.contains(configFilePath)) {
                                logger.info("The file [{}] has changed, reloading the config", file.getAbsolutePath());
                                reloadConfig(file);
                            } else {
                                logger.warn("Unknown file [{}] changed, ignoring", file.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            logger.error("Exception while reloading the file " + file.getAbsolutePath(), e);
                        }
                    }
                }, configFilePath);
            }
            else{
                logger.error("Config file is not found.The config file path {} is resolved to {}",
                        taskArgs.get(CONFIG_ARG), configFile != null ? configFile.getAbsolutePath() : null);
            }
            initialized = true;
        }
    }

    private void reloadConfig(File file) {
        config = YmlReader.readFromFile(file, Configuration.class);
        if (config != null) {
            int numOfThreads = config.getNumberOfThreads();
            if (executorService == null) {
                executorService = createThreadPool(numOfThreads);
                logger.info("Initializing the ThreadPool with size {}", config.getNumberOfThreads());
            }
            else if (numOfThreads != executorServiceSize) {
                logger.info("The ThreadPool size has been updated from {} -> {}", executorServiceSize, numOfThreads);
                executorService.shutdown();
                executorService = createThreadPool(numOfThreads);
            }
            executorServiceSize = numOfThreads;
            //decrypt password
            if(config.getEncryptionKey() != null){
                for(Server server : config.getServers()) {
                    Map cryptoMap = Maps.newHashMap();
                    cryptoMap.put(PASSWORD_ENCRYPTED,server.getEncryptedPassword());
                    cryptoMap.put(ENCRYPTION_KEY,config.getEncryptionKey());
                    server.setPassword(CryptoUtil.getPassword(cryptoMap));
                }
            }
        }
        else {
            throw new IllegalArgumentException("The config cannot be initialized from the file " + file.getAbsolutePath());
        }
    }


    private ExecutorService createThreadPool(int numOfThreads) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Coherence-Task-Thread-%d")
                .build();
        return Executors.newFixedThreadPool(numOfThreads,
                threadFactory);
    }


    private void runConcurrentTasks() {
        if (config != null) {
            for (Server server : config.getServers()) {
                try {
                    //passing the context to the task.
                    CoherenceMonitorTask task = createTask(server);
                    executorService.execute(task);
                }
                catch (IOException e){
                    logger.error("Unable to create JMX connection for {}",server.getDisplayName(),e);
                }
            }
        }
    }

    private CoherenceMonitorTask createTask(Server server) throws IOException {
        return new CoherenceMonitorTask.Builder()
                .metricPrefix(config.getMetricPrefix())
                .metricWriter(this)
                .serviceURL(createJMXServiceUrl(server))
                .server(server)
                .mbeans(config.getMbeans())
                .build();
    }



    private JMXServiceURL createJMXServiceUrl(Server server) throws MalformedURLException {
        String url;
        if(Strings.isNullOrEmpty(server.getServiceUrl())) {
            url = "service:jmx:rmi:///jndi/rmi://" + server.getHost() + ":" + server.getPort() + "/jmxrmi";
        }
        else{
            url = server.getServiceUrl();
        }
        return new JMXServiceURL(url);
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
