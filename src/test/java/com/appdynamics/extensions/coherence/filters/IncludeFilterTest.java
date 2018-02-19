/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence.filters;

import com.appdynamics.extensions.coherence.DictionaryGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class IncludeFilterTest {

    @Test
    public void whenAttribsMatch_thenIncludeMetrics(){
        List dictionary = DictionaryGenerator.createIncludeDictionaryWithDefaults();
        List<String> metrics = Lists.newArrayList("OptimizedQueryAverageMillis","OpenFileDescriptorCount","TotalGets","CacheHits");
        IncludeFilter filter = new IncludeFilter(dictionary);
        Set<String> filteredSet = Sets.newHashSet();
        filter.apply(filteredSet,metrics);
        Assert.assertTrue(filteredSet.contains("CacheHits"));
        Assert.assertTrue(filteredSet.contains("TotalGets"));
        Assert.assertTrue(!filteredSet.contains("OpenFileDescriptorCount"));
        Assert.assertTrue(!filteredSet.contains("MemoryMaxMB"));
    }

    @Test
    public void whenNullDictionary_thenReturnUnchangedSet(){
        List<String> metrics = Lists.newArrayList("OptimizedQueryAverageMillis","OpenFileDescriptorCount");
        IncludeFilter filter = new IncludeFilter(null);
        Set<String> filteredSet = Sets.newHashSet();
        filter.apply(filteredSet,metrics);
        Assert.assertTrue(filteredSet.size() == 0);
    }

    @Test
    public void whenEmptyDictionary_thenReturnUnchangedSet(){
        List dictionary = Lists.newArrayList();
        List<String> metrics = Lists.newArrayList("OptimizedQueryAverageMillis","OpenFileDescriptorCount");
        IncludeFilter filter = new IncludeFilter(dictionary);
        Set<String> filteredSet = Sets.newHashSet("CacheHits");
        filter.apply(filteredSet,metrics);
        Assert.assertTrue(filteredSet.size() == 1);
    }
}
