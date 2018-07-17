package com.microsoft.jenkins.iotedge;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.appservice.implementation.WebAppsInner;
import com.microsoft.azure.management.containerregistry.Registries;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.model.HasInner;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.jenkins.azurecommons.command.BaseCommandContext;
import com.microsoft.jenkins.iotedge.util.AzureUtils;
import com.microsoft.jenkins.iotedge.util.Constants;
import com.sun.prism.impl.Disposer;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Created by zhqqi on 7/5/2018.
 */
public abstract class BaseRecorder extends Recorder implements SimpleBuildStep {
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

    private String azureCredentialsId;
    private String resourceGroup;

    protected BaseRecorder(String azureCredentialsId, String resourceGroup) {
        this.azureCredentialsId = azureCredentialsId;
        this.resourceGroup = resourceGroup;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    protected static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

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
            // list all app services
            // https://github.com/Azure/azure-sdk-for-java/issues/1762
            if (StringUtils.isNotBlank(azureCredentialsId)) {
                final Azure azureClient = AzureUtils.buildClient(owner, azureCredentialsId);
                for (final Registry registry : azureClient.containerRegistries().listByResourceGroup(resourceGroup)) {
                    model.add(registry.name());
                }
            }
            return model;
        }
    }
}