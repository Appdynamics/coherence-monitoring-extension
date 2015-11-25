package com.appdynamics.extensions.coherence;


import com.singularity.ee.agent.systemagent.api.MetricWriter;

public class MetricAggregator {

    protected long sum;
    protected long count;
    protected String clusterAggregationInMA;
    protected String aggregationType;
    protected String timeRollup;
    protected String clusterRollup;

    public MetricAggregator(String clusterAggregationInMA,String aggregationType,String timeRollup,String clusterRollup){
        this.clusterAggregationInMA = clusterAggregationInMA;
        this.aggregationType = aggregationType;
        this.timeRollup = timeRollup;
        this.clusterRollup = clusterRollup;
    }

    public long aggregate(){
        if(clusterAggregationInMA.equalsIgnoreCase(MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE)){
            return (count == 0) ? count : sum/count;
        }
        return sum;
    }

    public void report(long val){
        count++;
        sum += val;
    }

}
