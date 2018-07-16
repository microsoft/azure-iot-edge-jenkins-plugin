package com.microsoft.jenkins.iotedge;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.AccessKeyType;
import com.microsoft.azure.management.containerregistry.Registries;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import com.microsoft.azure.management.containerregistry.implementation.ContainerRegistryManager;
import com.microsoft.azure.management.containerregistry.implementation.RegistryImpl;
import com.microsoft.jenkins.iotedge.model.AzureCloudException;
import com.microsoft.jenkins.iotedge.util.AzureUtils;
import com.microsoft.jenkins.iotedge.util.Constants;
import com.microsoft.jenkins.iotedge.util.Env;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
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

    public String getModulesToBuild() {
        return modulesToBuild;
    }

    @DataBoundSetter
    public void setModulesToBuild(String modulesToBuild) {
        this.modulesToBuild = modulesToBuild;
    }

    private String modulesToBuild;

    private String dockerRegistryType;

    private String acrName;

    private DockerRegistryEndpoint dockerRegistryEndpoint;

    @DataBoundConstructor
    public EdgePushBuilder(final String azureCredentialsId,
                           final String resourceGroup) {
        super(azureCredentialsId, resourceGroup);
        this.dockerRegistryType = Constants.DOCKER_REGISTRY_TYPE_ACR;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        boolean isAcr = dockerRegistryType.equals(Constants.DOCKER_REGISTRY_TYPE_ACR);
        listener.getLogger().println(ContainerRegistryManager.class.getPackage().getSpecificationVersion());
        listener.getLogger().println("Is ACR? " + isAcr);
        String url="", username="", password="";
        if (isAcr) {
            final Azure azureClient = AzureUtils.buildClient(run.getParent(), getAzureCredentialsId());
            Registries rs = azureClient.containerRegistries();
            Registry r = rs.getByResourceGroup(getResourceGroup(), acrName);
            RegistryCredentials rc2 = rs.getCredentials(getResourceGroup(), acrName);
            RegistryImpl impl;
            listener.getLogger().println(r.adminUserEnabled());
            listener.getLogger().println(r.loginServerUrl());
            RegistryCredentials rc = r.getCredentials();
            username = rc.username();
            url = r.loginServerUrl();
            listener.getLogger().println(url);
            listener.getLogger().println(username);
            for (Map.Entry<AccessKeyType, String> entry : rc.accessKeys().entrySet())
            {
                listener.getLogger().println(entry.getKey().PRIMARY + ":" + entry.getKey().SECONDARY + "/" + entry.getValue());
            }
        } else {
            url = dockerRegistryEndpoint.getUrl();
            String credentialId = dockerRegistryEndpoint.getCredentialsId();
            StandardUsernamePasswordCredentials credential = CredentialsProvider.findCredentialById(credentialId, StandardUsernamePasswordCredentials.class, run);
            if(credential != null) {
                username = credential.getUsername();
                password = credential.getPassword().getPlainText();
                listener.getLogger().println(url);
                listener.getLogger().println(username);
                listener.getLogger().println(password);
            }
        }
        listener.getLogger().println(workspace.getRemote());
        PrintWriter writer = new PrintWriter(Paths.get(workspace.getRemote(), Constants.IOTEDGEDEV_ENV_FILENAME).toString(), "UTF-8");
        writer.println(Env.EnvString);
        writer.println(Constants.IOTEDGEDEV_ENV_REGISTRY_SERVER + "=\""+url+"\"");
        writer.println(Constants.IOTEDGEDEV_ENV_REGISTRY_USERNAME + "=\""+username+"\"");
        writer.println(Constants.IOTEDGEDEV_ENV_REGISTRY_PASSWORD + "=\""+password+"\"");
        writer.println(Constants.IOTEDGEDEV_ENV_ACTIVE_MODULES + "=\""+modulesToBuild+"\"");
        writer.close();

        ShellExecuter executer = new ShellExecuter(listener.getLogger(), new File(workspace.getRemote()));
        try {
            executer.executeAZ("iotedgedev push", true);
        } catch (AzureCloudException e) {
            e.printStackTrace();
            listener.getLogger().println(e.getMessage());
            run.setResult(Result.FAILURE);
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
            return "Azure IoT Edge Push";
        }

    }

}
