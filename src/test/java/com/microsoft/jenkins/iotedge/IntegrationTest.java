package com.microsoft.jenkins.iotedge;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.util.AzureCredentials;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.core.AzureClientFactory;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsGlobalConfig;
import com.microsoft.jenkins.iotedge.util.Constants;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Result;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.browser.GitRepositoryBrowser;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.util.Secret;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Created by zhqqi on 7/23/2018.
 */
public class IntegrationTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();
    @Rule
    public Timeout globalTimeout = new Timeout(20, TimeUnit.MINUTES);
    //Timeout.seconds(20 * 60); // integration tests are very slow
    private static final Logger LOGGER = Logger.getLogger(IntegrationTest.class.getName());

    @Test
    public void main() throws Exception {
        // Prepare
        String branch = "master";
        FreeStyleProject project = j.createFreeStyleProject();
        setScmUsingBranch(project, branch);

        String testName = "jenkins-test-"+ branch;
        String deviceId= testName+"-device";

        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "common",
                dockerRegistryEndpoint,
                null,
                "",
                null);

        project.getBuildersList().add(pushBuilder);

        EdgeDeployBuilder deployBuilder = getDeployBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "single",
                deviceId,
                null,
                testName,
                "0",
                null);
        project.getBuildersList().add(deployBuilder);

        // Test
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.SUCCESS);
        assertThat(s, CoreMatchers.containsString("BUILD COMPLETE"));

        // Load env
        assertThat(s, CoreMatchers.containsString("Environment Variables loaded from: .env"));

        // Expand modules
        assertThat(s, CoreMatchers.not((CoreMatchers.containsString("\"image\": \"${MODULES."))));

        // Push
        assertThat(s, CoreMatchers.containsString("\"status\":\"The push refers to repository"));
        assertThat(s, CoreMatchers.containsString("PUSH COMPLETE"));
        assertThat(s, CoreMatchers.containsString("PUSHING DOCKER IMAGE: michaeljqzq/jenkins-test-master:0.0.1-amd64"));

        // Deploy
        assertThat(s, CoreMatchers.containsString("\"id\": \""+testName+"\","));
        assertThat(s, CoreMatchers.containsString("\"targetCondition\": \"deviceId='"+deviceId+"'\""));
        int index = s.indexOf("Error");
        if(index != -1) {
            assertTrue(!s.contains("error") && s.substring(index - 2, index + 5).equals("0 Error"));
        }
    }

    @Test
    public void empty() throws Exception {
        // Prepare
        String branch = "empty";
        FreeStyleProject project = j.createFreeStyleProject();
        setScmUsingBranch(project, branch);

        String testName = "jenkins-test-"+ branch;
        String deviceId= testName+"-device";

        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "common",
                dockerRegistryEndpoint,
                null,
                "",
                null);

        project.getBuildersList().add(pushBuilder);

        EdgeDeployBuilder deployBuilder = getDeployBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "single",
                deviceId,
                null,
                testName,
                "0",
                null
        );
        project.getBuildersList().add(deployBuilder);

        // Test
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.FAILURE);
    }

    @Test
    public void errorMissingManifest() throws Exception {
        // Prepare
        String branch = "error-missing-manifest";
        FreeStyleProject project = j.createFreeStyleProject();
        setScmUsingBranch(project, branch);

        String testName = "jenkins-test-"+ branch;
        String deviceId= testName+"-device";

        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "common",
                dockerRegistryEndpoint,
                null,
                "",
                null);

        project.getBuildersList().add(pushBuilder);

        EdgeDeployBuilder deployBuilder = getDeployBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "single",
                deviceId,
                null,
                testName,
                "0",
                null
        );

        // Test
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.FAILURE);
        assertThat(s, CoreMatchers.containsString("Unable to find config files in solution root directory"));

        project.getBuildersList().remove(pushBuilder);
        project.getBuildersList().add(deployBuilder);

        build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.FAILURE);
        assertThat(s, CoreMatchers.containsString("Unable to find config files in solution root directory"));
    }

