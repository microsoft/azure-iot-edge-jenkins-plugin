package com.microsoft.jenkins.iotedge.model;

import com.microsoft.azure.util.AzureCredentials;

public class AzureCredential {

    public final String subscriptionId;
    public final String clientId;
    public final String clientSecret;
    public final String tenantId;

    public AzureCredential(AzureCredentials.ServicePrincipal servicePrincipal) {
        subscriptionId = servicePrincipal.getSubscriptionId();
        clientId = servicePrincipal.getClientId();
        clientSecret = servicePrincipal.getClientSecret();
        tenantId = servicePrincipal.getTenant();

    }

}
