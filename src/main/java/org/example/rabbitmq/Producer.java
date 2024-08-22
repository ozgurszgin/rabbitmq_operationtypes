package org.example.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.example.entity.BlockedIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class Producer {
    private static final String EXCHANGE_NAME = "fanout_ips";
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);

    public void produceFanoutMessages(List<BlockedIp> blockedIps) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            for (BlockedIp blockedIp : blockedIps) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(blockedIp);
                oos.flush();
                byte[] messageBytes = bos.toByteArray();
                channel.basicPublish(EXCHANGE_NAME, "", null, messageBytes);
                logger.info("Yeni IP Queue'ya eklendi: " + blockedIp.getIp());
            }
        } catch (IOException | TimeoutException e) {
            logger.error("Mesaj üretimi sırasında hata: " + e.getMessage());
            throw e;
        }
    }
}

