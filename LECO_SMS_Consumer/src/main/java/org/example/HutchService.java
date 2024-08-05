package org.example;

import com.rabbitmq.client.Channel;
import okhttp3.*;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class HutchService {

    private String hutchTokenRefresh;
    private String hutchAccessToken;

    public void login() {
        OkHttpClient client = getUnsafeOkHttpClient();

        // Constructing the request body with username and password
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                "{\"username\": \"" + "ekettipearachchi@gmail.com" + "\", \"password\": \"" + "cp@^68CM" + "\"}");

        // Build the request with appropriate headers and body
        Request request = new Request.Builder()
                .url(Constants.HUTCH_LOGIN)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "*/*")
                .addHeader("X-API-VERSION", "v1")
                .build();

        try {
            // Execute the request and handle the response
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                // Parse the response body to extract the access token and refresh token
                assert response.body() != null;
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);

                hutchAccessToken = json.getString("accessToken");
                hutchTokenRefresh = json.getString("refreshToken");
            } else if (response.code() == 401) {
                // Login failed due to Unauthorized, call login API to retrieve fresh tokens
                System.exit(1);
            } else {
                System.exit(1);
            }
            response.body().close();
        } catch (IOException e) {
            // Handle IOException
            System.exit(1);
        }
    }

    public void sendSMS(String message, String mobileNumber, String accountNumber, Channel channel) {
        try {
            // Create OkHttpClient with increased timeouts
            OkHttpClient client = getUnsafeOkHttpClient();

            // Create JSON object for the request body
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("campaignName", "Test campaign");
            jsonBody.put("mask", "LECOTestH");
            jsonBody.put("numbers", mobileNumber);
            jsonBody.put("content", message);

            // Create RequestBody
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());

            // Create Request
            Request request = new Request.Builder()
                    .url(Constants.HUTCH_SEND_SMS)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + hutchAccessToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "*/*")
                    .addHeader("X-API-VERSION", "v1")
                    .build();

            // Get the current date time when the SMS is being sent
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime currentDateTime = LocalDateTime.now();

            // Send the request and get the response
            Response response = client.newCall(request).execute();

            // Get the current date time when the response is received
            LocalDateTime sentDateTime = LocalDateTime.now();

            System.out.println("Success response code " + response);

            // Handle the response
            if (response.isSuccessful()) {
                // Parse the response body to get the serverRef
                assert response.body() != null;
                String responseBody = response.body().string();

                if (!responseBody.isEmpty() && responseBody.startsWith("{")) {
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    String serverRef = "";
                    if (jsonResponse.has("serverRef")) {
                        Object serverRefObject = jsonResponse.get("serverRef");
                        serverRef = String.valueOf(serverRefObject);
                    }

                    // Prepare data for the new queue
                    String dataForNewQueue = "message: " + message + ", mobile number: " + mobileNumber
                            + ", status code: "
                            + response.code() + ", account number: " + accountNumber + ", sent date time: "
                            + dtf.format(sentDateTime) + ", current date time: " + dtf.format(currentDateTime)
                            + ", serverRef: "
                            + serverRef;

                    // Declare a new queue to store the response
                    String DB_WRITE_QUEUE = "db_write_queue";
                    channel.queueDeclare(DB_WRITE_QUEUE, true, false, false, null);

                    // Publish the data to the new queue
                    channel.basicPublish("", DB_WRITE_QUEUE, null, dataForNewQueue.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Data published to database write queue: " + dataForNewQueue);
                } else {
                    System.out.println("Invalid or empty JSON response.");
                }
            } else if (response.code() == 401) {
                // If token renewal failed due to Unauthorized, call refreshAccessToken API to
                // retrieve fresh tokens
                String newAccessToken = refreshAccessToken(hutchTokenRefresh);

                if (newAccessToken != null) {
                    // Retry sending SMS with the new access token
                    hutchAccessToken = newAccessToken;
                    sendSMS(message, mobileNumber, accountNumber, channel);
                } else {
                    System.out.println("Failed to refresh access token. Re-login...");
                    login();
                    System.out.println("Logged in successfully. Retrying sending SMS...");
                    sendSMS(message, mobileNumber, accountNumber, channel);
                }
            } else if (response.code() == 429) {
                System.out.println("SMS not sent. Response code: " + response.code() + "\nRetrying to send the SMS.");
                // Retry sending the SMS
                sendSMS(message, mobileNumber, accountNumber, channel);
            } else {
                System.out.println("SMS not sent. Response code: " + response.code());
            }
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("SocketTimeoutException occurred");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("An error occurred while sending SMS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String refreshAccessToken(String refreshToken) {
        OkHttpClient client = getUnsafeOkHttpClient();

        // Build the request URL with the refresh token
        HttpUrl url = HttpUrl.parse(Constants.HUTCH_REFRESH_ACCESS_TOKEN).newBuilder()
                .build();

        // Build the request with appropriate headers
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "*/*")
                .addHeader("X-API-VERSION", "v1")
                .addHeader("Authorization", "Bearer " + refreshToken)
                .build();

        try {
            // Execute the request and handle the response
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                // Parse the response body to extract the new access token
                assert response.body() != null;
                String responseBody = response.body().string();
                JSONObject json = new JSONObject(responseBody);

                String newAccessToken = json.getString("accessToken");

                hutchAccessToken = newAccessToken;

                response.body().close();

                // Store or use the new access token as needed
                return newAccessToken;
            } else if (response.code() == 401) {
                // If token renewal failed due to Unauthorized, call login API to retrieve fresh
                // tokens
                System.out.println("Token renewal failed due to Unauthorized. Retrieving fresh tokens...");
                return null;
            } else {
                // Handle other error responses
                System.exit(1);
                return null;
            }
        } catch (IOException e) {
            // Handle IOException
            System.exit(1);
            return null;
        }
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }};

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

