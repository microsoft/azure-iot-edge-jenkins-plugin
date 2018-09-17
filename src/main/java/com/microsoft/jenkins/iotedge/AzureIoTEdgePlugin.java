/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge;

import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsClientFactory;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsUtils;
import com.microsoft.jenkins.azurecommons.telemetry.AzureHttpRecorder;
import com.microsoft.jenkins.iotedge.util.Constants;
import hudson.Plugin;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AzureIoTEdgePlugin extends Plugin {
    private static Properties mavenProperties;
    static {
        mavenProperties = new Properties();
        try {
            mavenProperties.load(AzureIoTEdgePlugin.class.getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendEvent(String jobType, String taskType, String errorMessage, String jobId, String subscriptionId, String iotHubName) {
        Map<String, String> properties = new HashMap<>();
        String jobName = "", buildNumber="";
        Pattern r = Pattern.compile("(.*)\\s#(\\d+)");
        Matcher m = r.matcher(jobId);
        if(m.find()) {
            jobName = m.group(1);
            buildNumber = m.group(2);
        }
        properties.put(Constants.TELEMETRY_KEY_JOB_NAME, AppInsightsUtils.hash(jobName));
        properties.put(Constants.TELEMETRY_KEY_BUILD_NUMBER, buildNumber);
        properties.put(Constants.TELEMETRY_KEY_JOB_TYPE, jobType);
        properties.put(Constants.TELEMETRY_KEY_SUBSCRIPTION_ID, AppInsightsUtils.hash(subscriptionId));
        properties.put(Constants.TELEMETRY_KEY_IOTHUB_NAME, AppInsightsUtils.hash(iotHubName));
        properties.put(Constants.TELEMETRY_KEY_SUCCESS, String.valueOf(errorMessage == null));
        properties.put(Constants.TELEMETRY_KEY_ERROR_MESSAGE, errorMessage);

        properties.put(Constants.TELEMETRY_KEY_TASK_TYPE, taskType);
        properties.put(Constants.TELEMETRY_KEY_COMMON_EXTNAME, mavenProperties.getProperty("artifactId"));

        AppInsightsClientFactory.getInstance(AzureIoTEdgePlugin.class).withInstrumentationKey("fed7fc65-5b4a-4e66-9d46-c5f016d4e2b4")
                .sendEvent("", properties.get(Constants.TELEMETRY_KEY_TASK_TYPE), properties, false);
    }

    public static class AzureTelemetryInterceptor implements Interceptor {
        @Override
        public Response intercept(final Chain chain) throws IOException {
            final Request request = chain.request();
            final Response response = chain.proceed(request);
            new AzureHttpRecorder(AppInsightsClientFactory.getInstance(AzureIoTEdgePlugin.class))
                    ;
            return response;
        }
    }
}
