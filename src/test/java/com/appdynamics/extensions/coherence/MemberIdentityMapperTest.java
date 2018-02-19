/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence;


import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.management.*;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MemberIdentityMapperTest {

    JMXConnectionAdapter adapter;

    @Mock
    JMXConnector jmxConnector;

    @Before
    public void setup() throws MalformedURLException {
        adapter = JMXConnectionAdapter.create(null,"localhost",1984,"username","password");
    }

    @Test
    public void whenClusterMBeanAndMembersAttribExists_thenPopulateMemberMap() throws IOException, MalformedObjectNameException, ReflectionException, InstanceNotFoundException {
        MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
        when(mBeanServerConnection.queryMBeans(ObjectName.getInstance(MemberIdentityMapper.COHERENCE_TYPE_CLUSTER),null)).thenReturn(JMXDataProviderUtil.getSetWithClusterObjInstance());
        AttributeList attributes = JMXDataProviderUtil.getAttributes();
        when(mBeanServerConnection.getAttributes(ObjectName.getInstance(MemberIdentityMapper.COHERENCE_TYPE_CLUSTER),new String[]{MemberIdentityMapper.MEMBERS_ATTRIB})).thenReturn(attributes);
        when(jmxConnector.getMBeanServerConnection()).thenReturn(mBeanServerConnection);
        MemberIdentityMapper mim = new MemberIdentityMapper();
        Map<String,CoherenceMember> map = mim.map(adapter,jmxConnector);
        Assert.assertTrue(map.size() != 0);
    }

    @Test
    public void whenClusterMBeanAbsent_thenPopulateEmptyMap() throws MalformedObjectNameException, IOException, ReflectionException, InstanceNotFoundException {
        MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
        when(mBeanServerConnection.queryMBeans(ObjectName.getInstance(MemberIdentityMapper.COHERENCE_TYPE_CLUSTER),null)).thenReturn(Sets.<ObjectInstance>newHashSet());
        AttributeList attributes = JMXDataProviderUtil.getAttributes();
        when(mBeanServerConnection.getAttributes(ObjectName.getInstance(MemberIdentityMapper.COHERENCE_TYPE_CLUSTER),new String[]{MemberIdentityMapper.MEMBERS_ATTRIB})).thenReturn(attributes);
        when(jmxConnector.getMBeanServerConnection()).thenReturn(mBeanServerConnection);
        MemberIdentityMapper mim = new MemberIdentityMapper();
        Map<String,CoherenceMember> map = mim.map(adapter,jmxConnector);
        Assert.assertTrue(map.isEmpty());
    }


    @Test
    public void whenMemberAttribAbsent_thenPopulateEmptyMap() throws MalformedObjectNameException, IOException, ReflectionException, InstanceNotFoundException {
        MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
        when(mBeanServerConnection.queryMBeans(ObjectName.getInstance(MemberIdentityMapper.COHERENCE_TYPE_CLUSTER),null)).thenReturn(JMXDataProviderUtil.getSetWithClusterObjInstance());
        when(mBeanServerConnection.getAttributes(ObjectName.getInstance(MemberIdentityMapper.COHERENCE_TYPE_CLUSTER),new String[]{MemberIdentityMapper.MEMBERS_ATTRIB})).thenReturn(new AttributeList());
        when(jmxConnector.getMBeanServerConnection()).thenReturn(mBeanServerConnection);
        MemberIdentityMapper mim = new MemberIdentityMapper();
        Map<String,CoherenceMember> map = mim.map(adapter,jmxConnector);
        Assert.assertTrue(map.isEmpty());
    }


}
