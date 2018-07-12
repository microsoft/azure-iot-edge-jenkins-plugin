/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge.model;

import com.microsoft.azure.CloudException;

public final class AzureCloudException extends Exception {

    private static final long serialVersionUID = -8157417759485046943L;

    private AzureCloudException(String msg) {
        super(msg);
    }

    private AzureCloudException(String msg, Exception ex) {
        super(msg, ex);
    }

    public static AzureCloudException create(Exception ex) {
        return create(null, ex);
    }

    public static AzureCloudException create(String msg) {
        return new AzureCloudException(msg);
    }

    public static AzureCloudException create(String msg, Exception ex) {
        if (ex instanceof CloudException) {
            // Drop stacktrace of CloudException and throw its message only
            //
            // Fields in CloudException contain details of HTTP requests and responses. Their types are in okhttp
            // package. Once serialized and persisted, these fields may not be recognized and unmarshalled properly
            // when Cloud Statistics Plugin loads next time, as okhttp related classes haven't loaded yet at that
            // point. This can cause crash of the plugin and makes Jenkins unusable.
            if (msg != null) {
                return new AzureCloudException(String.format("%s: %s", msg, ex.getMessage()));
            } else {
                return new AzureCloudException(ex.getMessage());
            }
        } else {
            return new AzureCloudException(msg, ex);
        }
    }
}
