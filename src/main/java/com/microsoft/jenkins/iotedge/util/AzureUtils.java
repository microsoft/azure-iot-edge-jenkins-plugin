/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.jenkins.iotedge.util;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.util.AzureBaseCredentials;
import com.microsoft.azure.util.AzureCredentialUtil;
import com.microsoft.jenkins.azurecommons.core.AzureClientFactory;
import com.microsoft.jenkins.azurecommons.core.credentials.TokenCredentialData;
import com.microsoft.jenkins.iotedge.util.Constants;
import hudson.model.Item;

public final class AzureUtils {

    public static TokenCredentialData getToken(Item owner, String credentialId) {
        AzureBaseCredentials credential = AzureCredentialUtil.getCredential(owner, credentialId);
        if (credential == null) {
            throw new IllegalStateException(
                    String.format("Can't find credential in scope %s with id: %s", owner, credentialId));
        }
        return TokenCredentialData.deserialize(credential.serializeToTokenData());
    }

    public static Azure buildClient(Item owner, String credentialId) {
        TokenCredentialData token = getToken(owner, credentialId);
        return AzureClientFactory.getClient(token, new AzureClientFactory.Configurer() {
            @Override
            public Azure.Configurable configure(Azure.Configurable configurable) {
                return configurable
                        .withLogLevel(Constants.DEFAULT_AZURE_SDK_LOGGING_LEVEL)
//                        .withInterceptor(new AzureAppServicePlugin.AzureTelemetryInterceptor())
                        .withUserAgent(AzureClientFactory.getUserAgent(Constants.PLUGIN_NAME,
                                AzureUtils.class.getPackage().getImplementationVersion()));
            }
        });
    }

    private AzureUtils() {
    }
}
