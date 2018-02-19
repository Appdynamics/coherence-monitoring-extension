/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence;


import com.google.common.collect.Maps;

import javax.management.*;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MemberIdentityMapper {

    static final String MEMBERS_ATTRIB = "Members";
    static final String COHERENCE_TYPE_CLUSTER = "Coherence:type=Cluster";

    public Map<String,CoherenceMember> map(JMXConnectionAdapter jmxAdapter, JMXConnector jmxConnection) throws MalformedObjectNameException, IOException, InstanceNotFoundException, ReflectionException {
        java.util.Map<String,CoherenceMember> membersMap = Maps.newHashMap();
        Set<ObjectInstance> objectInstances = jmxAdapter.queryMBeans(jmxConnection, ObjectName.getInstance(COHERENCE_TYPE_CLUSTER));
        for(ObjectInstance instance : objectInstances){
            List<Attribute> attributes = jmxAdapter.getAttributes(jmxConnection,instance.getObjectName(), new String[]{MEMBERS_ATTRIB});
            populateNodeMemberMap(jmxAdapter,attributes, membersMap);
        }
        return membersMap;
    }

    private void populateNodeMemberMap(JMXConnectionAdapter jmxAdapter,List<Attribute> attributes, java.util.Map<String,CoherenceMember> membersMap) {
        Object members = null;
        for(Attribute attr : attributes){
            if(jmxAdapter.matchAttributeName(attr, MEMBERS_ATTRIB)){
                members = attr.getValue();
                break;
            }
        }
        if(members != null ){
            String[] membersArr = (String[]) members;
            for(String member : membersArr){
                CoherenceMember node = new CoherenceMember(member);
                if(node.getId() != null){
                    membersMap.put(node.getId(),node);
                }
            }
        }
    }
}
