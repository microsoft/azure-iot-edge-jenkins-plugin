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
    public String username;
    public String password;
    public String address;

    @JsonCreator
    public DockerCredential(@JsonProperty("username") String username, @JsonProperty("password") String password, @JsonProperty("address") String address) {
        this.username = username;
        this.password = password;
        this.address = address;
    }
}
