package org.example.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.example.MessageService.MessageSender;
import org.example.entity.BlockedIp;
import org.example.entity.Message;
import org.example.entity.OperationType;
import org.example.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Service
public class Consumer1 {

    private static final Logger logger = LoggerFactory.getLogger(Consumer1.class);
    private static final String EXCHANGE_NAME = "blockedIp.exchange";
    Connection connection;
    Channel channel;

    @Autowired
    FileService fileService;

    @Autowired
    MessageSender messageSender;

    int lastPID = 1000;
    boolean hasRestarted = true;

    public void startConsumer() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                try {
                    if (connection == null || !connection.isOpen() || channel == null || !channel.isOpen()) {
                        connectToRabbitMQ();
                    }

                    String queueName;
                    if (hasRestarted) {
                        queueName = generateUniqueQueueName("consumer1_getall");
                        sendGetAllMessage(queueName);
                        hasRestarted = false;
                    } else {
                        queueName = "consumer1_add";
                        sendAddMessage(queueName);
                    }

                    Thread.sleep(20000);
                    consumeMessages(queueName);
                } catch (IOException e) {
                    logger.error("Consumer1 hatası: " + e.getMessage());
                    reconnectToRabbitMQ();
                }

                try {
                    Thread.sleep(20000); // Bir süre bekleyip tekrar dene
                } catch (InterruptedException e) {
                    logger.error("Consumer interrupted", e);
                }
            }
        });
    }

    void consumeMessages(String queueName) throws IOException, InterruptedException {
        try {
            channel.queueBind(queueName, EXCHANGE_NAME, queueName);
            logger.info("Consumer1 queue başarıyla bind edildi: " + queueName);
        } catch (IOException e) {
            logger.error("Consumer1 queue bind hatası: " + e.getMessage(), e);
            throw e;
        }
        Thread.sleep(5000);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            byte[] body = delivery.getBody();
            try (ByteArrayInputStream bis = new ByteArrayInputStream(body);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                BlockedIp blockedIp = (BlockedIp) ois.readObject();
                fileService.writeToFile(blockedIp.toString(), queueName); // Mesajı dosyaya yaz
                logger.info("Consumer1 BlockedIp " + blockedIp.getIp() + " " + queueName + "'in listesine yazıldı.");

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (ClassNotFoundException | IOException e) {
                logger.error("Consumer1 Mesaj işleme hatası: " + e.getMessage());
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };
        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }

    void connectToRabbitMQ() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Map<String, Object> args = new HashMap<>();
            args.put("x-max-priority", 10);
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            logger.info("Consumer1 RabbitMQ'ya bağlanıldı");
        } catch (IOException | TimeoutException e) {
            logger.error("Consumer1 RabbitMQ'ya bağlanırken hata: " + e.getMessage());
        }
    }

    void reconnectToRabbitMQ() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            logger.error("Consumer1 Bağlantıyı kapatırken hata: " + e.getMessage());
        }
        connectToRabbitMQ();
        logger.info("Consumer1 RabbitMQ'ya yeniden bağlanıldı.");
    }

    String generateUniqueQueueName(String baseName) {
        return baseName + "." + System.currentTimeMillis();
    }

    void sendGetAllMessage(String queueNamePattern) {
        Message message=new Message();
        message.setOperationType(OperationType.GETALL);
        message.setLastIpId(lastPID);
        message.setQueueNamePattern(queueNamePattern);
        messageSender.sendMessage(message);
    }

    void sendAddMessage(String queueName) {
        Message message=new Message();
        message.setOperationType(OperationType.ADD);
        message.setLastIpId(lastPID);
        message.setQueueNamePattern(queueName);
        messageSender.sendMessage(message);
    }
}
