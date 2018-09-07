/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge;

import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsClient;
import com.microsoft.jenkins.azurecommons.telemetry.AppInsightsClientFactory;
import com.microsoft.jenkins.azurecommons.telemetry.AzureHttpRecorder;
import com.microsoft.jenkins.iotedge.util.Constants;
import hudson.Plugin;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.awt.SystemColor.text;
import com.microsoft.applicationinsights.TelemetryClient;

public class AzureIoTEdgePlugin extends Plugin {
    private static TelemetryClient telemetry;
    private static Properties mavenProperties;
    static {
        telemetry = new TelemetryClient();
        mavenProperties = new Properties();
        try {
            mavenProperties.load(AzureIoTEdgePlugin.class.getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendEvent(boolean success, long time, String workspace, String buildNumber, String taskType) {
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.TELEMETRY_KEY_TEAM_PROJECT, new String(Base64.encodeBase64(sha256(workspace)), StandardCharsets.UTF_8));
        properties.put(Constants.TELEMETRY_KEY_OS_TYPE, System.getProperty("os.name"));
        properties.put(Constants.TELEMETRY_KEY_BUILD_ID, buildNumber);
        properties.put(Constants.TELEMETRY_KEY_SUCCESS, String.valueOf(success));
        properties.put(Constants.TELEMETRY_KEY_TASK_TIME, String.valueOf(time));

        properties.put(Constants.TELEMETRY_KEY_TASK_TYPE, taskType);
        properties.put(Constants.TELEMETRY_KEY_COMMON_EXTNAME, mavenProperties.getProperty("artifactId"));
        properties.put(Constants.TELEMETRY_KEY_COMMON_EXTVERSION, mavenProperties.getProperty("version"));

        telemetry.trackEvent(mavenProperties.getProperty("artifactId") +"/"+ properties.get(Constants.TELEMETRY_KEY_TASK_TYPE), properties, null);
        telemetry.flush();
    }

    private static byte[] sha256(String raw) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        return hash;
    }

    public static class AzureTelemetryInterceptor implements Interceptor {
        @Override
        public Response intercept(final Chain chain) throws IOException {
            final Request request = chain.request();
            final Response response = chain.proceed(request);
            new AzureHttpRecorder(AppInsightsClientFactory.getInstance(AzureIoTEdgePlugin.class))
                    .record(new AzureHttpRecorder.HttpRecordable()
                            .withHttpCode(response.code())
                            .withHttpMessage(response.message())
                            .withHttpMethod(request.method())
                            .withRequestUri(request.url().uri())
                            .withRequestId(response.header("x-ms-request-id"))
                    );
            return response;
        }
    }
}