//    @Test
//    public void errorBreakManifest() throws Exception {
//        // Prepare
//        String branch = "error-break-manifest";
//        FreeStyleProject project = j.createFreeStyleProject();
//        setScmUsingBranch(project, branch);
//
//        String testName = "jenkins-test-"+ branch;
//        String deviceId= testName+"-device";
//
//        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
//                testEnv.testResourceGroup,
//                "common",
//                dockerRegistryEndpoint,
//                null,
//                "");
//
//        project.getBuildersList().add(pushBuilder);
//
//        EdgeDeployBuilder deployBuilder = getDeployBuilderInstance(testEnv.credentialIdAzure,
//                testEnv.testResourceGroup,
//                "single",
//                deviceId,
//                null,
//                testName,
//                "0"
//        );
//
//        // Test
//        FreeStyleBuild build = project.scheduleBuild2(0).get();
//        System.out.println(build.getDisplayName() + " completed");
//
//        // Verify
//        String s = FileUtils.readFileToString(build.getLogFile());
//        System.out.println(s);
//
//        // Result of build
////        assertEquals(build.getResult(), Result.FAILURE);
////        assertThat(s, CoreMatchers.containsString("Unable to find config files in solution root directory"));
//
//        project.getBuildersList().remove(pushBuilder);
//        project.getBuildersList().add(deployBuilder);
//
//        build = project.scheduleBuild2(0).get();
//        System.out.println(build.getDisplayName() + " completed");
//
//        // Verify
//        s = FileUtils.readFileToString(build.getLogFile());
//        System.out.println(s);
//
//        // Result of build
//        assertEquals(build.getResult(), Result.FAILURE);
//        assertThat(s, CoreMatchers.containsString("Unable to find config files in solution root directory"));
//    }

    @Test
    public void errorMissingDockerfile() throws Exception {
        // Prepare
        String branch = "error-missing-dockerfile";
        FreeStyleProject project = j.createFreeStyleProject();
        setScmUsingBranch(project, branch);

        String testName = "jenkins-test-"+ branch;
        String deviceId= testName+"-device";

        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "common",
                dockerRegistryEndpoint,
                null,
                "",
                null);

        project.getBuildersList().add(pushBuilder);

        // Test
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.FAILURE);
    }

    @Test
    public void errorMismatchDocker() throws Exception {
        // Prepare
        String branch = "error-mismatch-docker";
        FreeStyleProject project = j.createFreeStyleProject();
        setScmUsingBranch(project, branch);

        String testName = "jenkins-test-"+ branch;
        String deviceId= testName+"-device";

        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "common",
                dockerRegistryEndpoint,
                null,
                "",
                null);

        project.getBuildersList().add(pushBuilder);


        // Test
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.FAILURE);
        assertThat(s, CoreMatchers.not((CoreMatchers.containsString("PUSH COMPLETE"))));
    }

    @Test
    public void errorCode() throws Exception {
        // Prepare
        String branch = "error-code";
        FreeStyleProject project = j.createFreeStyleProject();
        setScmUsingBranch(project, branch);

        String testName = "jenkins-test-"+ branch;
        String deviceId= testName+"-device";

        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "common",
                dockerRegistryEndpoint,
                null,
                "",
                null);

        project.getBuildersList().add(pushBuilder);


        // Test
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.FAILURE);
        assertThat(s, CoreMatchers.containsString("BuildError"));
        assertThat(s, CoreMatchers.not((CoreMatchers.containsString("BUILD COMPLETE"))));
    }

    @Test
    public void customTag() throws Exception {
        // Prepare
        String branch = "custom-tag";
        FreeStyleProject project = j.createFreeStyleProject();
        Map<String, String> envs = new HashMap<>();
        String envValue = "custom-tags";
        envs.put("ZHIQING", envValue);
        setEnv(envs);
        setScmUsingBranch(project, branch);

        String testName = "jenkins-test-"+ branch;
        String deviceId= testName+"-device";

        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "common",
                dockerRegistryEndpoint,
                null,
                "",
                null);

        project.getBuildersList().add(pushBuilder);

        EdgeDeployBuilder deployBuilder = getDeployBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "single",
                deviceId,
                null,
                testName,
                "0",
                null
        );
        project.getBuildersList().add(deployBuilder);

        // Test
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.SUCCESS);
        assertThat(s, CoreMatchers.containsString("BUILD COMPLETE"));

        // Load env
        assertThat(s, CoreMatchers.containsString("Environment Variables loaded from: .env"));

        // Expand modules
        assertThat(s, CoreMatchers.not((CoreMatchers.containsString("\"image\": \"${MODULES."))));

        // Push
        assertThat(s, CoreMatchers.containsString("\"status\":\"The push refers to repository"));
        assertThat(s, CoreMatchers.containsString("PUSH COMPLETE"));
        assertThat(s, CoreMatchers.containsString("PUSHING DOCKER IMAGE: michaeljqzq/jenkins-test-"+envValue+":0.0.1"+envValue+"-amd64"));

        // Deploy
        assertThat(s, CoreMatchers.containsString("\"id\": \""+testName+"\","));
        assertThat(s, CoreMatchers.containsString("\"targetCondition\": \"deviceId='"+deviceId+"'\""));
        assertThat(s, CoreMatchers.containsString("\"customValue\": \""+envValue+"\""));
        int index = s.indexOf("Error");
        if(index != -1) {
            assertTrue(!s.contains("error") && s.substring(index - 2, index + 5).equals("0 Error"));
        }
    }

    @Test(timeout = 300000)
    public void multiModule() throws Exception {
        // Prepare
        String branch = "mm";
        FreeStyleProject project = j.createFreeStyleProject();
        setScmUsingBranch(project, branch);

        String testName = "jenkins-test-"+ branch;
        String deviceId= testName+"-device";

        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "common",
                dockerRegistryEndpoint,
                null,
                "CSharpModule,FilterModuleACR",
                null);

        EdgePushBuilder pushBuilder2 = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "acr",
                null,
                testEnv.acrName,
                "FilterModule",
                null);

        project.getBuildersList().add(pushBuilder);
        project.getBuildersList().add(pushBuilder2);

        EdgeDeployBuilder deployBuilder = getDeployBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "single",
                deviceId,
                null,
                testName,
                "0",
                null
        );
        project.getBuildersList().add(deployBuilder);

        // Test
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.SUCCESS);
        assertThat(s, CoreMatchers.containsString("BUILD COMPLETE"));

        // Load env
        assertThat(s, CoreMatchers.containsString("Environment Variables loaded from: .env"));

        // Expand modules
        assertThat(s, CoreMatchers.not((CoreMatchers.containsString("\"image\": \"${MODULES."))));

        // Push
        assertThat(s, CoreMatchers.containsString("\"status\":\"The push refers to repository"));
        assertThat(s, CoreMatchers.containsString("PUSH COMPLETE"));
        assertThat(s, CoreMatchers.containsString("PUSHING DOCKER IMAGE: michaeljqzq/jenkins-test-master:0.0.1-amd64"));
        // 3 modules

        // Deploy
        assertThat(s, CoreMatchers.containsString("\"id\": \""+testName+"\","));
        assertThat(s, CoreMatchers.containsString("\"targetCondition\": \"deviceId='"+deviceId+"'\""));
        // credential
        int index = s.indexOf("Error");
        if(index != -1) {
            assertTrue(!s.contains("error") && s.substring(index - 2, index + 5).equals("0 Error"));
        }
    }

    @Test(timeout = 300000)
    public void subFolder() throws Exception {
        // Prepare
        String branch = "subfolder";
        FreeStyleProject project = j.createFreeStyleProject();
        setScmUsingBranch(project, branch);

        String testName = "jenkins-test-"+ branch;
        String deviceId= testName+"-device";

        EdgePushBuilder pushBuilder = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "common",
                dockerRegistryEndpoint,
                null,
                "",
                "edgesolution");

        EdgeDeployBuilder deployBuilder = getDeployBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "single",
                deviceId,
                null,
                testName,
                "0",
                "edgesolution"
        );

        EdgePushBuilder pushBuilder2 = getPushBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "acr",
                null,
                testEnv.acrName,
                "",
                "edgesolution2");

        EdgeDeployBuilder deployBuilder2 = getDeployBuilderInstance(testEnv.credentialIdAzure,
                testEnv.testResourceGroup,
                "single",
                deviceId,
                null,
                testName,
                "0",
                "edgesolution2"
        );

        project.getBuildersList().add(pushBuilder);
        project.getBuildersList().add(deployBuilder);
        project.getBuildersList().add(pushBuilder2);
        project.getBuildersList().add(deployBuilder2);

        // Test
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        // Verify
        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        // Result of build
        assertEquals(build.getResult(), Result.SUCCESS);
        assertThat(s, CoreMatchers.containsString("BUILD COMPLETE"));

        // Load env
        assertThat(s, CoreMatchers.containsString("Environment Variables loaded from: .env"));

        // Expand modules
        assertThat(s, CoreMatchers.not((CoreMatchers.containsString("\"image\": \"${MODULES."))));

        // Push
        assertThat(s, CoreMatchers.containsString("\"status\":\"The push refers to repository"));
        assertThat(s, CoreMatchers.containsString("PUSH COMPLETE"));
        assertThat(s, CoreMatchers.containsString("PUSHING DOCKER IMAGE: michaeljqzq/jenkins-test-master:0.0.1-amd64"));

        // Deploy
        assertThat(s, CoreMatchers.containsString("\"id\": \""+testName+"\","));
        assertThat(s, CoreMatchers.containsString("\"targetCondition\": \"deviceId='"+deviceId+"'\""));
        int index = s.indexOf("Error");
        if(index != -1) {
            assertTrue(!s.contains("error") && s.substring(index - 2, index + 5).equals("0 Error"));
        }
    }

    protected static class TestEnvironment {

        public final String subscriptionId;
        public final String clientId;
        public final String clientSecret;
        public final String tenantId;
        public final String dockerUrl;
        public final String dockerUsername;
        public final String dockerPassword;
        public final String solutionRepository;
        public final String iotHubName;
        public final String acrName;
        public final String testResourceGroup = "iotedge-jenkins-automation-test";
        public final String credentialIdDocker= "EDGE_TEST_CREDENTIAL_DOCKER_ID";
        public final String credentialIdAzure= "EDGE_TEST_CREDENTIAL_AZURE_ID";


        TestEnvironment() throws Exception {
            subscriptionId = TestEnvironment.loadFromEnv("EDGE_TEST_SUBSCRIPTION_ID", false);
            clientId = TestEnvironment.loadFromEnv("EDGE_TEST_CLIENT_ID", false);
            clientSecret = TestEnvironment.loadFromEnv("EDGE_TEST_CLIENT_SECRET", false);
            tenantId = TestEnvironment.loadFromEnv("EDGE_TEST_TENANT_ID", false);
            dockerUrl = TestEnvironment.loadFromEnv("EDGE_TEST_DOCKER_URL", false);
            dockerUsername = TestEnvironment.loadFromEnv("EDGE_TEST_DOCKER_USERNAME", false);
            dockerPassword = TestEnvironment.loadFromEnv("EDGE_TEST_DOCKER_PASSWORD", false);
            solutionRepository = TestEnvironment.loadFromEnv("EDGE_TEST_SOLUTION_REPO", false);
            iotHubName = TestEnvironment.loadFromEnv("EDGE_TEST_IOTHUB_NAME", false);
            acrName = TestEnvironment.loadFromEnv("EDGE_TEST_ACR_NAME", false);

            AppInsightsGlobalConfig.get().setAppInsightsEnabled(false);
        }

        private static String loadFromEnv(final String name) throws Exception {
            return loadFromEnv(name, true);
        }

        private static String loadFromEnv(final String name, boolean allowNull) throws Exception {
            String result = TestEnvironment.loadFromEnv(name, null);
            if(result == null && !allowNull) {
                throw new Exception("Env key: "+name+" is missing");
            }
            return result;
        }

        private static String loadFromEnv(final String name, final String defaultValue) {
            final String value = System.getenv(name);
            if (value == null || value.isEmpty()) {
                return defaultValue;
            } else {
                return value;
            }
        }

        public static String GenerateRandomString(int length) {
            String uuid = UUID.randomUUID().toString();
            return uuid.replaceAll("[^a-z0-9]", "a").substring(0, length);
        }
    }

    protected TestEnvironment testEnv = null;
    protected AzureCredentials.ServicePrincipal servicePrincipal = null;
    protected DockerRegistryEndpoint dockerRegistryEndpoint = null;

    @Before
    public void setUp() throws Exception {
        testEnv = new TestEnvironment();
        StandardUsernamePasswordCredentials cre = new StandardUsernamePasswordCredentials() {
            @NonNull
            @Override
            public Secret getPassword() {
                return Secret.fromString(testEnv.dockerPassword);
            }

            @NonNull
            @Override
            public String getDescription() {
                return null;
            }

            @NonNull
            @Override
            public String getId() {
                return testEnv.credentialIdDocker;
            }

            @NonNull
            @Override
            public String getUsername() {
                return testEnv.dockerUsername;
            }

            @Override
            public CredentialsScope getScope() {
                return CredentialsScope.GLOBAL;
            }

            @NonNull
            @Override
            public CredentialsDescriptor getDescriptor() {
                return this.getDescriptor();
            }
        };
        SystemCredentialsProvider.getInstance().getCredentials().add(cre);
        dockerRegistryEndpoint = new DockerRegistryEndpoint(testEnv.dockerUrl, testEnv.credentialIdDocker);

        AzureCredentials azureCre = new AzureCredentials(CredentialsScope.GLOBAL, testEnv.credentialIdAzure, "", testEnv.subscriptionId, testEnv.clientId, testEnv.clientSecret);
        azureCre.setTenant(testEnv.tenantId);
        SystemCredentialsProvider.getInstance().getCredentials().add(azureCre);
    }


