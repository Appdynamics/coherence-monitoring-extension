package com.appdynamics.extensions.coherence;


import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Test;

import java.util.Map;

public class CoherenceMonitorTest {

    public static final String CONFIG_ARG = "config-file";

    @Test
    public void testCassandraMonitorExtension() throws TaskExecutionException {
        CoherenceMonitor cassandraMonitor = new CoherenceMonitor();
        Map<String, String> taskArgs = Maps.newHashMap();
        taskArgs.put(CONFIG_ARG, "src/test/resources/conf/config.yml");
        cassandraMonitor.execute(taskArgs, null);
    }
}
