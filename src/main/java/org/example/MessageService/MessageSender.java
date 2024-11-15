package org.example.MessageService;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.example.entity.Message;
import org.example.entity.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Service
public class MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    private final String exchangeName_ = "blockedIp.exchange";

    private final String routingKey_="MessagesKey";

    public void sendMessage(Message message) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()){
            String exchangeName=exchangeName_;
            channel.exchangeDeclare(exchangeName, "topic");
           // channel.queueDeclare(queueName, true, false, false, null);
            String routingKey = routingKey_;
            String new_Message = message.getOperationType() + " " + message.getQueueNamePattern() + " " + message.getLastIpId();
            channel.basicPublish(exchangeName, routingKey, null, new_Message.getBytes("UTF-8"));
            logger.info("Sent message: '{}' to exchange: '{}'", message, exchangeName);

        } catch (IOException e) {
            logger.error("I/O Error occurred while sending message", e);
        } catch (TimeoutException e) {
            logger.error("Timeout Error occurred while sending message", e);
        }

    }
}
