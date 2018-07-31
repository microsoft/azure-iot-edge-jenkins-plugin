package com.microsoft.jenkins.iotedge.util;

/**
 * Created by zhqqi on 7/12/2018.
 */
public final class Env {
    public static final String EnvString = "# HOST\n" +
            "#\n" +
            "\n" +
            "RUNTIME_HOME_DIR=\".\"\n" +
            "    # Directory to host the Runtime generated files and certs\n" +
            "    # \".\" - Auto detect, defaults to the following:\n" +
            "    # \"%PROGRAMDATA%\\azure-iot-edge\\data\" - Windows\n" +
            "    # \"/var/lib/azure-iot-edge\" - Linux\n" +
            "    # \"/var/lib/azure-iot-edge\" - Mac\n" +
            "\n" +
            "RUNTIME_CONFIG_DIR=\".\"\n" +
            "    # Directory to host the Runtime config files\n" +
            "    # \".\" - Auto detect, defaults to the following:\n" +
            "    # \"%PROGRAMDATA%\\azure-iot-edge\\config\" - Windows\n" +
            "    # \"/etc/azure-iot-edge\" - Linux\n" +
            "    # \"/etc/azure-iot-edge\" - Mac\n" +
            "\n" +
            "RUNTIME_HOST_NAME=\".\"\n" +
            "    # \".\" - Auto detect\n" +
            "\n" +
            "RUNTIME_TAG=\"1.0-preview\"\n" +
            "\n" +
            "RUNTIME_VERBOSITY=\"INFO\"\n" +
            "    # \"DEBUG\", \"INFO\", \"ERROR\", \"WARNING\"\n" +
            "\n" +
            "#\n" +
            "# MODULES\n" +
            "#\n" +
            "ACTIVE_DOCKER_PLATFORMS=\"\"\n" +
            "    # \"*\" - to build all docker files\n" +
            "    # \"amd64,amd64.debug\" - Comma delimted list of docker files to build\n" +
            "\n" +
            "CONTAINER_TAG=\"\" \n" +
            "\n" +
            "DOTNET_VERBOSITY=\"q\"\n" +
            "    # q[uiet], m[inimal], n[ormal], d[etailed], and diag[nostic]\n" +
            "\n" +
            "DOTNET_EXE_DIR=\"./bin/Debug/netcoreapp2.0/publish\"\n" +
            "    # The default EXE_DIR directory to pass to the Docker build command.\n" +
            "\n" +
            "#\n" +
            "# SOLUTION SETTINGS\n" +
            "#\n" +
            "\n" +
            "CONFIG_OUTPUT_DIR=\"config\"\n" +
            "DEPLOYMENT_CONFIG_FILE=\"deployment.json\" \n" +
            "RUNTIME_CONFIG_FILE=\"runtime.json\" \n" +
            "LOGS_PATH=\"logs\"\n" +
            "MODULES_PATH=\"modules\"\n" +
            "IOT_REST_API_VERSION=\"2017-11-08-preview\"\n" +
            "\n" +
            "#\n" +
            "# DOCKER LOGS COMMAND\n" +
            "#\n" +
            "# Command used when calling iotedgedev docker --logs or --show-logs\n" +
            "\n" +
            "LOGS_CMD=\"start /B start cmd.exe @cmd /k docker logs {0} -f\"\n" +
            "    # \"start /B start cmd.exe @cmd /k docker logs {0} -f\" - for CMD\n" +
            "    # \"docker logs {0} -f -new_console:sV\" - for ConEmu\n" +
            "\n" +
            "#\n" +
            "# AZURE SETTINGS\n" +
            "#\n" +
            "# These settings will override parameters to the `iotedgedev azure --setup` command.\n" +
            "# CREDENTIALS=\"username password\"\n" +
            "# SERVICE_PRINCIPAL=\"username password tenant\"\n" +
            "# RESOURCE_GROUP_LOCATION=\"australiaeast|australiasoutheast|brazilsouth|canadacentral|canadaeast|centralindia|centralus|eastasia|eastus|eastus2|japanwest|japaneast|northeurope|northcentralus|southindia|uksouth|ukwest|westus|westeurope|southcentralus|westcentralus|westus2\"\n" +
            "# IOTHUB_SKU=\"F1|S1|S2|S3\"\n" +
            "# UPDATE_DOTENV=\"True|False\"\n" +
            "\n" +
            "SUBSCRIPTION_ID=\"\"\n" +
            "RESOURCE_GROUP_NAME=\"\"\n" +
            "RESOURCE_GROUP_LOCATION=\"\"\n" +
            "IOTHUB_NAME=\"\"\n" +
            "IOTHUB_SKU=\"\"\n" +
            "EDGE_DEVICE_ID=\"\"\n" +
            "CREDENTIALS=\"\"\n" +
            "SERVICE_PRINCIPAL=\"\"\n" +
            "UPDATE_DOTENV=\"\"\n";
}
