package com.appdynamics.extensions.coherence;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class DictionaryGenerator {

    public static List<Map> createIncludeDictionaryWithDefaults() {
        List<Map> dictionary = Lists.newArrayList();
        Map metric1 = Maps.newLinkedHashMap();
        metric1.put("CacheHits","CacheHitsAlias");
        dictionary.add(metric1);
        Map metric2 = Maps.newLinkedHashMap();
        metric2.put("TotalGets","TotalGetsAlias");
        dictionary.add(metric2);
        Map metric3 = Maps.newLinkedHashMap();
        metric3.put("Size","SizeAlias");
        dictionary.add(metric3);
        Map metric4 = Maps.newLinkedHashMap();
        metric4.put("EvictionCount","EvictionCountAlias");
        dictionary.add(metric4);
        return dictionary;
    }

    public static List<Map> createIncludeDictionaryWithLocalOverrides() {
        List<Map> dictionary = Lists.newArrayList();
        Map metric1 = Maps.newLinkedHashMap();
        metric1.put("CacheHits","CacheHitsAlias");
        metric1.put(ConfigConstants.METRIC_TYPE,"SUM SUM COLLECTIVE");
        dictionary.add(metric1);
        Map metric2 = Maps.newLinkedHashMap();
        metric2.put("TotalGets","TotalGetsAlias");
        metric2.put(ConfigConstants.AGGREGATION,"true");
        dictionary.add(metric2);
        Map metric3 = Maps.newLinkedHashMap();
        metric3.put("Size","SizeAlias");
        dictionary.add(metric3);
        Map metric4 = Maps.newLinkedHashMap();
        metric4.put("EvictionCount","EvictionCountAlias");
        dictionary.add(metric4);
        return dictionary;
    }



    public static List<String> createExcludeDictionary() {
        return Lists.newArrayList("CacheHits","TotalGets","OptimizedQueryAverageMillis");
    }
}
