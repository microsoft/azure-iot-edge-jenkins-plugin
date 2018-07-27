/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge.model;

public class AzureCredentialsValidationException extends Exception {

    private static final long serialVersionUID = -8157417759456046943L;

    public AzureCredentialsValidationException(String message) {
        super(message);
    }

    public AzureCredentialsValidationException() {
        super();
    }

    public AzureCredentialsValidationException(String msg, Exception excep) {
        super(msg, excep);
    }

    public AzureCredentialsValidationException(Exception excep) {
        super(excep);
    }

}
