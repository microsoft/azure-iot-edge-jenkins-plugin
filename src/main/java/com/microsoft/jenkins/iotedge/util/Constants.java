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
    public static final String EDGE_DEPLOYMENT_CONFIG_FOLDERNAME = "config";
    public static final String EDGE_DEPLOYMENT_CONFIG_FILENAME = "deployment.json";
    public static final String EDGE_MODULE_CONFIG_FILENAME = "module.json";

    public static final String IOTEDGEDEV_ENV_FILENAME = ".env";
    public static final String IOTEDGEDEV_ENV_REGISTRY_SERVER = "CONTAINER_REGISTRY_SERVER";
    public static final String IOTEDGEDEV_ENV_REGISTRY_USERNAME = "CONTAINER_REGISTRY_USERNAME";
    public static final String IOTEDGEDEV_ENV_REGISTRY_PASSWORD = "CONTAINER_REGISTRY_PASSWORD";
    public static final String IOTEDGEDEV_ENV_ACTIVE_MODULES = "BYPASS_MODULES";

    public static final Map<String, String> iotedgedevEnvMap;

    static {
        iotedgedevEnvMap = new TreeMap<String, String>();
        iotedgedevEnvMap.put("ACTIVE_MODULES", "*");
        iotedgedevEnvMap.put("ACTIVE_DOCKER_PLATFORMS", "amd64");
    }

    public static final String IOT_HUB_URL = "%s.azure-devices.net";

    public static final String REST_GET_TOKEN_URL = "https://login.microsoftonline.com/%s/oauth2/token";
    public static final String REST_GET_TOKEN_BODY = "resource=https%%3A%%2F%%2Fmanagement.core.windows.net%%2F&client_id=%s&grant_type=client_credentials&client_secret=%s";
    public static final String REST_GET_IOT_KEY_URL = "https://management.azure.com/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Devices/IotHubs/%s/listkeys?api-version=2018-04-01";
    public static final String REST_GET_DEVICES_URL = "https://%s.azure-devices.net/devices/query?api-version=2018-06-30";
    public static final String REST_GET_DEVICES_BODY = "{\"query\": \"SELECT * FROM DEVICES where capabilities.iotEdge=true\"}";

    /**
     * AI constants.
     */
    public static final String TELEMETRY_KEY_JOB_NAME = "hashJobName";
    public static final String TELEMETRY_KEY_BUILD_NUMBER = "buildNumber";
    public static final String TELEMETRY_KEY_JOB_TYPE = "jobType";
    public static final String TELEMETRY_KEY_TASK_TYPE = "taskType";
    public static final String TELEMETRY_KEY_SUCCESS = "isSuccess";
    public static final String TELEMETRY_KEY_COMMON_EXTNAME = "common.extname";
    public static final String TELEMETRY_KEY_SUBSCRIPTION_ID = "hashSubscriptionId";
    public static final String TELEMETRY_KEY_IOTHUB_NAME = "hashIotHubName";
    public static final String TELEMETRY_KEY_ERROR_MESSAGE = "errorMessage";


    public static final String TELEMETRY_VALUE_TASK_TYPE_PUSH = "Build and Push";
    public static final String TELEMETRY_VALUE_TASK_TYPE_DEPLOY = "Deploy";

    public static final int SAS_TOKEN_MINUTES = 5;
    public static final String CHARSET_UTF_8 = "UTF-8";
}
