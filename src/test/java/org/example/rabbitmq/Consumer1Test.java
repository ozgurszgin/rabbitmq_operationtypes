package org.example.rabbitmq;

import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import org.example.MessageService.MessageSender;
import org.example.entity.Message;
import org.example.entity.OperationType;
import org.example.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Consumer1Test {

    @Mock
    private FileService fileService;

    @Mock
    private MessageSender messageSender;

    @Mock
    private Logger logger;

    @Mock
    private Connection connection;

    @Mock
    private Channel channel;

    @Mock
    private ExecutorService executorService;

    @InjectMocks
    private Consumer1 consumer1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer1 = spy(new Consumer1());
        consumer1.fileService = fileService;
        consumer1.messageSender = messageSender;
        consumer1.channel = channel;
    }

    @Test
    void testStartConsumer_GetAllMessageSentOnRestart() throws Exception {
        doNothing().when(consumer1).connectToRabbitMQ();
        doReturn("consumer1_getall").when(consumer1).generateUniqueQueueName("consumer1_getall");
        doNothing().when(consumer1).consumeMessages(anyString());
        doNothing().when(consumer1).sendGetAllMessage(anyString());

        consumer1.startConsumer();

        verify(consumer1, atLeastOnce()).sendGetAllMessage(anyString());
        verify(consumer1, never()).sendAddMessage(anyString());
    }

    @Test
    void testStartConsumer_AddMessageSentOnSubsequentCalls() throws Exception {
        doNothing().when(consumer1).connectToRabbitMQ();
        doReturn("consumer1_add").when(consumer1).generateUniqueQueueName("consumer1_add");
        doNothing().when(consumer1).consumeMessages(anyString());

        consumer1.hasRestarted = false;
        consumer1.startConsumer();

        verify(consumer1, atLeastOnce()).sendAddMessage(anyString());
        verify(consumer1, never()).sendGetAllMessage(anyString());
    }

    @Test
    void testConsumeMessages_BindsQueueAndConsumesMessage() throws Exception {
        String queueName = "consumer1_getall";

        consumer1.consumeMessages(queueName);

        verify(channel, atLeastOnce()).queueBind(eq(queueName), eq("blockedIp.exchange"), eq(queueName));
        verify(channel).basicConsume(eq(queueName), eq(false), any(DeliverCallback.class), any(CancelCallback.class));
    }

    @Test
    void testSendGetAllMessage_SendsCorrectMessage() {
        String queueNamePattern = "consumer1_getall";

        consumer1.sendGetAllMessage(queueNamePattern);

        verify(messageSender, atLeastOnce()).sendMessage(argThat(message ->
                message.getOperationType() == OperationType.GETALL &&
                        message.getQueueNamePattern().equals(queueNamePattern) &&
                        message.getLastIpId() == consumer1.lastPID
        ));
    }

    @Test
    void testSendAddMessage_SendsCorrectMessage() {
        String queueName = "consumer1_add";

        consumer1.sendAddMessage(queueName);

        verify(messageSender, atLeastOnce()).sendMessage(argThat(message ->
                message.getOperationType() == OperationType.ADD &&
                        message.getQueueNamePattern().equals(queueName) &&
                        message.getLastIpId() == consumer1.lastPID
        ));
    }

    @Test
    void testReconnectToRabbitMQ_ClosesAndReconnects() throws Exception {
        consumer1.connection = connection;
        consumer1.channel = channel;

        doNothing().when(channel).close();
        doNothing().when(connection).close();
        doNothing().when(consumer1).connectToRabbitMQ();

        consumer1.reconnectToRabbitMQ();

        verify(channel, atLeastOnce()).close();
        verify(connection, atLeastOnce()).close();

        verify(consumer1, atLeastOnce()).connectToRabbitMQ();
    }
}
