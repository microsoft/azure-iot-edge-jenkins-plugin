/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.iotedge.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by zhqqi on 7/16/2018.
 */
public class DockerCredential {
    public String credentialId;
    public String url;
    public boolean isAcr;

    @JsonCreator
    public DockerCredential(@JsonProperty("credentialId") String credentialId, @JsonProperty("isAcr") boolean isAcr, @JsonProperty("url") String url) {
        this.credentialId = credentialId;
        this.isAcr = isAcr;
        this.url = url;
    }
}
