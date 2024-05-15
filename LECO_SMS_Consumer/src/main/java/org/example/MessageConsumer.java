package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.impl.AMQImpl.Basic;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class MessageConsumer {
    private final DBConnect dbConnect;

    public MessageConsumer(DBConnect dbConnect) {
        this.dbConnect = dbConnect;
    }

    public static void main(String[] args) throws Exception {
        DBConnect dbConnect = new DBConnect();
        MessageConsumer consumer = new MessageConsumer(dbConnect);
        consumer.consumeMessages();
    }

    public void consumeMessages() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()) {
            channel.queueDeclare(Constants.SMS_OUTBOX, true, false, false, null);

            // Consumer set up
            DeliverCallback deliverCallback = createDeliverCallback(channel);

            // Consume msg
            channel.basicConsume(Constants.HIGH_PROCESSING, false, deliverCallback, consumerTag -> {
            });
            channel.basicConsume(Constants.MEDIUM_PROCESSING, false, deliverCallback, consumerTag -> {
            });
            channel.basicConsume(Constants.LOW_PROCESSING, false, deliverCallback, consumerTag -> {
            });

            System.out.println("Waiting for messages. To exit press Ctrl+C");
            // Keep the program running to receive messages
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException | RuntimeException | TimeoutException e) {
            System.out.println("Error setting up consumer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private DeliverCallback createDeliverCallback(Channel channel) {
        return (consumerTag, delivery) -> {
            String fullMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Received message: " + fullMessage);

            try {
                // Acknowledge the message
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                // Parse the message to get the required parameters
                JSONObject json = parseMessage(fullMessage);
                String fromNumber = json.getString("from_number");
                String body = json.getString("body");
                Integer call_type = json.getInt("call_type");
                String message_template = json.getString("message_template");
                String accountNo = null;
                String api = null;

                // Split the body into messageCode
                String[] parts = body.split(" ");
                String messageCode = parts[0]; // ACB or INF ...etc
                
                // if the message code has call type 1 then it is a getAPI call, if
                // it has a call type 2 then it is a postAPI call, if
                // it has a call type 3 then it is a direct call

                // make the result accept a string or a list of strings
                Object result = null;

                if (call_type == 1) {
                    accountNo = parts[1]; // Account number
                    api = json.getString("api");

                    // GetAPI
                    List<String> feedback = getAPI(messageCode, api, fromNumber, accountNo, message_template);
                    result = feedback;
                } else if (call_type == 2) {
                    // PstAPI
                    postAPI(fullMessage, fromNumber);
                } else if (call_type == 3) {
                    // Direct call
                    String directResult = directCall(message_template, fromNumber, body, messageCode);
                    result = directResult;
                }

                // Check the type of result before publishing
                if (result instanceof String) {
                    channel.basicPublish("", Constants.SMS_OUTBOX, null,
                            ((String) result).getBytes(StandardCharsets.UTF_8));
                } else if (result instanceof List) {
                    // Convert the list to a string before publishing
                    String listResult = String.join(",", (List<String>) result);
                    channel.basicPublish("", Constants.SMS_OUTBOX, null, listResult.getBytes(StandardCharsets.UTF_8));
                }
                System.out.println("Published message to " + Constants.SMS_OUTBOX);

            } catch (IOException | RuntimeException e) {
                System.out.println("Error processing message: " + e.getMessage());
                e.printStackTrace();
                // If there's an error, reject the message without re-queueing
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };

    }

    private JSONObject parseMessage(String fullMessage) {
        return new JSONObject(fullMessage);
    }

    private List<String> getAPI(String messageCode, String api, String fromNumber, String accountNumber,
            String message_template) {
        try {
            // Make the API call
            URL url = new URL(api + accountNumber);
            // Create a connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            // Get the response code
            int responseCode = conn.getResponseCode();

            // If the response code is 200, it means the request was successful
            if (responseCode == 200) {
                JSONObject jsonResponse = readResponse(conn);

                if (jsonResponse.has("chatbotAcoountData")) {
                    // Get the 'chatbotAcoountData' object
                    JSONObject chatbotAccountData = jsonResponse.getJSONObject("chatbotAcoountData");

                    if (chatbotAccountData.has("accountNo")) {
                        // Get each parameter separately
                        String accountNo = chatbotAccountData.getString("accountNo").trim();
                        String cusName = chatbotAccountData.getString("name").trim();
                        String accBal = chatbotAccountData.getString("accountBalance").trim();
                        String billVal = chatbotAccountData.getString("lastBillAmount").trim();
                        String billReadDate = chatbotAccountData.getString("lastBillDate").trim();
                        String paymentAmt = chatbotAccountData.getString("lastPayment").trim();
                        String paymentReadDate = chatbotAccountData.getString("lastPaymentDate").trim();

                        // Use the message template
                        String message_body = message_template.replace("{name}", cusName)
                                .replace("{accountBalance}", accBal)
                                .replace("{lastBillAmount}", billVal)
                                .replace("{lastBillDate}", billReadDate)
                                .replace("{lastPayment}", paymentAmt)
                                .replace("{lastPaymentDate}", paymentReadDate)
                                .replace("{fromNumber}", fromNumber)
                                .replace("{accountNo}", accountNo);

                        return Collections.singletonList(message_body);
                    }
                }
            } else {
                System.out.println("GET request did not work!");
                return Collections.emptyList();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        String message_body = "Incorrect account number! MOBILE: " + fromNumber;
        System.out.println(message_body); // Print incorrect account number to console
        return Collections.emptyList();
    }

    private void postAPI(String fullMessage, String fromNumber) {
        // TODO: Implement the POST request when a suitable message code is received
    }

    private String directCall(String message_template, String fromNumber, String body, String messageCode) {
        if (messageCode.equals("LLL")) {
            // Insert a record into the table
            try {
                // Prepare the SQL statement
                String sql = "INSERT INTO ESMS.ESMS_INBOX (ID, PHONE_NO, DATE_TIME, MESSAGE) VALUES (ESMS_INBOX_SEQ.NEXTVAL, ?, ?, ?)";

                // Get a connection from the DBConnect object
                java.sql.Connection conn = dbConnect.getConnection();

                // Create a PreparedStatement
                PreparedStatement pstmt = conn.prepareStatement(sql);

                // Set the values
                // pstmt.setInt(1, ID_incrementer);
                pstmt.setString(1, fromNumber);
                pstmt.setDate(2, new java.sql.Date(System.currentTimeMillis())); // Current date
                pstmt.setString(3, body);

                // Execute the statement
                pstmt.executeUpdate();
                System.out.println("Record inserted successfully to table ESMS_INBOX");
            } catch (SQLException e) {
                System.out.println("Error inserting record: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return message_template + " MOBILE: " + fromNumber;
    }

    private JSONObject readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return new JSONObject(response.toString());
    }
}
