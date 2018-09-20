package com.microsoft.jenkins.iotedge;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * Created by zhqqi on 7/27/2018.
 */
public class Util {
    public static final Pattern targetConditionPattern = Pattern.compile("^(deviceId|tags\\..+|properties\\.reported\\..+).*=.+$");
    public static final Pattern deploymentIdPattern = Pattern.compile("^[a-z0-9-:+%_#*?!(),=@;']+$");
    public static final Pattern priorityPattern = Pattern.compile("^\\d+$");

    public static boolean isValidTargetCondition(String targetCondition) {
        return targetConditionPattern.matcher(targetCondition).find();
    }

    public static boolean isValidDeploymentId(String deploymentId) {
        if(deploymentId.length() > 128) return false;
        return deploymentIdPattern.matcher(deploymentId).find();
    }

    public static boolean isValidPriority(String priority) {
        return priorityPattern.matcher(priority).find();
    }

    public static String getSharedAccessToken(String resourceUri, String signingKey, String policyName, int expiresInMins) {
        try {
            resourceUri = URLEncoder.encode(resourceUri, "utf-8");
            // Set expiration in seconds
            long expires = (System.currentTimeMillis() / 1000) + expiresInMins * 60;

            String toSign = resourceUri + "\n" + expires;

            // Use crypto
            byte[] base64UriEncoded = sha256_HMAC(toSign, Base64.decodeBase64(signingKey));

            // Construct autorization string
            String token = "SharedAccessSignature sr=" + resourceUri + "&sig="
                    + URLEncoder.encode(Base64.encodeBase64String(base64UriEncoded), "utf-8") + "&se=" + expires;
            if (policyName!=null) token += "&skn="+policyName;
            return token;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] sha256_HMAC(String message, byte[] secret) {
        String hash = "";
        byte[] bytes = null;
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret, "HmacSHA256");
            sha256_HMAC.init(secret_key);
            bytes = sha256_HMAC.doFinal(message.getBytes());
        } catch (Exception e) {
            System.out.println("Error HmacSHA256 ===========" + e.getMessage());
        }
        return bytes;
    }

    public static String encodeURIComponent(String str) {
        try {
            String encode = URLEncoder.encode(str, "utf-8");
            encode = encode.replace("+", "%20")
                    .replace("%7E", "~")
                    .replace("%27", "'")
                    .replace("%28", "(")
                    .replace("%29", ")")
                    .replace("%21", "!");
            return encode;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String executePost(String targetURL, String urlParameters, String auth, String contentType) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            if (auth != null) {
                connection.setRequestProperty("Authorization", auth);
            }

            if(contentType != null) {
                connection.setRequestProperty("Content-Type", contentType);
            }

            connection.setRequestProperty("Content-Length",
                    Integer.toString(urlParameters.getBytes().length));

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