//    @After
    public void tearDown() {
        clearAzureResources();
    }

    protected void clearAzureResources() {
        try {
            AzureClientFactory.getClient(
                    servicePrincipal.getClientId(),
                    servicePrincipal.getClientSecret(),
                    servicePrincipal.getTenant(),
                    servicePrincipal.getSubscriptionId(),
                    servicePrincipal.getAzureEnvironment()
            ).resourceGroups().deleteByName(testEnv.testResourceGroup);
        } catch (CloudException e) {
            if (e.response().code() != 404) {
                LOGGER.log(Level.SEVERE, null, e);
            }
        }

    }

    public EdgePushBuilder getPushBuilderInstance(String azureCredentialId, String azureResourceGroup, String dockerRegistryType, DockerRegistryEndpoint dockerEndpoint, String acrName, String bypassModules, String rootPath) {
        EdgePushBuilder pushBuilder;
        if(rootPath == null) {
            pushBuilder = new EdgePushBuilder(azureCredentialId, azureResourceGroup);
        }else {
            pushBuilder = new EdgePushBuilder(azureCredentialId, azureResourceGroup, rootPath);
        }
        pushBuilder.setDockerRegistryType(dockerRegistryType);
        if(dockerRegistryType == Constants.DOCKER_REGISTRY_TYPE_COMMON) {
            pushBuilder.setDockerRegistryEndpoint(dockerEndpoint);
        }else {
            pushBuilder.setAcrName(acrName);
        }
        pushBuilder.setBypassModules(bypassModules);
        return pushBuilder;
    }

    public EdgeDeployBuilder getDeployBuilderInstance(String azureCredentialId, String azureResourceGroup, String deploymentType, String deviceId, String targetCondition, String deploymentId, String priority, String rootPath) {
        EdgeDeployBuilder deployBuilder;
        if(rootPath == null) {
            deployBuilder = new EdgeDeployBuilder(azureCredentialId, azureResourceGroup);
        }else {
            deployBuilder = new EdgeDeployBuilder(azureCredentialId, azureResourceGroup, rootPath);
        }
        deployBuilder.setDeploymentType(deploymentType);
        if(deploymentType == "single") {
            deployBuilder.setDeviceId(deviceId);
        }else {
            deployBuilder.setTargetCondition(targetCondition);
        }
        deployBuilder.setDeploymentId(deploymentId);
        deployBuilder.setIothubName(testEnv.iotHubName);
        deployBuilder.setPriority(priority);
        return deployBuilder;
    }

    protected static void setEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }

