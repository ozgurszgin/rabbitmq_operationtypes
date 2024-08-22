package org.example.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.example.entity.BlockedIp;
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
    private static final String EXCHANGE_NAME = "fanout_ips";
    private static final String QUEUE_NAME = "ip_queue2";

    private Connection connection;
    private Channel channel;

    @Autowired
    FileService fileService;

    public void consumeFanoutMessages()  {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                try {
                    if (connection == null || !connection.isOpen() || channel == null || !channel.isOpen()) {
                        connectToRabbitMQ();
                    }

                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        byte[] body = delivery.getBody();
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(body);
                             ObjectInputStream ois = new ObjectInputStream(bis)) {
                            BlockedIp blockedIp = (BlockedIp) ois.readObject();
                            fileService.writeToFile(blockedIp.toString(), QUEUE_NAME);
                            logger.info("BlockedIp " + blockedIp.getIp() +QUEUE_NAME+ "'in listesine yazıldı.");

                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        } catch (ClassNotFoundException | IOException e) {
                            logger.error("Mesaj işleme hatası: " + e.getMessage());
                            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                        }
                    };

                    channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
                } catch (IOException e) {
                    logger.error("Tüketici hatası: " + e.getMessage());
                    reconnectToRabbitMQ();
                }
            }
        });
    }

    private void connectToRabbitMQ() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Map<String, Object> args = new HashMap<>();
            args.put("x-max-priority", 10);
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            channel.queueDeclare(QUEUE_NAME, true, false, false, args);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");
            logger.info("RabbitMQ'ya bağlanıldı");
        } catch (IOException | TimeoutException e) {
            logger.error("RabbitMQ'ya bağlanırken hata: " + e.getMessage());
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
            logger.error("Bağlantıyı kapatırken hata: " + e.getMessage());
        }
        connectToRabbitMQ();
        logger.info("RabbitMQ'ya yeniden bağlanıldı.");
    }
}
