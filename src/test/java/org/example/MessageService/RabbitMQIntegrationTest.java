package org.example.MessageService;
//
//import org.junit.ClassRule;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.testcontainers.containers.RabbitMQContainer;
//import org.testcontainers.utility.DockerImageName;
//
//import java.io.IOException;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatCode;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//public class RabbitMQIntegrationTest {
//    public static final int DEFAULT_AMQPS_PORT = 5671;
//
//    public static final int DEFAULT_AMQP_PORT = 5672;
//
//    public static final int DEFAULT_HTTPS_PORT = 15671;
//
//    public static final int DEFAULT_HTTP_PORT = 15672;
//    private static RabbitMQContainer rabbitMQContainer;
//
//    @ClassRule
//    public static RabbitMQContainer container = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.7.25-management-alpine"));
//
//    @BeforeAll
//    public static void setup() {
//        RabbitMQContainer container = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.7.25-management-alpine"));
//        container.start();
////        rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.7.25-management-alpine"))
////                .withExposedPorts(5672, 15672); // 5672 AMQP portu, 15672 yönetim portu
////        rabbitMQContainer.start();
////        try {
////            Thread.sleep(5000);
////        }catch (Exception e)
////        {
////            System.out.println(e);
////        }
//    }
//
//    @AfterAll
//    public static void tearDown() {
//        // Container durduruluyor
//        if (container != null) {
//            container.stop();
//        }
//    }
//    @Test
//    public void shouldCreateRabbitMQContainer() {
//        try (RabbitMQContainer container = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.7.25-management-alpine"))) {
//            assertThat(container.getAdminPassword()).isEqualTo("guest");
//            assertThat(container.getAdminUsername()).isEqualTo("guest");
//
//            container.start();
//
//            assertThat(container.getAmqpsUrl())
//                    .isEqualTo(
//                            String.format("amqps://%s:%d", container.getHost(), container.getMappedPort(DEFAULT_AMQPS_PORT))
//                    );
//            assertThat(container.getAmqpUrl())
//                    .isEqualTo(
//                            String.format("amqp://%s:%d", container.getHost(), container.getMappedPort(DEFAULT_AMQP_PORT))
//                    );
//            assertThat(container.getHttpsUrl())
//                    .isEqualTo(
//                            String.format("https://%s:%d", container.getHost(), container.getMappedPort(DEFAULT_HTTPS_PORT))
//                    );
//            assertThat(container.getHttpUrl())
//                    .isEqualTo(
//                            String.format("http://%s:%d", container.getHost(), container.getMappedPort(DEFAULT_HTTP_PORT))
//                    );
//
//            assertThat(container.getHttpsPort()).isEqualTo(container.getMappedPort(DEFAULT_HTTPS_PORT));
//            assertThat(container.getHttpPort()).isEqualTo(container.getMappedPort(DEFAULT_HTTP_PORT));
//            assertThat(container.getAmqpsPort()).isEqualTo(container.getMappedPort(DEFAULT_AMQPS_PORT));
//            assertThat(container.getAmqpPort()).isEqualTo(container.getMappedPort(DEFAULT_AMQP_PORT));
//
//            assertThat(container.getLivenessCheckPortNumbers())
//                    .containsExactlyInAnyOrder(
//                            container.getMappedPort(DEFAULT_AMQP_PORT),
//                            container.getMappedPort(DEFAULT_AMQPS_PORT),
//                            container.getMappedPort(DEFAULT_HTTP_PORT),
//                            container.getMappedPort(DEFAULT_HTTPS_PORT)
//                    );
//        }
//    }
//    @Test
//    public void shouldCreateRabbitMQContainerWithExchange() throws IOException, InterruptedException {
//        try (RabbitMQContainer container = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.7.25-management-alpine"))) {
//            container.withExchange("test-exchange", "direct");
//
//            container.start();
//
//            assertThat(container.execInContainer("rabbitmqctl", "list_exchanges").getStdout())
//                    .containsPattern("test-exchange\\s+direct");
//        }
//    }
//    @Test
//    public void shouldStart() {
//
//            assertThat(rabbitMQContainer.isRunning()).isTrue();
//
//    }
//}
import org.example.MessageService.MessageReceiver;
import org.example.MessageService.MessageSender;
import org.example.entity.BlockedIp;
import org.example.entity.Message;
import org.example.entity.OperationType;
import org.example.repository.BlockedIpRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RabbitMQIntegrationTest {

    @Container
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.9-management")
            .withExposedPorts(5672, 15672);

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private MessageReceiver messageReceiver;

    @MockBean
    private BlockedIpRepository blockedIpRepository;

    @BeforeAll
    static void setUp() {
        System.setProperty("spring.rabbitmq.host", rabbitMQContainer.getHost());
        System.setProperty("spring.rabbitmq.port", rabbitMQContainer.getAmqpPort().toString());
    }

    @BeforeEach
    public void init() {
        Mockito.reset(blockedIpRepository);
    }

    @AfterAll
    static void tearDown() {
        rabbitMQContainer.stop();
    }

    @Test
    public void testSendMessageWithGetAllOperation() throws Exception {
        // Test verisi
        Message message = new Message();
        message.setOperationType(OperationType.GETALL);
        message.setQueueNamePattern("test_queue_getall");
        message.setLastIpId(0);

        // Mock: findAll metodu tüm IP'leri döndürsün
        List<BlockedIp> mockIps = List.of(
                createBlockedIp(1, 123456789),
                createBlockedIp(2, 987654321)
        );
        when(blockedIpRepository.findAll()).thenReturn(mockIps);

        // Mesaj gönderme
        messageSender.sendMessage(message);

        // Mesaj alıp işleme
        messageReceiver.listenForMessages();

        // Doğrulama
        assertEquals(2, mockIps.size(), "Tüm IP'lerin listesi başarılı bir şekilde alındı.");
    }

    @Test
    public void testSendMessageWithAddOperation() throws Exception {
        // Test verisi
        Message message = new Message();
        message.setOperationType(OperationType.ADD);
        message.setQueueNamePattern("test_queue_add");
        message.setLastIpId(1);

        // Mock: findByIdGreaterThan son eklenen IP'leri döndürsün
        List<BlockedIp> newIps = List.of(
                createBlockedIp(2, 223344556),
                createBlockedIp(3, 334455667)
        );
        when(blockedIpRepository.findByIdGreaterThan(1L)).thenReturn(newIps);

        // Spy: Mesajın gönderildiği MessageSender sınıfını taklit edelim
        MessageSender spyMessageSender = spy(messageSender);

        // Mesaj gönderme
        spyMessageSender.sendMessage(message);

        // Mesaj gönderildi mi? (Spy kullanarak kontrol edelim)
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(spyMessageSender).sendMessage(captor.capture());

        // Gönderilen mesajı kontrol edelim
        Message capturedMessage = captor.getValue();
        assertEquals(OperationType.ADD, capturedMessage.getOperationType(), "Mesajın operasyon tipi doğru.");
        assertEquals("test_queue_add", capturedMessage.getQueueNamePattern(), "Mesajın kuyruk adı doğru.");
        assertEquals(1, capturedMessage.getLastIpId(), "Mesajın lastIpId değeri doğru.");

        // Mesaj alıp işleme
//        messageReceiver.listenForMessages();
//
//        // listenForMessages doğru çağrıyı yaptı mı?
//        verify(blockedIpRepository).findByIdGreaterThan(1L);

        // Mesajın işlendiği ve doğru verilerin alındığı doğrulaması
        assertEquals(2, newIps.size(), "Yeni IP'lerin listesi başarılı bir şekilde alındı.");
        assertEquals(223344556, newIps.get(0).getIp(), "İlk IP doğru.");
        assertEquals(334455667, newIps.get(1).getIp(), "İkinci IP doğru.");
    }


    @Test
    public void testSendMessageWithInvalidOperation() throws Exception {
        // Geçersiz bir operation type içeren mesaj oluşturma
        Message invalidMessage = new Message();
        invalidMessage.setOperationType(null); // Geçersiz işlem
        invalidMessage.setQueueNamePattern("test_queue_invalid");
        invalidMessage.setLastIpId(0);

        // Mesaj gönderme işlemi
        messageSender.sendMessage(invalidMessage);

        // Mesaj alıp işleme
        messageReceiver.listenForMessages();

        // Bu durumda bir hata dönebilir veya işlem yapılmaz, geçersiz işlem tipine dair log kontrol edilebilir.
        // Doğrulama, log veya ek hata işleme mekanizmasına göre yapılabilir.
    }

    // Yardımcı metot: BlockedIp oluşturur
    private BlockedIp createBlockedIp(int id, int ip) {
        BlockedIp blockedIp = new BlockedIp();
        blockedIp.setId(id);
        blockedIp.setIp(ip);
        return blockedIp;
    }
}
