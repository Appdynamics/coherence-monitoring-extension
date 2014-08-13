package com.appdynamics.extensions.coherence;

import com.google.common.base.Splitter;

public class CoherenceMember {

    public static final String ID = "Id";
    public static final String MACHINE = "machine";
    public static final String MEMBER = "member";
    private String id;
    private String machineName;
    private String memberName;

    public CoherenceMember(String text){
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
}
