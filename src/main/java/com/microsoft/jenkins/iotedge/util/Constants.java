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
    public static final String AI_WEB_APP = "WebApp";
    public static final String AI_FUNCTIONS = "Functions";
    public static final String AI_START_DEPLOY = "StartDeploy";
    public static final String AI_GIT_DEPLOY = "GitDeploy";
    public static final String AI_GIT_DEPLOY_FAILED = "GitDeployFailed";
    public static final String AI_FTP_DEPLOY = "FTPDeploy";
    public static final String AI_FTP_DEPLOY_FAILED = "GitDeployFailed";
    public static final String AI_WAR_DEPLOY = "WarDeploy";
    public static final String AI_WAR_DEPLOY_FAILED = "WarDeployFailed";
    public static final String AI_DOCKER_DEPLOY = "DockerDeploy";
    public static final String AI_DOCKER_DEPLOY_FAILED = "DockerDeployFailed";
    public static final String AI_DOCKER_PUSH = "Push";
    public static final String AI_DOCKER_PUSH_FAILED = "PushFailed";
}
