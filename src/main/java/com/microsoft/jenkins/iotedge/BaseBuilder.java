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
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.iotedge.util.AzureUtils;
import com.microsoft.jenkins.iotedge.util.Constants;
import com.microsoft.jenkins.iotedge.util.Env;
import com.microsoft.jenkins.iotedge.util.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.*;

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
            writer = new PrintWriter(path, Constants.CHARSET_UTF_8);
            writer.println(Env.EnvString);
            writer.println(Constants.IOTEDGEDEV_ENV_REGISTRY_SERVER + "=\"" + url + "\"");
            writer.println(Constants.IOTEDGEDEV_ENV_ACTIVE_MODULES + "=\"" + bypassModules + "\"");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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

        protected ListBoxModel listDeviceIdItems(Item owner, String azureCredentialsId,
                                                   String resourceGroup, String iothubName) {
            final ListBoxModel model = new ListBoxModel(new ListBoxModel.Option(Constants.EMPTY_SELECTION, ""));

            if (StringUtils.isNotBlank(azureCredentialsId)) {
                AzureCredentials.ServicePrincipal servicePrincipal = AzureCredentials.getServicePrincipal(azureCredentialsId);
                String output = null;

                output = Util.executePost(String.format(Constants.REST_GET_TOKEN_URL, servicePrincipal.getTenant()),
                        String.format(Constants.REST_GET_TOKEN_BODY, servicePrincipal.getClientId(), Util.encodeURIComponent(servicePrincipal.getClientSecret())),
                        null, null);

                String accessToken = new JSONObject(output).getString("access_token");

                output = Util.executePost(String.format(Constants.REST_GET_IOT_KEY_URL, servicePrincipal.getSubscriptionId(), Util.encodeURIComponent(resourceGroup), Util.encodeURIComponent(iothubName)),
                        "",
                        "Bearer " + accessToken, "application/json");
                String key = "";
                JSONArray keys = new JSONObject(output).getJSONArray("value");
                for (int i = 0; i < keys.length(); i++) {
                    JSONObject obj = keys.getJSONObject(i);
                    if (obj.getString("keyName").equals("iothubowner")) {
                        key = obj.getString("primaryKey");
                        break;
                    }
                }

                output = Util.executePost(String.format(Constants.REST_GET_DEVICES_URL, Util.encodeURIComponent(iothubName)),
                        Constants.REST_GET_DEVICES_BODY,
                        Util.getSharedAccessToken(String.format(Constants.IOT_HUB_URL, Util.encodeURIComponent(iothubName)),
                                key,
                                "iothubowner",
                                Constants.SAS_TOKEN_MINUTES),
                        "application/json");

                JSONArray deviceArr = new JSONArray(output);
                for (int i = 0; i < deviceArr.length(); i++) {
                    model.add(deviceArr.getJSONObject(i).getString("deviceId"));
                }

            }

            return model;
        }

    }
}
