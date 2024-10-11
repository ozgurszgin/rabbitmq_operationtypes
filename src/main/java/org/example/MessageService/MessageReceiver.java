package org.example.MessageService;

import com.rabbitmq.client.*;
import org.example.entity.BlockedIp;
import org.example.entity.OperationType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.example.repository.BlockedIpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageReceiver {
    private static final Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

    @Autowired
    private final BlockedIpRepository blockedIpRepository;

    private long lastIpId = 1000;
    private final static String exchangeName = "blockedIp.exchange";

    private final static String queueName = "MessagesQueue";
    private final static String routingKey="MessagesKey";

    private Connection connection;
    private Channel channel;

    public MessageReceiver(BlockedIpRepository blockedIpRepository) {
        this.blockedIpRepository = blockedIpRepository;
    }

    public void listenForMessages() {
        try {
            connectToRabbitMQ();
            channel.basicConsume(queueName, true, this::processMessage, consumerTag -> {
            });
        } catch (IOException e) {
            logger.error("Error setting up consumer: " + e.getMessage());
        }
    }

    private void connectToRabbitMQ() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, "topic");
            logger.info("MessageReceiver Connected to RabbitMQ");
            channel.queueDeclare(queueName,true,false,false,null);
            channel.queueBind(queueName, exchangeName, routingKey);
            Thread.sleep(5000);
        } catch (IOException | TimeoutException e) {
            logger.error("Error connecting to RabbitMQ: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void processMessage(String consumerTag, Delivery delivery) throws IOException {
        String message = new String(delivery.getBody());
        String[] parts = message.split(" ");
        if (parts.length != 3) {
            logger.warn("Invalid message format");
            return;
        }

        String operationType = parts[0];
        String queueNamePattern = parts[1];
        long lastIpId = Long.parseLong(parts[2]);

        logger.info("Processing message - Operation Type: {}, Queue Name Pattern: {}", operationType, queueNamePattern);

        if (OperationType.GETALL.name().equals(operationType)) {
            List<BlockedIp> allIps = fetchAllIpsFromServer();
            createQueueAndAddIps(queueNamePattern, allIps);
        } else if (OperationType.ADD.name().equals(operationType)) {
            List<BlockedIp> newIps = fetchNewIpsFromServer(lastIpId);
            createQueueAndAddIps(queueNamePattern, newIps);
        } else {
            logger.warn("Unknown operation type: {}", operationType);
        }
    }

    private void createQueueAndAddIps(String queueNamePattern, List<BlockedIp> ips) throws IOException {
        String createQueueName = queueNamePattern;

        channel.queueDeclare(createQueueName, false, false, true, null);
        channel.queueBind(createQueueName, exchangeName, queueNamePattern);


        for (BlockedIp ip : ips) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(ip);
            oos.flush();
            byte[] messageBytes = bos.toByteArray();
            channel.basicPublish(exchangeName, queueNamePattern, null, messageBytes);
            logger.info("Published IP: " + ip.getIp() + " to queue: " + createQueueName);
        }
    }

    private List<BlockedIp> fetchAllIpsFromServer() {
        List<BlockedIp> blockedIps = blockedIpRepository.findAll();
        lastIpId = blockedIps.get(blockedIps.size() - 1).getId();
        logger.info("Fetched all IPs from server.");

        return blockedIps;
    }

    private List<BlockedIp> fetchNewIpsFromServer(long lastIpId) {
        List<BlockedIp> newIps = blockedIpRepository.findByIdGreaterThan(lastIpId);
        if (!newIps.isEmpty()) {
            this.lastIpId = newIps.get(newIps.size() - 1).getId();
            logger.info("Fetched new IPs from server.");
        } else {
            logger.info("No new IPs found.");
        }
        return newIps;
    }
}
