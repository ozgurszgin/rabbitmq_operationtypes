//package org.example.MessageService;
//
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;
//import org.example.entity.Message;
//import org.example.entity.OperationType;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.io.IOException;
//import java.util.concurrent.TimeoutException;
//
//import org.slf4j.Logger;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class MessageSenderTest {
//    @Mock
//    private ConnectionFactory connectionFactory;
//
//    @Mock
//    private Connection connection;
//
//    @Mock
//    private Channel channel;
//
//    @Mock
//    private Logger logger;
//
//    @InjectMocks
//    private MessageSender messageSender;
//
//    @BeforeEach
//    void setUp() throws IOException, TimeoutException {
//        when(connectionFactory.newConnection()).thenReturn(connection);
//        when(connection.createChannel()).thenReturn(channel);
//    }
//
//    @AfterEach
//    void tearDown() {
//    }
//
//    @Test
//    void testSendMessage() throws IOException, TimeoutException {
//
//        Message message = new Message();
//        message.setQueueNamePattern("test.queue");
//        message.setOperationType(OperationType.GETALL);
//        message.setLastIpId(12345);
//
//        messageSender.sendMessage(message);
//
//        String expectedMessage = message.getOperationType() + " " + message.getQueueNamePattern() + " " + message.getLastIpId();
//
//        verify(channel).exchangeDeclare("blockedIp.exchange", "topic"); // exchange'in doğruluğunu kontrol et
//        verify(channel).basicPublish("blockedIp.exchange", "MessagesKey", null, expectedMessage.getBytes("UTF-8"));
//
//        verify(channel).close();
//        verify(connection).close();
//    }
//
//    @Test
//    void testSendMessageWithException() throws IOException, TimeoutException {
//
////        when(connectionFactory.newConnection()).thenReturn(connection);
////        when(connection.createChannel()).thenReturn(channel);
//        when(connectionFactory.newConnection()).thenThrow(new IOException("Mocked IOException"));
//        doThrow(new IOException("Test Exception")).when(channel).basicPublish(anyString(), anyString(), any(), any());
//
//        Message message = new Message();
//        message.setQueueNamePattern("test.queue");
//        message.setOperationType(OperationType.GETALL);
//        message.setLastIpId(12345);
//
//        messageSender.sendMessage(message);
//
//        verify(logger).error(eq("Error occurred while sending message"), any(IOException.class));
//
//    }
//}
package org.example.MessageService;

import com.rabbitmq.client.*;
import org.example.entity.Message;
import org.example.entity.OperationType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.RabbitMQContainer;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
class MessageSenderTest {

    @Container
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.11-management-alpine")
            .withExposedPorts(5672, 15672); // 5672 AMQP portu, 15672 yönetim portu

    @Autowired
    private MessageSender messageSender;

    private static Connection connection;
    private static Channel channel;
    private final static String queueName = "MessagesQueue";
    private final static String exchangeName = "blockedIp.exchange";
    private final static String routingKey = "MessagesKey";

    @BeforeAll
    static void setUp() throws IOException, TimeoutException {

        rabbitMQContainer.start();
        try {
            Thread.sleep(5000);
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest"); // Varsayılan kullanıcı
        factory.setPassword("guest"); // Varsayılan şifre
        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(exchangeName, "topic", true);
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, exchangeName, routingKey);
    }

    @AfterAll
    static void tearDown() throws IOException, TimeoutException {
        channel.close();
        connection.close();
        rabbitMQContainer.stop();
    }

    @Test
    void testSendMessage() throws IOException, InterruptedException {
        // Kuyruğu temizle
        channel.queuePurge(queueName);

        // Test mesajını gönder
        Message testMessage = new Message();
        testMessage.setQueueNamePattern("testQueuePattern");
        testMessage.setOperationType(OperationType.ADD);
        testMessage.setLastIpId(12345);
        messageSender.sendMessage(testMessage);

        // Mesajı doğrula
        GetResponse response = channel.basicGet(queueName, true);
        assertNotNull(response, "Mesaj kuyruktan alınamadı");
        assertEquals(testMessage.getOperationType() + " " + testMessage.getQueueNamePattern() + " " + testMessage.getLastIpId(),
                new String(response.getBody(), StandardCharsets.UTF_8));
    }
}
