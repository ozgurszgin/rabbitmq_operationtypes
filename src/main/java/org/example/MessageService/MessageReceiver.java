package org.example.MessageService;

import com.rabbitmq.client.*;
import org.example.entity.BlockedIp;
import org.example.entity.OperationType;

import java.io.IOException;
import java.util.ArrayList;
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

    private long lastIpId = 0;

    public MessageReceiver(BlockedIpRepository blockedIpRepository) {
        this.blockedIpRepository = blockedIpRepository;
    }

    public void listenForMessages() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare("exchange_name", "topic");
            String queueName="Messages";
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, "exchange_name","#");

            logger.info("Listening for messages...");

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    logger.info("Received message: {}", message);

                    // Mesajı işleme
                    String[] messageParts = message.split(" ");
                    String operationType = messageParts[0];
                    String queueNamePattern = messageParts[1];

                    processMessage(operationType, queueNamePattern);
                }
            };

            channel.basicConsume(queueName, true, consumer);

        } catch (IOException | TimeoutException e) {
            logger.error("Error occurred while listening for messages", e);
        }
    }

    public void processMessage(String operationType, String queueNamePattern) {
        logger.info("Processing message - Operation Type: " + operationType + ", Queue Name Pattern: " + queueNamePattern);

        if (OperationType.getAll.equals(operationType)) {
            List<BlockedIp> allIps = fetchAllIpsFromServer();
            createQueueAndAddIps(queueNamePattern, allIps);
        } else if (OperationType.update.equals(operationType)) {
            // Yeni IP'leri sunucudan çek ve ilgili kuyruğa ekle
            List<BlockedIp> newIps = fetchNewIpsFromServer();
            createQueueAndAddIps(queueNamePattern, newIps);
        }else {
            logger.warn("Unknown operation type: {}", operationType);
        }
    }

    private List<BlockedIp> fetchAllIpsFromServer() {
        // Sunucudan tüm IP'leri çekme işlemi burada yapılır.
        List<BlockedIp> blockedIps;
        blockedIps = blockedIpRepository.findAll();
        lastIpId = blockedIps.get(blockedIps.size() - 1).getId();
        logger.info("Publisher service ile IP'ler çekildi ve producer'a gönderildi.");

        return blockedIps;

    }

    private List<BlockedIp> fetchNewIpsFromServer() {
        List<BlockedIp> newIps = blockedIpRepository.findByIdGreaterThan(lastIpId);
        if (!newIps.isEmpty()) {
            lastIpId = newIps.get(newIps.size() - 1).getId();
                logger.info("Yeni IP'ler alındı.");
                return newIps;
        }
        else{
            logger.info("Yeni Ip bulunamadı.");
            return newIps;
        }
    }

    private void createQueueAndAddIps(String queueNamePattern, List<BlockedIp> ips) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(queueNamePattern, true, false, false, null);

            for (BlockedIp ip : ips) {
                String  message = String.valueOf(ip.getIp());
                channel.basicPublish("", queueNamePattern, null, message.getBytes());
                logger.info("IP '{}' has been sent to the queue '{}'", message, queueNamePattern);
            }

            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            logger.error("Error occurred while creating queue and adding IPs", e);
        }
    }
}
