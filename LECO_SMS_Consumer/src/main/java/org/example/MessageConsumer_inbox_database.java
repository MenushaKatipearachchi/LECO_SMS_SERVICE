package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

public class MessageConsumer_inbox_database {
    private final DBConnect dbConnect;

    public MessageConsumer_inbox_database(DBConnect dbConnect) {
        this.dbConnect = dbConnect;
    }

    public static void main(String[] args) throws Exception {
        DBConnect dbConnect = new DBConnect();
        MessageConsumer_inbox_database consumer = new MessageConsumer_inbox_database(dbConnect);
        consumer.consumeAndWriteToDB();
    }

    public void consumeAndWriteToDB() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(Constants.INBOX_DB_WRITE, true, false, false, null);

            // Set up the consumer
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String fullMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + fullMessage);

                // Write to DB
                writeToDB(fullMessage);

                // Acknowledge the message
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            // Consume msg
            channel.basicConsume(Constants.INBOX_DB_WRITE, false, deliverCallback, consumerTag -> { });

            System.out.println("Waiting for messages. To exit press Ctrl+C");
            // Keep the program running to receive messages
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
    }

    private void writeToDB(String fullMessage) {
        try {
            System.out.println("Writing message to DB: " + fullMessage);

            // Parse the message as JSON
            JSONObject json = new JSONObject(fullMessage);
            String fromNumber = json.getString("from_number");
            String toNumber = json.getString("to_number");
            String body = json.getString("body");
            String dateTimeStr = json.optString("date_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            
            // Format the date
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date dateTime = formatter.parse(dateTimeStr);

            // Get DB connection
            java.sql.Connection conn = dbConnect.getConnection();

            // Prepare SQL statement
            String sql = "INSERT INTO ESMS.ESMS_INBOX1910_AUTO_PUSH (FROM_NO, TO_NO, MESSAGE, DATE_TIME) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            // Set parameters
            pstmt.setString(1, fromNumber);
            pstmt.setString(2, toNumber);
            pstmt.setString(3, body);
            pstmt.setTimestamp(4, new java.sql.Timestamp(dateTime.getTime()));

            // Execute update
            pstmt.executeUpdate();

            // Close resources
            pstmt.close();
            conn.close();

            System.out.println("Message written to DB: " + fullMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
