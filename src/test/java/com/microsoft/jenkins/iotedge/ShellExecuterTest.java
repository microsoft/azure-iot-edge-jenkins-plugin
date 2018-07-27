/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge;

import org.junit.Assert;
import org.junit.Test;

public class ShellExecuterTest {
    @Test
    public void testAzVersion() throws Exception {
        ShellExecuter shellExecuter = new ShellExecuter();
        String version = shellExecuter.getVersion();
        Assert.assertTrue(version.contains("azure-cli"));
    }
}