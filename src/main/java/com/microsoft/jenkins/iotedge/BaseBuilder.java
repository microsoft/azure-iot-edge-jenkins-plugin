package com.microsoft.jenkins.iotedge;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.appservice.implementation.WebAppsInner;
import com.microsoft.azure.management.containerregistry.Registries;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.resources.GenericResource;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.model.HasInner;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.iotedge.model.AzureCloudException;
import com.microsoft.jenkins.iotedge.model.AzureCredentialCache;
import com.microsoft.jenkins.iotedge.model.AzureCredentialsValidationException;
import com.microsoft.jenkins.iotedge.util.AzureUtils;
import com.microsoft.jenkins.iotedge.util.Constants;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhqqi on 7/5/2018.
 */
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
    private String rootPath;

    protected BaseBuilder(String azureCredentialsId, String resourceGroup, String rootPath) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroup = resourceGroup;
        this.rootPath = rootPath;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    protected static class DescriptorImpl extends BuildStepDescriptor<Builder> {

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
                    if(resource.resourceProviderNamespace().equals("Microsoft.Devices") && resource.resourceType().equals("IotHubs")) {
                        model.add(resource.name());
                    }
                }
            }

//            if (StringUtils.isNotBlank(azureCredentialsId)) {
//                ShellExecuter executer = new ShellExecuter();
//                AzureCredentials.ServicePrincipal servicePrincipal = AzureCredentials.getServicePrincipal(azureCredentialsId);
//                AzureCredentialCache credentialCache = new AzureCredentialCache(servicePrincipal);
//
//                try {
//                    executer.login(credentialCache);
//                    String iothubsResult = executer.executeAZ("az iot hub list -g \""+ resourceGroup+"\"", false);
//                    JSONArray iothubsJson = new JSONArray(iothubsResult);
//                    for(int i=0;i<iothubsJson.length();i++) {
//                        JSONObject iothub = iothubsJson.getJSONObject(i);
//                        model.add(iothub.getString("name"));
//                    }
//                } catch (AzureCredentialsValidationException e) {
//                    e.printStackTrace();
//                } catch (AzureCloudException e) {
//                    e.printStackTrace();
//                }
//            }
            return model;
        }
    }
}