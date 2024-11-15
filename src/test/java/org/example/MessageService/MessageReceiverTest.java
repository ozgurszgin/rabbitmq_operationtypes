package org.example.MessageService;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;
import org.example.entity.BlockedIp;
import org.example.entity.OperationType;
import org.example.repository.BlockedIpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageReceiverTest {

    @Mock
    private BlockedIpRepository blockedIpRepository;

    @Mock
    private Connection connection;

    @Mock
    private Channel channel;

    @Mock
    private Logger logger;

    @InjectMocks
    private MessageReceiver messageReceiver;


    @BeforeEach
    void setUp() {
//        MockitoAnnotations.initMocks(this);
//        messageReceiver = new MessageReceiver(blockedIpRepository, channel);
    }

    @Test
    void testFetchAllIpsFromServer() {

        BlockedIp ip1 = new BlockedIp();
        ip1.setIp(1921681110);
        ip1.setId(12345677);
        BlockedIp ip2 = new BlockedIp();
        ip2.setIp(1921681111);
        ip2.setId(12345678);
        List<BlockedIp> allIps = Arrays.asList(ip1, ip2);

        when(blockedIpRepository.findAll()).thenReturn(allIps);

        List<BlockedIp> result = messageReceiver.fetchAllIpsFromServer();

        assertEquals(allIps, result);
        verify(blockedIpRepository, times(1)).findAll();
        assertEquals(ip2.getId(), messageReceiver.lastIpId);
    }

    @Test
    void testFetchNewIpsFromServer() {

        BlockedIp ip1 = new BlockedIp();
        ip1.setIp(1921681110);
        ip1.setId(12345677);
        BlockedIp ip2 = new BlockedIp();
        ip2.setIp(1921681111);
        ip2.setId(12345678);
        List<BlockedIp> newIps = Arrays.asList(ip1, ip2);

        when(blockedIpRepository.findByIdGreaterThan(12345676)).thenReturn(newIps);
        List<BlockedIp> result = messageReceiver.fetchNewIpsFromServer(12345676);


        assertEquals(newIps, result);
        verify(blockedIpRepository, times(1)).findByIdGreaterThan(12345676);
        assertEquals(ip2.getId(), messageReceiver.lastIpId);
    }

    @Test
    void testProcessMessageWithGetAllOperation() throws Exception {

        String operationType = OperationType.GETALL.name();
        String queueNamePattern = "test.queue";
        BlockedIp ip1 = new BlockedIp();
        ip1.setIp(1921681110);
        ip1.setId(12345677);
        BlockedIp ip2 = new BlockedIp();
        ip2.setIp(1921681111);
        ip2.setId(12345678);
        int lastIpId = 0;
        List<BlockedIp> allIps = Arrays.asList(ip1, ip2);

        String message = operationType + " " + queueNamePattern + " " + lastIpId;
        Delivery delivery = new Delivery(null, null, message.getBytes());

        when(blockedIpRepository.findAll()).thenReturn(allIps);
        messageReceiver.processMessage("GETALL", delivery);


        verify(channel, times(1)).queueDeclare(queueNamePattern, false, false, true, null);
        verify(channel, times(1)).queueBind(queueNamePattern, "blockedIp.exchange", queueNamePattern);
        verify(channel, times(allIps.size())).basicPublish(eq("blockedIp.exchange"), eq(queueNamePattern), isNull(), any(byte[].class));
    }

    @Test
    void testProcessMessageWithAddOperation() throws Exception {

        String operationType = OperationType.ADD.name();
        String queueNamePattern = "test.queue";
        int lastIpId = 1000;
        BlockedIp ip1 = new BlockedIp();
        ip1.setIp(1921681110);
        ip1.setId(lastIpId + 1);
        List<BlockedIp> mockIps = Collections.singletonList(ip1); //singletonList yalnız bir veri alan ve değiştirlemez(ekleme-çıkarma) bir liste demekmiş.
        String message = operationType + " " + queueNamePattern + " " + lastIpId;
        Delivery delivery = new Delivery(null, null, message.getBytes());

        when(blockedIpRepository.findByIdGreaterThan(1000)).thenReturn(mockIps);
        doNothing().when(channel).close();
        doNothing().when(connection).close();
        when(channel.queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), isNull())).thenReturn(null);
//       doNothing().when(channel).basicPublish(anyString(), anyString(), isNull(), any(byte[].class));
//        doNothing().when(channel).queueBind(anyString(), anyString(), anyString());

        messageReceiver.processMessage("ADD", delivery);

        verify(channel).queueDeclare(queueNamePattern, false, false, true, null);
        verify(channel).queueBind(queueNamePattern, "blockedIp.exchange", queueNamePattern);

        for (BlockedIp ip : mockIps) {
            verify(channel).basicPublish(eq("blockedIp.exchange"), eq(queueNamePattern), isNull(), any(byte[].class));
        }

        verify(blockedIpRepository).findByIdGreaterThan(lastIpId);
    }

    @Test
    void testValidMessageFormat() throws Exception {
        String validMessage = "GETALL queue_name_pattern 0";
        Delivery delivery = mock(Delivery.class);
        when(delivery.getBody()).thenReturn(validMessage.getBytes());

        BlockedIp ip1 = new BlockedIp();
        ip1.setIp(1921681110);
        ip1.setId(12345677);
        List<BlockedIp> mockIpList = Arrays.asList(ip1);

        when(messageReceiver.fetchAllIpsFromServer()).thenReturn(mockIpList);
        doNothing().when(messageReceiver).createQueueAndAddIps(anyString(), anyList());

        messageReceiver.processMessage("", delivery);

        verify(logger, times(1)).info("Processing message - Operation Type: {}, Queue Name Pattern: {}", "GETALL", "queue_name_pattern");
        verify(messageReceiver, times(1)).fetchAllIpsFromServer();
        verify(messageReceiver, times(1)).createQueueAndAddIps("queue_name_pattern", mockIpList);
    }

    @Test
    void testInvalidMessageFormat() throws Exception {

        String invalidMessage = "INVALID MESSAGE"; // Geçersiz mesaj (üç parça değil)
        Delivery delivery = mock(Delivery.class);
        when(delivery.getBody()).thenReturn(invalidMessage.getBytes());

        messageReceiver.processMessage("", delivery);


        verify(logger, times(1)).warn("Invalid message format"); // Hatalı formatta mesajın uyarı verdiğini doğrula
    }
}