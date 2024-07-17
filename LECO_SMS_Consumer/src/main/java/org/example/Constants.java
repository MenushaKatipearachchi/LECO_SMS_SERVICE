package org.example;

public class Constants {
    public static final String HUTCH_SEND_SMS = "https://bsms.hutch.lk/api/sendsms";
    public static final String HUTCH_LOGIN = "https://bsms.hutch.lk/api/login";
    public static final String HUTCH_REFRESH_ACCESS_TOKEN = "https://bsms.hutch.lk/api/token/accessToken";

    // RabbitMQ queues
    public static final String DB_WRITE_QUEUE = "db_write_queue";
    public static final String INBOX_DB_WRITE = "inbox_db_write";
    public static final String SMS_OUTBOX = "sms_outbox";
    public static final String MEDIUM_PROCESSING = "medium_processing";
    public static final String LOW_PROCESSING = "low_processing";
    public static final String HIGH_PROCESSING = "high_processing";
}
