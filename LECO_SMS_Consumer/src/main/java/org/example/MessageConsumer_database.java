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

public class MessageConsumer_database {
    private final DBConnect dbConnect;

    public MessageConsumer_database(DBConnect dbConnect) {
        this.dbConnect = dbConnect;
    }

    public static void main(String[] args) throws Exception {
        DBConnect dbConnect = new DBConnect();
        MessageConsumer_database consumer = new MessageConsumer_database(dbConnect);
        consumer.consumeAndWriteToDB();
    }

    public void consumeAndWriteToDB() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()) {
            channel.queueDeclare(Constants.DB_WRITE_QUEUE, true, false, false, null);

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
            channel.basicConsume(Constants.DB_WRITE_QUEUE, false, deliverCallback, consumerTag -> {
            });

            System.out.println("Waiting for messages. To exit press Ctrl+C");
            // Keep the program running to receive messages
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
    }

    private void writeToDB(String fullMessage) {
        try {
            System.out.println("Writing message to DB: " + fullMessage);
            String[] parts = fullMessage.split(
                    ", mobile number: |, status code: |, account number: |, sent date time: |, current date time: |, serverRef: ");
            String message = parts[0].split("message: ")[1];
            String mobileNumber = parts[1];
            String response_code = parts[2];
            String account_no = parts[3];
            String sent_date_time = parts[4];
            String current_date_time = parts[5];
            String sms_delivery_id = parts.length > 6 ? parts[6] : null;

            System.out.println("ServerRef : " + sms_delivery_id);

            // Check if sms_delivery_id is null
            Integer sms_delivery_id_int = null;
            if (sms_delivery_id != null && !sms_delivery_id.isEmpty()) {
                sms_delivery_id_int = Integer.parseInt(sms_delivery_id);
            }

            // Format the date strings
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date sentDate = formatter.parse(sent_date_time);
            Date currentDate = formatter.parse(current_date_time);

            // Get DB connection
            java.sql.Connection conn = dbConnect.getConnection();

            // Prepare SQL statement
            String sql = "INSERT INTO ESMS.SMS_OUTBOX_RABBITMQ (MESSAGE, PHONE_NO, STATUS, ACCOUNT_NO, SENT_DATE_TIME, DATE_TIME, SMS_DELIVERY_ID, OPERATOR, TEMPLATE_ID, SMS_MASK_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            // Set parameters
            pstmt.setString(1, message);
            pstmt.setString(2, mobileNumber);
            pstmt.setInt(3, Integer.parseInt(response_code));
            pstmt.setString(4, account_no);
            pstmt.setTimestamp(5, new java.sql.Timestamp(sentDate.getTime()));
            pstmt.setTimestamp(6, new java.sql.Timestamp(currentDate.getTime()));
            pstmt.setObject(7, sms_delivery_id_int);
            pstmt.setString(8, "HUT");
            pstmt.setString(9, null);
            pstmt.setString(10, null);

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
