/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge.util;

import com.microsoft.rest.LogLevel;

import java.util.Map;
import java.util.TreeMap;

public final class Constants {

    private Constants() {
        // Hide
    }

    public static final String PLUGIN_NAME = "AzureJenkinsIoTEdge";

    public static final LogLevel DEFAULT_AZURE_SDK_LOGGING_LEVEL = LogLevel.NONE;

    // the first option for select element. Keep the same value as jenkins pre-defined default empty value.
    public static final String EMPTY_SELECTION = "- none -";

    public static final String JENKINS_TEST_ENVIRONMENT_ENV_KEY = "JENKINS_TEST_ENVIRONMENT_ENV_KEY";

    public static final String DOCKER_REGISTRY_TYPE_ACR = "acr";
    public static final String DOCKER_REGISTRY_TYPE_COMMON = "common";

    public static final String DOCKER_CREDENTIAL_FILENAME = ".dockercredential";
    public static final String EDGE_MODULES_FOLDERNAME = "modules";
    public static final String EDGE_DEPLOYMENT_MANIFEST_FILENAME = "deployment.template.json";
    public static final String EDGE_DEPLOYMENT_CONFIG_FILENAME = "deployment.json";
    public static final String EDGE_MODULE_CONFIG_FILENAME = "module.json";

    public static final String IOTEDGEDEV_ENV_FILENAME = ".env";
    public static final String IOTEDGEDEV_ENV_REGISTRY_SERVER = "CONTAINER_REGISTRY_SERVER";
    public static final String IOTEDGEDEV_ENV_REGISTRY_USERNAME = "CONTAINER_REGISTRY_USERNAME";
    public static final String IOTEDGEDEV_ENV_REGISTRY_PASSWORD = "CONTAINER_REGISTRY_PASSWORD";
    public static final String IOTEDGEDEV_ENV_ACTIVE_MODULES = "BYPASS_MODULES";

    public static Map<String, String> iotedgedevEnvMap;

    static {
        iotedgedevEnvMap = new TreeMap<String, String>();
        iotedgedevEnvMap.put("ACTIVE_MODULES", "*");
        iotedgedevEnvMap.put("ACTIVE_DOCKER_PLATFORMS", "amd64");
    }

    /**
     * AI constants.
     */
    public static final String TELEMETRY_KEY_TEAM_PROJECT = "hashTeamProjectId";
    public static final String TELEMETRY_KEY_OS_TYPE = "osType";
    public static final String TELEMETRY_KEY_BUILD_ID = "buildId";
    public static final String TELEMETRY_KEY_TASK_TYPE = "taskType";
    public static final String TELEMETRY_KEY_NUM_PUSH = "numOfPushSteps";
    public static final String TELEMETRY_KEY_NUM_DEPLOY = "numOfDeploySteps";
    public static final String TELEMETRY_KEY_SUCCESS = "isSuccess";
    public static final String TELEMETRY_KEY_TASK_TIME = "taskTime";
    public static final String TELEMETRY_KEY_COMMON_EXTVERSION = "common.extversion";
    public static final String TELEMETRY_KEY_COMMON_EXTNAME = "common.extname";


    public static final String TELEMETRY_VALUE_TASK_TYPE_PUSH = "Build and Push";
    public static final String TELEMETRY_VALUE_TASK_TYPE_DEPLOY = "Deploy";

}
