package com.appdynamics.extensions.coherence;

/**
 * Created by mithun.banerjee on 5/24/16.
 * The High Availability (HA) status for this service. The value of MACHINE-SAFE means that all the cluster nodes
 * running on any given machine could be stopped at once without data loss. The value of NODE-SAFE means that any
 * cluster node could be stopped without data loss. The value of ENDANGERED indicates that abnormal termination
 * of any cluster node that runs this service may cause data loss.
 * <p/>
 * https://docs.oracle.com/cd/E15357_01/coh.360/e15723/appendix_mbean.htm#COHDG5446
 */
public enum CoherenceStatusHAEnum {


    MACHINE_SAFE("MACHINE-SAFE", 3),  //MACHINE-SAFE
    NODESAFE("NODE-SAFE", 2),  //NODE-SAFE
    ENDANGERED("ENDANGERED", 1);

    private int statusHACode;
    private String statusHA;

    CoherenceStatusHAEnum(String statusHA, int statusHACode) {

        this.statusHACode = statusHACode;
        this.statusHA = statusHA;
    }

    public static int getStatusHACode(String varStatusHA) {

        for (CoherenceStatusHAEnum coherenceStatusHAEnum : CoherenceStatusHAEnum.values()) {
            if (coherenceStatusHAEnum.getStatusHA().equalsIgnoreCase(varStatusHA)) {
                return coherenceStatusHAEnum.getStatusHACode();
            }
        }
        throw new IllegalArgumentException("Illegal Argument for statusHA value is:"+varStatusHA);
    }

    public int getStatusHACode() {
        return this.statusHACode;
    }

    public String getStatusHA() {
        return this.statusHA;
    }


}



