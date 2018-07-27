package com.microsoft.jenkins.iotedge;

import java.util.regex.Pattern;

/**
 * Created by zhqqi on 7/27/2018.
 */
public class Util {
    public static final Pattern targetConditionPattern = Pattern.compile("^(deviceId|tags\\..+|properties\\.reported\\..+).*=.+$");
    public static final Pattern deploymentIdPattern = Pattern.compile("^[a-z0-9-:+%_#*?!(),=@;']+$");
    public static final Pattern priorityPattern = Pattern.compile("^\\d+$");

    public static boolean isValidTargetCondition(String targetCondition) {
        return targetConditionPattern.matcher(targetCondition).find();
    }

    public static boolean isValidDeploymentId(String deploymentId) {
        if(deploymentId.length() > 128) return false;
        return deploymentIdPattern.matcher(deploymentId).find();
    }

    public static boolean isValidPriority(String priority) {
        return priorityPattern.matcher(priority).find();
    }
}
