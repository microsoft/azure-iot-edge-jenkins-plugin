/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge;

import com.microsoft.jenkins.iotedge.model.AzureCloudException;
import com.microsoft.jenkins.iotedge.model.AzureCredentialCache;
import com.microsoft.jenkins.iotedge.model.AzureCredentialsValidationException;
import hudson.Launcher.ProcStarter;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.*;
import java.util.Map;

public class ShellExecuter {

    public TaskListener listener;
    public File workspace;
    public Launcher launcher;

    public ShellExecuter(Launcher launcher, TaskListener listener, File workspace) {
        this.listener = listener;
        this.workspace = workspace;
        this.launcher = launcher;
    }

    public void login(AzureCredentialCache credentialsCache) throws AzureCredentialsValidationException {
        String command = "az login --service-principal -u " + credentialsCache.clientId + " -p " + credentialsCache.clientSecret + " --tenant " + credentialsCache.tenantId;
        try {
            executeAZ(command, false);
            command = "az account set -s " + credentialsCache.subscriptionId;
            executeAZ(command, false);
        } catch (AzureCloudException e) {
            throw new AzureCredentialsValidationException(e.getMessage());
        }
    }

    public String getVersion() throws AzureCloudException {
        String command = "az --version";
        ExitResult result = executeCommand(command);
        if (result.code == 0) {
            return result.output;
        }
        throw AzureCloudException.create("Azure CLI not found");
    }

    public String executeAZ(String command, Boolean printCommand) throws AzureCloudException {
        if (printCommand) {
            if (listener != null) listener.getLogger().println("Running: " + command);
        }
        ExitResult result = executeCommand(command);
        if (result.code == 0) {
            return result.output;
        }
        throw AzureCloudException.create(result.output);
    }

    private static class ExitResult {
        public String output;
        public int code;

        ExitResult(String output, int code) {
            this.output = output;
            this.code = code;
        }
    }

    private ExitResult executeCommand(String command) {
        ProcStarter ps = launcher.launch();
        int exitCode = -1;
        String output = null;
        if (File.pathSeparatorChar == ':') {
            command = "/bin/sh -c " + command;
        } else {
            command = "cmd /c " + command;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Proc p = launcher.launch(ps.cmdAsSingleString(command).envs(System.getenv()).pwd(workspace).stdout(baos));
            String line = "";
            exitCode = p.join();
            output = new String(baos.toByteArray(), "utf-8");
            listener.getLogger().println(output);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new ExitResult(output, exitCode);
    }
}
