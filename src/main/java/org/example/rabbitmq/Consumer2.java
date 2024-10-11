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
public class Consumer2 {

    private static final Logger logger = LoggerFactory.getLogger(Consumer2.class);
    private static final String EXCHANGE_NAME = "blockedIp.exchange";
    private Connection connection;
    private Channel channel;

    @Autowired
    private FileService fileService;

    @Autowired
    private MessageSender messageSender;

    int lastPID = 1000;
    private boolean hasRestarted = true;

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
                        queueName = generateUniqueQueueName("consumer2_getall");
                        sendGetAllMessage(queueName);
                        hasRestarted = false;
                    } else {
                        queueName = "consumer2_add";
                        sendAddMessage(queueName);
                    }

                    Thread.sleep(20000);
                    consumeMessages(queueName);
                } catch (IOException e) {
                    logger.error("Consumer2 hatası: " + e.getMessage());
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

    private void consumeMessages(String queueName) throws IOException, InterruptedException {
        try {
            channel.queueBind(queueName, EXCHANGE_NAME, queueName);
            logger.info("Consumer2 queue başarıyla bind edildi: " + queueName);
        } catch (IOException e) {
            logger.error("Consumer2 queue bind hatası: " + e.getMessage(), e);
            throw e;
        }
        Thread.sleep(5000);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            byte[] body = delivery.getBody();
            try (ByteArrayInputStream bis = new ByteArrayInputStream(body);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                BlockedIp blockedIp = (BlockedIp) ois.readObject();
                fileService.writeToFile(blockedIp.toString(), queueName); // Mesajı dosyaya yaz
                logger.info("Consumer2 BlockedIp " + blockedIp.getIp() + " " + queueName + "'in listesine yazıldı.");

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (ClassNotFoundException | IOException e) {
                logger.error("Consumer2 Mesaj işleme hatası: " + e.getMessage());
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };
        try {
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
            });
            logger.info("Consumer2 mesajları dinlemeye başladı: " + queueName);
        } catch (IOException e) {
            logger.error("Consumer2 basicConsume hatası: " + e.getMessage(), e);
            throw e;
        }
    }

    private void connectToRabbitMQ() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Map<String, Object> args = new HashMap<>();
            args.put("x-max-priority", 10);
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            logger.info("Consumer2 RabbitMQ'ya bağlanıldı");
        } catch (IOException | TimeoutException e) {
            logger.error("Consumer2 RabbitMQ'ya bağlanırken hata: " + e.getMessage());
        }
    }

    private void reconnectToRabbitMQ() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            logger.error("Consumer2 Bağlantıyı kapatırken hata: " + e.getMessage());
        }
        connectToRabbitMQ();
        logger.info("Consumer2 RabbitMQ'ya yeniden bağlanıldı.");
    }

    private String generateUniqueQueueName(String baseName) {
        return baseName + "." + System.currentTimeMillis();//uuid.
    }

    private void sendGetAllMessage(String queueNamePattern) {
        Message message=new Message();
        message.setOperationType(OperationType.GETALL);
        message.setLastIpId(lastPID);
        message.setQueueNamePattern(queueNamePattern);
        messageSender.sendMessage(message);
    }

    private void sendAddMessage(String queueName) {
        Message message=new Message();
        message.setOperationType(OperationType.ADD);
        message.setLastIpId(lastPID);
        message.setQueueNamePattern(queueName);
        messageSender.sendMessage(message);
    }
}
