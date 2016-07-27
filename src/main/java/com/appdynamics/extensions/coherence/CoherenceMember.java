package com.appdynamics.extensions.coherence;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;


public class CoherenceMember {

    private static final String ID = "Id";
    private static final String MACHINE = "machine";
    private static final String MEMBER = "member";
    private String id;
    private String machineName;
    private String memberName;


    CoherenceMember(String text){
        parseText(text);
    }

    private void parseText(String text) {
        String subText = text.substring(text.indexOf('(') + 1,text.indexOf(')'));
        Iterable<String> keyValues = Splitter.on(',').split(subText);
        for(String keyVal : keyValues){
            if(keyVal.startsWith(ID)){
                setId(keyVal.substring(keyVal.indexOf("=") + 1));
            }
            else if(keyVal.startsWith(MACHINE)){
               setMachineName(keyVal.substring(keyVal.indexOf(":") + 1));
            }
            else if(keyVal.startsWith(MEMBER)){
                setMemberName(keyVal.substring(keyVal.indexOf(":") + 1));
            }
        }
    }

    public String getMemberInfo() {
        String memberInfo = "";
        memberInfo = memberInfo.concat(Strings.isNullOrEmpty(this.getMachineName()) ? "" : this.getMachineName());
        memberInfo = memberInfo.concat(Strings.isNullOrEmpty(this.getMemberName()) ? "" : this.getMemberName());
        return memberInfo;
    }

    String getId() {
        return id;
    }

    private void setId(String id) {
        this.id = id;
    }

    String getMachineName() {
        return machineName;
    }

    private void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    String getMemberName() {
        return memberName;
    }

    private void setMemberName(String memberName) {
        this.memberName = memberName;
    }


}