//    protected static void setEnv2(String key, String value) throws Exception {
//        Class pe = Class.forName("java.lang.ProcessEnvironment");
//        Method getenv = pe.getDeclaredMethod("getenv");
//        getenv.setAccessible(true);
//        Object unmodifiableEnvironment = getenv.invoke(null);
//        Class map = Class.forName("java.util.Collections$UnmodifiableMap");
//        Field m = map.getDeclaredField("m");
//        m.setAccessible(true);
//        Map envMap = (Map) m.get(unmodifiableEnvironment);
//        envMap.put(key, value);
//    }

    public void setScmUsingBranch(Project project, String branch) throws IOException {
        GitSCM scm = new GitSCM(GitSCM.createRepoList(testEnv.solutionRepository, (String)null), Collections.singletonList(new BranchSpec("*/"+branch)), Boolean.valueOf(false), new ArrayList<SubmoduleConfig>(){}, (GitRepositoryBrowser)null, (String)null, new ArrayList<GitSCMExtension>(){});
        project.setScm(scm);
    }

    protected static class CommandErrorException extends Exception {

        private String msg;

        public CommandErrorException(String msg) {
            super();
            this.msg = msg;
        }

        public CommandErrorException(String msg, Exception ex) {
            super();
            if (ex == null) {
                this.msg = msg;
            } else {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                this.msg = msg + ex.getMessage() + "\n" + sw.toString();
            }
        }

        @Override
        public String getMessage() {
            return msg;
        }
    }

    protected void setUpBaseCommandMockErrorHandling(IBaseCommandData commandDataMock) {

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                throw (Exception) args[0];
            }
        }).when(commandDataMock).logError(any(Exception.class));

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                throw (Exception) args[1];
            }
        }).when(commandDataMock).logError(anyString(), any(Exception.class));

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                throw new CommandErrorException((String) args[0]);
            }
        }).when(commandDataMock).logError(anyString());
    }
}
