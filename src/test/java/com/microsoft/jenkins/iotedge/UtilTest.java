package com.microsoft.jenkins.iotedge;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by zhqqi on 7/27/2018.
 */
public class UtilTest {

    @Test
    public void targetConditionValidationTest() {
        Assert.assertTrue(Util.isValidTargetCondition("tags.city='Shanghai'"));
        Assert.assertTrue(Util.isValidTargetCondition("properties.reported.lastStatus='200'"));
        Assert.assertTrue(Util.isValidTargetCondition("deviceId='device1'"));
        Assert.assertFalse(Util.isValidTargetCondition("properties.desired.lastStatus='200'"));
        Assert.assertFalse(Util.isValidTargetCondition("tags.='Shanghai'"));
        Assert.assertFalse(Util.isValidTargetCondition("tag.stauts='200'"));
    }

    @Test
    public void deploymentIdValidationTest() {
        Assert.assertTrue(Util.isValidDeploymentId("abc"));
        Assert.assertTrue(Util.isValidDeploymentId("a1b2"));
        Assert.assertTrue(Util.isValidDeploymentId("a-:+??=5"));
        Assert.assertFalse(Util.isValidDeploymentId(""));
        Assert.assertFalse(Util.isValidDeploymentId("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"));
        Assert.assertFalse(Util.isValidDeploymentId("ab$cd"));
    }
}
