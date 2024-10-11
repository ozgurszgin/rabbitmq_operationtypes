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
    private final String exchangeName = "blockedIp.exchange";

    private final String queueName = "MessagesQueue";
    private final String routingKey="MessagesKey";

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
        String queueName = queueNamePattern;

        channel.queueDeclare(queueName, false, false, true, null);
        channel.queueBind(queueName, exchangeName, queueNamePattern);


        for (BlockedIp ip : ips) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(ip);
            oos.flush();
            byte[] messageBytes = bos.toByteArray();
            channel.basicPublish(exchangeName, queueNamePattern, null, messageBytes);
            logger.info("Published IP: " + ip.getIp() + " to queue: " + queueName);
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
//    private void createExchangeAndAddIps(String queueNamePattern, List<BlockedIp> ips) {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
//
//        try (Connection connection = factory.newConnection();
//             Channel channel = connection.createChannel()) {
//
//            channel.exchangeDeclare(exchangeName, "topic", true);
//            String routingKey = queueNamePattern; // Örneğin: "consumer3.add"
//
//
//            for (BlockedIp ip : ips) {
//                String message = String.valueOf(ip.getIp());
//                channel.basicPublish(exchangeName, routingKey, null, message.getBytes());
//                logger.info("Added IPs to exchange with routing key: {}", routingKey);
//            }
//
//        } catch (IOException | TimeoutException e) {
//            logger.error("Error occurred while adding IPs to exchange", e);
//        }
//    }
//    public void listenForMessages() {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
//
//        try {
//            Connection connection = factory.newConnection();
//            Channel channel = connection.createChannel();
//
//            channel.exchangeDeclare(exchangeName, "topic");
//
//            String tempQueueName = channel.queueDeclare().getQueue(); // Geçici bir kuyruk oluştur
//            channel.queueBind(queueName, exchangeName, "#");
//            channel.queueBind(tempQueueName, exchangeName, "#");
//
//            logger.info("Listening for messages...");
//
//            Consumer consumer = new DefaultConsumer(channel) {
//                @Override
//                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
//                    String message = new String(body, "UTF-8");
//                    logger.info("Received message: {}", message);
//
//                    // Mesajı işleme
//                    String[] messageParts = message.split(" ");
//                    String operationType = messageParts[0];
//                    String queueNamePattern = messageParts[1];
//                    lastIpId = Long.parseLong(messageParts[2]);
//
//                    processMessage(operationType, queueNamePattern, lastIpId);
//                }
//            };
//
//            channel.basicConsume(queueName, true, consumer);
//
//        } catch (IOException | TimeoutException e) {
//            logger.error("Error occurred while listening for messages", e);
//        }
//    }

//    private void processMessage(String operationType, String queueNamePattern, long lastIpId) {
//        logger.info("Processing message - Operation Type: {}, Queue Name Pattern: {}", operationType, queueNamePattern);
//
//        if (OperationType.GETALL.name().equals(operationType)) {
//            List<BlockedIp> allIps = fetchAllIpsFromServer();
//            createExchangeAndAddIps(queueNamePattern, allIps);
//        } else if (OperationType.ADD.name().equals(operationType)) {
//            List<BlockedIp> newIps = fetchNewIpsFromServer(lastIpId);
//            createExchangeAndAddIps(queueNamePattern, newIps);
//        } else {
//            logger.warn("Unknown operation type: {}", operationType);
//        }
//    }

//    private void publishToExchange(String exchangeName, List<BlockedIp> ips) {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
//
//        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
//            channel.exchangeDeclare(exchangeName, "fanout");
//
//            for (BlockedIp ip : ips) {
//                String message = String.valueOf(ip.getIp());
//                channel.basicPublish(exchangeName, "", null, message.getBytes());
//                logger.info("IP '{}' has been published to exchange '{}'", message, exchangeName);
//            }
//        } catch (IOException | TimeoutException e) {
//            logger.error("Error occurred while publishing to exchange", e);
//        }
//    }
//}

//        private void publishToExchange(String exchangeName, List<BlockedIp> ips) {
//            ConnectionFactory factory = new ConnectionFactory();
//            factory.setHost("localhost");
//
//            try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
//                channel.exchangeDeclare(exchangeName, "fanout");
//
//                for (BlockedIp ip : ips) {
//                    String message = ip.getIp();
//                    channel.basicPublish(exchangeName, "", null, message.getBytes());
//                    logger.info("IP '{}' has been published to exchange '{}'", message, exchangeName);
//                }
//            } catch (IOException | TimeoutException e) {
//                logger.error("Error occurred while publishing to exchange", e);
//            }
//        }
//        factory.setHost("localhost");
//
//        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
//            channel.exchangeDeclare(exchangeName, "fanout");
//
//            for (BlockedIp ip : ips) {
//                String message = String.valueOf( ip.getIp());
//                channel.basicPublish(exchangeName, "", null, message.getBytes());
//                logger.info("IP '{}' has been published to exchange '{}'", message, exchangeName);
//            }
//        } catch (IOException | TimeoutException e) {
//            logger.error("Error occurred while publishing to exchange", e);
//        }
//    }
//    public void processMessage(String operationType, String queueNamePattern, String lastIpId) {
//        logger.info("Processing message - Operation Type: " + operationType + ", Queue Name Pattern: " + queueNamePattern);
//
//        if (OperationType.getAll.equals(operationType)) {
//            List<BlockedIp> allIps = fetchAllIpsFromServer();
//            createQueueAndAddIps(queueNamePattern, allIps);
//        } else if (OperationType.add.equals(operationType)) {
//            // Yeni IP'leri sunucudan çek ve ilgili kuyruğa ekle
//            List<BlockedIp> newIps = fetchNewIpsFromServer();
//            createQueueAndAddIps(queueNamePattern, newIps);
//        }else {
//            logger.warn("Unknown operation type: {}", operationType);
//        }
//    }
//
//    private List<BlockedIp> fetchAllIpsFromServer() {
//        // Sunucudan tüm IP'leri çekme işlemi burada yapılır.
//        List<BlockedIp> blockedIps;
//        blockedIps = blockedIpRepository.findAll();
//        lastIpId = blockedIps.get(blockedIps.size() - 1).getId();
//        logger.info("Publisher service ile IP'ler çekildi ve producer'a gönderildi.");
//
//        return blockedIps;
//
//    }
//
//    private List<BlockedIp> fetchNewIpsFromServer() {
//        List<BlockedIp> newIps = blockedIpRepository.findByIdGreaterThan(lastIpId);
//        if (!newIps.isEmpty()) {
//            lastIpId = newIps.get(newIps.size() - 1).getId();
//                logger.info("Yeni IP'ler alındı.");
//                return newIps;
//        }
//        else{
//            logger.info("Yeni Ip bulunamadı.");
//            return newIps;
//        }
//    }
//
//    private void createQueueAndAddIps(String queueNamePattern, List<BlockedIp> ips) {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
//
//        try {
//            Connection connection = factory.newConnection();
//            Channel channel = connection.createChannel();
//
//            channel.queueDeclare(queueNamePattern, true, false, false, null);
//
//            for (BlockedIp ip : ips) {
//                String  message = String.valueOf(ip.getIp());
//                channel.basicPublish("", queueNamePattern, null, message.getBytes());
//                logger.info("IP '{}' has been sent to the queue '{}'", message, queueNamePattern);
//            }
//
//            channel.close();
//            connection.close();
//        } catch (IOException | TimeoutException e) {
//            logger.error("Error occurred while creating queue and adding IPs", e);
//        }
//    }
}
