/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.iotedge.model.AzureCloudException;
import com.microsoft.jenkins.iotedge.model.AzureCredentialCache;
import com.microsoft.jenkins.iotedge.model.DockerCredential;
import com.microsoft.jenkins.iotedge.util.Constants;
import com.microsoft.jenkins.iotedge.util.Env;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EdgeDeployBuilder extends BaseBuilder {

    public String getIothubName() {
        return iothubName;
    }

    @DataBoundSetter
    public void setIothubName(String iothubName) {
        this.iothubName = iothubName;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    @DataBoundSetter
    public void setDeploymentType(String deploymentType) {
        this.deploymentType = deploymentType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    @DataBoundSetter
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTargetCondition() {
        return targetCondition;
    }

    @DataBoundSetter
    public void setTargetCondition(String targetCondition) {
        this.targetCondition = targetCondition;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    @DataBoundSetter
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getPriority() {
        return priority;
    }

    @DataBoundSetter
    public void setPriority(String priority) {
        this.priority = priority;
    }

    private String iothubName;
    private String deploymentType;
    private String deviceId;
    private String targetCondition;

    private String deploymentId;
    private String priority;

    @DataBoundConstructor
    public EdgeDeployBuilder(final String azureCredentialsId,
                             final String resourceGroup,
                             final String rootPath) {
        super(azureCredentialsId, resourceGroup, rootPath);
    }

    public EdgeDeployBuilder(final String azureCredentialsId,
                             final String resourceGroup) {
        super(azureCredentialsId, resourceGroup);
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

        // Get deployment.json using iotedgedev
        ShellExecuter executer = new ShellExecuter(listener.getLogger(), new File(workspace.getRemote(), getRootPath()));
        try {
            writeEnvFile(Paths.get(workspace.getRemote(), getRootPath(), Constants.IOTEDGEDEV_ENV_FILENAME).toString(), "","","","");
            executer.executeAZ("iotedgedev build", true);

        } catch (AzureCloudException e) {
            e.printStackTrace();
            listener.getLogger().println(e.getMessage());
            run.setResult(Result.FAILURE);
        }

        String deploymentJsonPath = Paths.get(workspace.getRemote(), getRootPath(), "config", Constants.EDGE_DEPLOYMENT_CONFIG_FILENAME).toString();

        // Modify deployment.json structure
        InputStream stream = new FileInputStream(deploymentJsonPath);
        JSONObject deploymentJson = new JSONObject(IOUtils.toString(stream, "UTF-8"));
        stream.close();

        // Get docker credential from temp file
        ObjectMapper mapper = new ObjectMapper();
        Map<String, DockerCredential> credentialMap = new HashMap<>();
        File credentialFile = new File(Paths.get(workspace.getRemote(), getRootPath(), Constants.DOCKER_CREDENTIAL_FILENAME).toString());
        if (credentialFile.exists() && !credentialFile.isDirectory()) {
            credentialMap = mapper.readValue(credentialFile, new TypeReference<Map<String, DockerCredential>>() {
            });
        } else {
            listener.getLogger().println("No docker credential cache");
        }
        credentialFile.delete();
        if (credentialMap.size() != 0) {
            JSONObject moduleContents = null;
            if(deploymentJson.has("modulesContent")) {
                moduleContents = deploymentJson.getJSONObject("modulesContent");
            }else if (deploymentJson.has("moduleContent")) {
                // Backward compatible for old pattern
                moduleContents = deploymentJson.getJSONObject("moduleContent");
            }else {
                throw new JSONException("moduleContent or modulesContent not found");
            }
            JSONObject settings = moduleContents
                    .getJSONObject("$edgeAgent")
                    .getJSONObject("properties.desired")
                    .getJSONObject("runtime")
                    .getJSONObject("settings");
            if (settings.has("registryCredentials")) {
                JSONObject registryCredentials = settings.getJSONObject("registryCredentials");
                JSONArray keys = registryCredentials.names();
                List<DockerCredential> credentialList = new ArrayList<>(credentialMap.values());
                int index = 0;
                for (int i = 0; i < keys.length(); i++) {
                    JSONObject credential = registryCredentials.getJSONObject(keys.getString(i));
                    if (credential.has("username") && credential.getString("username").startsWith("$") && index < credentialList.size()) {
                        JSONObject updatedCredential = new JSONObject(mapper.valueToTree(credentialList.get(index++)).toString());
                        registryCredentials.put(keys.getString(i), updatedCredential);
                    }
                }
            }
        }

        JSONObject newJson = new JSONObject();
        newJson.put("content", deploymentJson);
        PrintWriter writer = new PrintWriter(deploymentJsonPath);
        writer.write(newJson.toString());
        writer.close();

        // deploy using azure cli
        String condition = "";
        if (deploymentType.equals("multiple")) {
            condition = targetCondition;
        } else {
            condition = "deviceId='" + deviceId + "'";
        }
        AzureCredentials.ServicePrincipal servicePrincipal = AzureCredentials.getServicePrincipal(getAzureCredentialsId());
        AzureCredentialCache credentialCache = new AzureCredentialCache(servicePrincipal);
        ShellExecuter azExecuter = new ShellExecuter(listener.getLogger());
        try {
            azExecuter.login(credentialCache);

            String scriptToDelete = "az iot edge deployment delete --hub-name \"" + iothubName + "\" --config-id \"" + deploymentId + "\"";
            azExecuter.executeAZ(scriptToDelete, true);
        } catch (Exception e) {
            if (!e.getMessage().contains("ConfigurationNotFound")) {
                listener.getLogger().println("Failure: " + e.getMessage());
                run.setResult(Result.FAILURE);
            }
        }

        try {
            String scriptToDeploy = "az iot edge deployment create --config-id \"" + deploymentId + "\" --hub-name \"" + iothubName + "\" --content \"" + deploymentJsonPath + "\" --target-condition \"" + condition + "\" --priority \"" + priority + "\"";
            executer.executeAZ(scriptToDeploy, true);
        } catch (Exception e) {
            listener.getLogger().println("Failure: " + e.getMessage());
            run.setResult(Result.FAILURE);
        }
    }

    // TODO: remove this
    private void writeEnvFile(String path, String url, String username, String password, String bypassModules) {
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
        writer.println(Constants.IOTEDGEDEV_ENV_REGISTRY_USERNAME + "=\"" + username + "\"");
        writer.println(Constants.IOTEDGEDEV_ENV_REGISTRY_PASSWORD + "=\"" + password + "\"");
        writer.println(Constants.IOTEDGEDEV_ENV_ACTIVE_MODULES + "=\"" + bypassModules + "\"");
        writer.close();
    }

    @Extension
    @Symbol("azureIoTEdgeDeploy")
    public static final class DescriptorImpl extends BaseBuilder.DescriptorImpl {
        public static final String defaultPriority = "10";

        public FormValidation doCheckTargetCondition(@QueryParameter String value)
                throws IOException, ServletException {
            if(Util.isValidTargetCondition(value)) {
                return FormValidation.ok();
            }else {
                return FormValidation.error("Target condition is not in right format. Click help button to learn more.");
            }
        }

        public FormValidation doCheckPriority(@QueryParameter String value)
                throws IOException, ServletException {
            if(Util.isValidPriority(value)) {
                return FormValidation.ok();
            }else {
                return FormValidation.error("Priority is not in right format. Click help button to learn more.");
            }
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

        public ListBoxModel doFillIothubNameItems(@AncestorInPath Item owner,
                                                  @QueryParameter String azureCredentialsId,
                                                  @QueryParameter String resourceGroup) {
            if (StringUtils.isNotBlank(azureCredentialsId) && StringUtils.isNotBlank(resourceGroup)) {
                return listIothubNameItems(owner, azureCredentialsId, resourceGroup);
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
            return "Azure IoT Edge Deploy";
        }

    }

}
