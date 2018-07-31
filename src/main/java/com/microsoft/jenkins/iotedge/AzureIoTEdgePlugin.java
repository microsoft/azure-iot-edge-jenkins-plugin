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
import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import hudson.Plugin;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.util.HashMap;
import java.util.Map;

import static java.awt.SystemColor.text;

public class AzureIoTEdgePlugin extends Plugin {
    private static AppInsightsClient telemetryClient;
    public AzureIoTEdgePlugin() {
        telemetryClient = AppInsightsClientFactory.getInstance(AzureIoTEdgePlugin.class);
        telemetryClient.withInstrumentationKey("72516779-4009-45e2-9862-abaabb043081");
    }

    public static void sendEvent(final String item, final String action, final Class sourceClass, boolean success, double time, String workspace, String buildNumber) {
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.TELEMETRY_KEY_TEAM_PROJECT, sha256(workspace));
        properties.put(Constants.TELEMETRY_KEY_OS_TYPE, System.getProperty("os.name"));
        properties.put(Constants.TELEMETRY_KEY_BUILD_ID, buildNumber);
        if(sourceClass == EdgePushBuilder.class) {
            properties.put(Constants.TELEMETRY_KEY_TASK_TYPE, Constants.TELEMETRY_VALUE_TASK_TYPE_PUSH);
        }else if(sourceClass == EdgeDeployBuilder.class) {
            properties.put(Constants.TELEMETRY_KEY_TASK_TYPE, Constants.TELEMETRY_VALUE_TASK_TYPE_DEPLOY);
        }
        properties.put(Constants.TELEMETRY_KEY_SUCCESS, String.valueOf(success));
        properties.put(Constants.TELEMETRY_KEY_TASK_TIME, String.valueOf(time));

        telemetryClient.sendEvent(item, action, properties, false);
    }

    private static String sha256(String raw) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        return new String(hash, StandardCharsets.UTF_8);
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
