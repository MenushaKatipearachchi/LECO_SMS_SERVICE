package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class MessageConsumerHutch {

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        List<String> testNumbers = Arrays.asList("94711306818", "9471130681", "+94711306818", "94756897621",
                "0756897621", "+94756897621", "0740531238", "94740531238", "+94740531238",
                "711306818", "0717756869", "94717756869", "+94717756869",
                "0711306818", "+94783227685", "94783227685", "783227685", "0783227685", "+94710168151", "94710168151",
                "710168151", "0710168151", "+94770273653", "94770273653", "770273653", "0770273653", "94715356918",
                "94706897233", "0718144825", "94718144825", "+94718144825", "0742551906", "94742551906", "+94742551906",
                "0711572503", "94711572503", "+94711572503");

        // Login and get the access token
        System.out.println("Logging in...");
        HutchService hutchService = new HutchService();
        hutchService.login();
        System.out.println("Logged in successfully.");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            System.out.println("Declaring queue: " + Constants.SMS_OUTBOX);
            channel.queueDeclare(Constants.SMS_OUTBOX, true, false, false, null);
            System.out.println("Queue declared successfully.");

            // Set up the consumer
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String fullMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + fullMessage);

                try {
                    // Extract the message before the account number
                    String message = fullMessage.substring(0, fullMessage.indexOf("MOBILE:")).trim();

                    // Check if "ACCOUNT_NO:" is present in fullMessage and so on...
                    int accountNoIndex = fullMessage.indexOf("ACCOUNT_NO:");

                    String mobileNumber;

                    if (accountNoIndex != -1) {
                        // Extract mobile number from the message
                        mobileNumber = fullMessage
                                .substring(fullMessage.indexOf("MOBILE:") + 7, accountNoIndex).trim();
                    } else {
                        // Extract mobile number from the message
                        mobileNumber = fullMessage
                                .substring(fullMessage.indexOf("MOBILE:") + 7).trim();
                    }

                    // Check if the mobile number is one of the test numbers
                    if (testNumbers.contains(mobileNumber)) {
                        System.out.println("Mobile number is a test number. Sending SMS...");

                        // Extract account number from the message if present
                        String accountNumber = accountNoIndex != -1 ? fullMessage.substring(accountNoIndex + 11).trim()
                                : "";

                        // Send SMS
                        hutchService.sendSMS(message, mobileNumber, accountNumber, channel);
                    } else {
                        System.out.println("Mobile number is not a test number. Skipping SMS sending.");
                    }

                    // Acknowledge the message
                    System.out.println("Acknowledging message...");
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    System.out.println("Message acknowledged.");
                } catch (Exception e) {
                    System.out.println("An error occurred while handling the delivery: " + e.getMessage());
                    e.printStackTrace();
                    try {
                        // Nack the message and requeue it
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                    } catch (IOException ioException) {
                        System.out.println("Failed to nack the message: " + ioException.getMessage());
                        ioException.printStackTrace();
                    }
                }
            };

            // Consume msg
            System.out.println("Starting to consume messages from queue: " + Constants.SMS_OUTBOX);
            channel.basicConsume(Constants.SMS_OUTBOX, false, deliverCallback, consumerTag -> {
            });

            System.out.println("Waiting for messages. To exit press Ctrl+C");
            // Keep the program running to receive messages
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
