/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.resources.GenericResource;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsClient;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsClientFactory;
import com.microsoft.jenkins.iotedge.util.AzureUtils;
import com.microsoft.jenkins.iotedge.util.Constants;
import com.microsoft.jenkins.iotedge.util.Env;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public abstract class BaseBuilder extends Builder implements SimpleBuildStep {
    public String getAzureCredentialsId() {
        return azureCredentialsId;
    }

    @DataBoundSetter
    public void setAzureCredentialsId(String azureCredentialsId) {
        this.azureCredentialsId = azureCredentialsId;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    @DataBoundSetter
    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getRootPath() {
        return rootPath;
    }

    @DataBoundSetter
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }


    private String azureCredentialsId;
    private String resourceGroup;
    private String rootPath = DescriptorImpl.defaultRootPath;

    protected BaseBuilder(String azureCredentialsId, String resourceGroup, String rootPath) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroup = resourceGroup;
        this.rootPath = rootPath;
    }

    protected BaseBuilder(String azureCredentialsId, String resourceGroup) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroup = resourceGroup;
        this.rootPath = DescriptorImpl.defaultRootPath;
    }

    protected void writeEnvFile(String path, String url, String bypassModules) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(path, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        writer.println(Env.EnvString);
        writer.println(Constants.IOTEDGEDEV_ENV_REGISTRY_SERVER + "=\"" + url + "\"");
        writer.println(Constants.IOTEDGEDEV_ENV_ACTIVE_MODULES + "=\"" + bypassModules + "\"");
        writer.close();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    protected static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public static final String defaultRootPath = "./";
        public static final String defaultModulesToBuild = "";

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        protected ListBoxModel listAzureCredentialsIdItems(Item owner) {
            StandardListBoxModel model = new StandardListBoxModel();
            model.includeEmptyValue();
            model.includeAs(ACL.SYSTEM, owner, AzureBaseCredentials.class);
            return model;
        }

        /**
         * Leave for backward compatibility in azure-function plugin.
         *
         * @deprecated see {@link #listResourceGroupItems(Item, String)}.
         */
        @Deprecated
        protected ListBoxModel listResourceGroupItems(String azureCredentialsId) {
            return listResourceGroupItems(null, azureCredentialsId);
        }

        protected ListBoxModel listResourceGroupItems(Item owner,
                                                      String azureCredentialsId) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));
            // list all resource groups
            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = AzureUtils.buildClient(owner, azureCredentialsId);
                for (final ResourceGroup rg : azureClient.resourceGroups().list()) {
                    model.add(rg.name());
                }
            }
            return model;
        }

        protected ListBoxModel listAcrNameItems(Item owner, String azureCredentialsId,
                                                String resourceGroup) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));

            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = AzureUtils.buildClient(owner, azureCredentialsId);
                for (final Registry registry : azureClient.containerRegistries().listByResourceGroup(resourceGroup)) {
                    model.add(registry.name());
                }
            }
            return model;
        }

        protected ListBoxModel listIothubNameItems(Item owner, String azureCredentialsId,
                                                   String resourceGroup) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));

            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = AzureUtils.buildClient(owner, azureCredentialsId);
                for (final GenericResource resource : azureClient.genericResources().listByResourceGroup(resourceGroup)) {
                    if (resource.resourceProviderNamespace().equals("Microsoft.Devices") && resource.resourceType().equals("IotHubs")) {
                        model.add(resource.name());
                    }
                }
            }

            return model;
        }

        public FormValidation doCheckDeploymentId(@QueryParameter String value)
                throws IOException, ServletException {
            if (Util.isValidDeploymentId(value)) {
                return FormValidation.ok();
            } else {
                return FormValidation.error("Deployment ID is not in right format. Click help button to learn more.");
            }
        }
    }
}
