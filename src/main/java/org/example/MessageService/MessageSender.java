package org.example.MessageService;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Service
public class MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    private final String exchangeName = "exchange_name";

    private final String QueueName="Messages";

    public void sendMessage(String operationType, String queueNamePattern) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(exchangeName, "topic");
            channel.queueDeclare(QueueName, true, false, false, null);
            String message = operationType.toString() + " " + queueNamePattern;
            channel.basicPublish(exchangeName, QueueName, null, message.getBytes("UTF-8"));
            logger.info("Sent message: '{}' to exchange: '{}' with queue pattern: '{}'", message, exchangeName, queueNamePattern);

            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            logger.error("Error occurred while sending message", e);
        }

    }
}
