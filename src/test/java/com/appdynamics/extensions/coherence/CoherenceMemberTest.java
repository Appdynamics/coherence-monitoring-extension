/*
 *  Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.coherence;


import org.junit.Assert;
import org.junit.Test;

public class CoherenceMemberTest {

    @Test
    public void whenValid_thenSuccessfullyInitializeMembers(){
        String text = "Member(Id=8, Timestamp=2014-08-12 20:33:43.611, Address=192.168.57.102:8090, MachineId=50042, Location=site:,machine:prod001,process:21664,member:C1, Role=CoherenceConsole)";
        CoherenceMember cm = new CoherenceMember(text);
        Assert.assertTrue(cm.getId().equals("8"));
        Assert.assertTrue(cm.getMachineName().equals("prod001"));
        Assert.assertTrue(cm.getMemberName().equals("C1"));
    }

    @Test
    public void whenNotValid_thenCannotInitializeMembers(){
        String text = "Member(Id=1, Timestamp=2014-08-11 18:33:10.41, Address=192.168.57.102:8088, MachineId=48026, Location=site:,machine:myubuntu,process:18386, Role=CoherenceServer)";
        CoherenceMember cm = new CoherenceMember(text);
        Assert.assertTrue(cm.getMemberName() == null);
    }
}
