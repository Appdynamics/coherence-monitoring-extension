package com.appdynamics.extensions.coherence;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class JMXDataProviderUtil {

    static Set<ObjectInstance> getSetWithClusterObjInstance() throws MalformedObjectNameException {
        Set<ObjectInstance> instanceSet = Sets.newHashSet();
        instanceSet.add(new ObjectInstance("Coherence:type=Cluster","com.tangosol.coherence.component.manageable.modelAdapter.ClusterMBean"));
        return instanceSet;
    }

    public static Set<ObjectInstance> getSetWithNodeObjInstances() throws MalformedObjectNameException {
        Set<ObjectInstance> instanceSet = Sets.newHashSet();
        instanceSet.add(new ObjectInstance("Coherence:type=Node,nodeId=1","com.tangosol.coherence.component.manageable.modelAdapter.ClusterNodeMBean"));
        instanceSet.add(new ObjectInstance("Coherence:type=Node,nodeId=2","com.tangosol.coherence.component.manageable.modelAdapter.ClusterNodeMBean"));
        //instanceSet.add(new ObjectInstance("Coherence:type=PartitionAssignment,service=DistributedCache,responsibility=DistributionCoordinator","com.tangosol.net.partition.SimpleAssignmentStrategy"));
        //instanceSet.add(new ObjectInstance("Coherence:type=Service,name=InvocationService,nodeId=1","com.tangosol.coherence.component.manageable.modelAdapter.ServiceMBean"));
        return instanceSet;
    }

    static AttributeList getAttributes() {
        AttributeList list = new AttributeList();
        list.add(new Attribute("Members",new String[]{"Member(Id=8, Timestamp=2014-08-12 20:33:43.611, Address=192.168.57.102:8090, MachineId=50042, Location=site:,machine:prod001,process:21664,member:C1, Role=CoherenceConsole)",
        "Member(Id=1, Timestamp=2016-06-27 15:18:36.804, Address=10.0.2.15:8088, MachineId=2063, Location=site:,process:12756, Role=CoherenceServer)"}));
        list.add(new Attribute("CacheHits",34));
        list.add(new Attribute("TotalGets",178));
        list.add(new Attribute("ThreadCount",23));
        list.add(new Attribute("ThreadIdleCount",56));
        list.add(new Attribute("FreePhysicalMemorySize",34));
        list.add(new Attribute("MemoryMaxMB",56));
        list.add(new Attribute("StatusHA","ENDANGERED"));
        return list;
    }

    static AttributeList getAttributesForConvertTest() {
        AttributeList list = new AttributeList();
        list.add(new Attribute("TaskBacklog",56));
        list.add(new Attribute("StatusHA","ENDANGERED"));
        list.add(new Attribute("Members",new String[]{"Member(Id=1, Timestamp=2014-08-12 20:33:43.611, Address=192.168.57.102:8090, MachineId=50042, Location=site:,machine:prod001,process:21664,member:C1, Role=CoherenceConsole)"}));
        return list;
    }

    static AttributeList getAttributes(List<String> attrs) {
        AttributeList list = new AttributeList();
        Random rand = new Random();
        for(String attr : attrs){
            list.add(new Attribute(attr,rand.nextInt(100)));
        }
        return list;
    }

    static List<String> getAttributeNames(){
        return Lists.newArrayList("CacheHits","TotalGets","ThreadCount","ThreadIdleCount","FreePhysicalMemorySize","MemoryMaxMB","StatusHA","Members");
    }
}
