/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.AccessKeyType;
import com.microsoft.azure.management.containerregistry.Registries;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import com.microsoft.azure.management.containerregistry.implementation.ContainerRegistryManager;
import com.microsoft.jenkins.iotedge.model.AzureCloudException;
import com.microsoft.jenkins.iotedge.model.DockerCredential;
import com.microsoft.jenkins.iotedge.util.AzureUtils;
import com.microsoft.jenkins.iotedge.util.Constants;
import com.microsoft.jenkins.iotedge.util.Env;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EdgePushBuilder extends BaseBuilder {

    public String getDockerRegistryType() {
        return dockerRegistryType;
    }

    @DataBoundSetter
    public void setDockerRegistryType(String dockerRegistryType) {
        this.dockerRegistryType = dockerRegistryType;
    }

    public DockerRegistryEndpoint getDockerRegistryEndpoint() {
        return dockerRegistryEndpoint;
    }

    @DataBoundSetter
    public void setDockerRegistryEndpoint(DockerRegistryEndpoint dockerRegistryEndpoint) {
        this.dockerRegistryEndpoint = dockerRegistryEndpoint;
    }

    public String getAcrName() {
        return acrName;
    }

    @DataBoundSetter
    public void setAcrName(String acrName) {
        this.acrName = acrName;
    }

    public String getBypassModules() {
        return bypassModules;
    }

    @DataBoundSetter
    public void setBypassModules(String bypassModules) {
        this.bypassModules = bypassModules;
    }

    private String bypassModules = DescriptorImpl.defaultModulesToBuild;

    private String dockerRegistryType;

    private String acrName;

    private DockerRegistryEndpoint dockerRegistryEndpoint;

    @DataBoundConstructor
    public EdgePushBuilder(final String azureCredentialsId,
                           final String resourceGroup,
                           final String rootPath) {
        super(azureCredentialsId, resourceGroup, rootPath);
        this.dockerRegistryType = Constants.DOCKER_REGISTRY_TYPE_ACR;
    }

    public EdgePushBuilder(final String azureCredentialsId,
                           final String resourceGroup) {
        super(azureCredentialsId, resourceGroup);
        this.dockerRegistryType = Constants.DOCKER_REGISTRY_TYPE_ACR;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        boolean isAcr = dockerRegistryType.equals(Constants.DOCKER_REGISTRY_TYPE_ACR);
        String credentialId = null;
        listener.getLogger().println(ContainerRegistryManager.class.getPackage().getSpecificationVersion());
        String url = "", username = "", password = "";

        if (isAcr) {
            credentialId = getAzureCredentialsId();
            final Azure azureClient = AzureUtils.buildClient(run.getParent(), credentialId);
            Registries rs = azureClient.containerRegistries();
            Registry r = rs.getByResourceGroup(getResourceGroup(), acrName);
            RegistryCredentials rc = r.getCredentials();
            username = rc.username();
            url = r.loginServerUrl();
            password = rc.accessKeys().get(AccessKeyType.PRIMARY);
        } else {
            url = dockerRegistryEndpoint.getUrl();
            credentialId = dockerRegistryEndpoint.getCredentialsId();
            StandardUsernamePasswordCredentials credential = CredentialsProvider.findCredentialById(credentialId, StandardUsernamePasswordCredentials.class, run);
            if (credential != null) {
                username = credential.getUsername();
                password = credential.getPassword().getPlainText();
            }
        }

        // Generate .env file for iotedgedev use
        writeEnvFile(Paths.get(workspace.getRemote(), getRootPath(), Constants.IOTEDGEDEV_ENV_FILENAME).toString(), url, bypassModules);

        // Save docker credential id to a file
        ObjectMapper mapper = new ObjectMapper();
        Map<String, DockerCredential> credentialMap = new HashMap<>();
        File credentialFile = new File(Paths.get(workspace.getRemote(), getRootPath(), Constants.DOCKER_CREDENTIAL_FILENAME).toString());
        if (credentialFile.exists() && !credentialFile.isDirectory()) {
            credentialMap = mapper.readValue(credentialFile, new TypeReference<Map<String, DockerCredential>>() {
            });
        }
        DockerCredential dockerCredential = new DockerCredential(credentialId, isAcr, isAcr ? acrName : url);
        credentialMap.put(url, dockerCredential);
        mapper.writeValue(credentialFile, credentialMap);

        ShellExecuter executer = new ShellExecuter(launcher, listener, new File(workspace.getRemote(), getRootPath()));
        try {
            Map<String, String> envs = new HashMap<>();
            envs.put(Constants.IOTEDGEDEV_ENV_REGISTRY_USERNAME, username);
            envs.put(Constants.IOTEDGEDEV_ENV_REGISTRY_PASSWORD, password);
            executer.executeAZ("iotedgedev.exe push", true, envs);
        } catch (AzureCloudException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        }
    }

    @Extension
    @Symbol("azureIoTEdgePush")
    public static final class DescriptorImpl extends BaseBuilder.DescriptorImpl {
        public FormValidation doCheckName(@QueryParameter String value, @QueryParameter boolean useFrench)
                throws IOException, ServletException {

            return FormValidation.ok();
        }

        public DockerRegistryEndpoint.DescriptorImpl getDockerRegistryEndpointDescriptor() {
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                return (DockerRegistryEndpoint.DescriptorImpl)
                        jenkins.getDescriptor(DockerRegistryEndpoint.class);
            } else {
                return null;
            }
        }

        public ListBoxModel doFillAzureCredentialsIdItems(@AncestorInPath final Item owner) {
            return listAzureCredentialsIdItems(owner);
        }

        public ListBoxModel doFillResourceGroupItems(@AncestorInPath Item owner,
                                                     @QueryParameter String azureCredentialsId) {
            return listResourceGroupItems(owner, azureCredentialsId);
        }

        public ListBoxModel doFillAcrNameItems(@AncestorInPath Item owner,
                                               @QueryParameter String azureCredentialsId,
                                               @QueryParameter String resourceGroup) {
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                return listAcrNameItems(owner, azureCredentialsId, resourceGroup);
            } else {
                return new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            }
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Azure IoT Edge Build and Push";
        }

    }

}
