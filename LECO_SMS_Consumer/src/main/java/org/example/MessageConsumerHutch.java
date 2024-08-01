package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import okhttp3.*;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageConsumerHutch {

    private static String HUTCH_TOKEN_REFRESH;
    private static String HUTCH_ACCESS_TOKEN;

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        List<String> testNumbers = Arrays.asList("94711306818", "9471130681", "+94711306818", "94756897621", "0756897621", "+94756897621", "0740531238", "94740531238", "+94740531238",
                "711306818", "0717756869", "94717756869", "+94717756869",
                "0711306818", "+94783227685", "94783227685", "783227685", "0783227685", "+94710168151", "94710168151",
                "710168151", "0710168151", "+94770273653", "94770273653", "770273653", "0770273653", "94715356918",
                "94706897233", "0718144825", "94718144825", "+94718144825", "0742551906", "94742551906", "+94742551906", "0711572503", "94711572503", "+94711572503");

        // Login and get the access token
        System.out.println("Logging in...");
        login();
        System.out.println("Logged in successfully.");

        try (Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()) {
            System.out.println("Declaring queue: " + Constants.SMS_OUTBOX);
            channel.queueDeclare(Constants.SMS_OUTBOX, true, false, false, null);
            System.out.println("Queue declared successfully.");

            // Set up the consumer
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    String fullMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println("Received message: " + fullMessage);

                    // Extract and process the message
                    String message = fullMessage.substring(0, fullMessage.indexOf("MOBILE:")).trim();
                    int accountNoIndex = fullMessage.indexOf("ACCOUNT_NO:");

                    String mobileNumber;
                    if (accountNoIndex != -1) {
                        mobileNumber = fullMessage.substring(fullMessage.indexOf("MOBILE:") + 7, accountNoIndex).trim();
                    } else {
                        mobileNumber = fullMessage.substring(fullMessage.indexOf("MOBILE:") + 7).trim();
                    }

                    if (testNumbers.contains(mobileNumber)) {
                        System.out.println("Mobile number is a test number. Sending SMS...");
                        String accountNumber = accountNoIndex != -1 ? fullMessage.substring(accountNoIndex + 11).trim()
                                : "";
                        sendSMS(message, mobileNumber, accountNumber, channel);
                    } else {
                        System.out.println("Mobile number is not a test number. Skipping SMS sending.");
                    }
                } catch (Exception e) {
                    System.out.println("An error occurred while handling the delivery: " + e.getMessage());
                    e.printStackTrace();
                    // Handle the exception (e.g., log it, requeue the message, etc.)
                } finally {
                    try {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        System.out.println("Message acknowledged.");
                    } catch (IOException ioException) {
                        System.out.println("Failed to acknowledge message: " + ioException.getMessage());
                        ioException.printStackTrace();
                    }
                }
            };

            // Start consuming messages
            System.out.println("Starting to consume messages from queue: " + Constants.SMS_OUTBOX);
            channel.basicConsume(Constants.SMS_OUTBOX, false, deliverCallback, consumerTag -> {
                System.out.println("Consumer cancelled: " + consumerTag);
            });

            System.out.println("Waiting for messages. To exit, press Ctrl+C or send an interrupt signal.");

            // Register a shutdown hook to gracefully close resources
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown signal received. Closing resources...");
                try {
                    channel.close();
                    connection.close();
                } catch (Exception e) {
                    System.out.println("Error occurred while closing resources: " + e.getMessage());
                }
            }));

            // Keep the program running until interrupted
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                System.out.println("Main thread interrupted.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendSMS(String message, String mobileNumber, String accountNumber, Channel channel) {
        int maxRetries = 5;  // maximum number of retries
        int retryCount = 0;  // current retry count
        int backoffTime = 1000;  // initial backoff time in milliseconds
    
        while (retryCount < maxRetries) {
            try {
                // Create OkHttpClient
                OkHttpClient client = getUnsafeOkHttpClient1();
    
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
                        .addHeader("Authorization", "Bearer " + HUTCH_ACCESS_TOKEN)
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
                    break;  // exit the retry loop if the request was successful
                } else if (response.code() == 401) {
                    // If token renewal failed due to Unauthorized, call refreshAccessToken API to retrieve fresh tokens
                    String newAccessToken = refreshAccessToken(HUTCH_TOKEN_REFRESH);
    
                    if (newAccessToken != null) {
                        // Retry sending SMS with the new access token
                        sendSMS(message, mobileNumber, accountNumber, channel);
                    } else {
                        System.out.println("Failed to refresh access token. Re login...");
                        login();
                        System.out.println("Logged in successfully. Retrying sending SMS...");
                        sendSMS(message, mobileNumber, accountNumber, channel);
                        System.out.println("SMS sent successfully.");
                    }
                    break;  // exit the retry loop if a new access token was obtained
                } else if (response.code() == 429) {
                    // Handle rate limiting by retrying with exponential backoff
                    System.out.println("Rate limit exceeded. Retrying in " + backoffTime + "ms...");
                    Thread.sleep(backoffTime);
                    retryCount++;
                    backoffTime *= 2;  // double the backoff time for the next retry
                } else {
                    System.out.println("SMS not sent. Response code: " + response.code());
                    System.exit(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    
        if (retryCount == maxRetries) {
            System.out.println("Exceeded maximum number of retries. SMS not sent.");
            System.exit(1);
        }
    }    

    private static void login() {
        OkHttpClient client = getUnsafeOkHttpClient1();

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

                HUTCH_ACCESS_TOKEN = json.getString("accessToken");
                HUTCH_TOKEN_REFRESH = json.getString("refreshToken");
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

    private static String refreshAccessToken(String refreshToken) {
        OkHttpClient client = getUnsafeOkHttpClient1();

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

                HUTCH_ACCESS_TOKEN = newAccessToken;

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

    private static OkHttpClient getUnsafeOkHttpClient1() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
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
                    return new java.security.cert.X509Certificate[] {};
                }
            } };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    }).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}