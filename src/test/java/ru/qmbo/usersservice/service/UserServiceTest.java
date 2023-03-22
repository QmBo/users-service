package ru.qmbo.usersservice.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.qmbo.usersservice.dto.CollectMessage;
import ru.qmbo.usersservice.dto.StatisticMessage;
import ru.qmbo.usersservice.dto.SubscribeMessage;
import ru.qmbo.usersservice.dto.UsersMessage;
import ru.qmbo.usersservice.model.User;
import ru.qmbo.usersservice.reposytory.UserRepository;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.qmbo.usersservice.service.UserService.*;


@SpringBootTest
@Testcontainers
class UserServiceTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ConnectionFactory connectionFactory;
    @Autowired
    private RabbitMQListenerTest rabbitMQListenerTest;
    @Value("${kafka.topic.telegram}")
    private String telegramKafkaTopic;
    @MockBean
    private UserRepository userRepository;
    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;
    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaGroupId;
    private KafkaConsumer<String, String> consumer;

    @Container
    public static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:latest"));
    @Container
    public static MongoDBContainer mongoDB = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.10"));
    @Container
    public static RabbitMQContainer mqContainer = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3-management-alpine"));

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", mqContainer::getHost);
        registry.add("spring.rabbitmq.port", mqContainer::getAmqpPort);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("kafka.topic.telegram", () -> "test-topic");
    }

    @BeforeEach
    public void setUp() {
        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, String.class);

        consumer = new KafkaConsumer<>(properties);
    }


    @Test
    public void whenCollectUserThenCollect() throws InterruptedException {
        when(userRepository.findById(112233L)).thenReturn(Optional.empty());
        rabbitTemplate.convertAndSend("users-service", "users.collect.one",
                new CollectMessage().setChatId(112233L).setName("test name")
        );
        Thread.sleep(500);
        verify(userRepository).save(userArgumentCaptor.capture());
        assertThat(userArgumentCaptor.getValue().getName()).isEqualTo("test name");
        assertThat(userArgumentCaptor.getValue().getChatId()).isEqualTo(112233L);
        assertThat(userArgumentCaptor.getValue().getSubscribe()).isNull();
    }

    @Test
    public void whenCollectUserWhenItAlreadyExistThenCollect() throws InterruptedException {
        when(userRepository.findById(112233L))
                .thenReturn(
                        Optional.of(
                                new User().setName("old name").setChatId(112233L).setSubscribe("subscribe")
                        )
                );
        rabbitTemplate.convertAndSend("users-service", "users.collect.one",
                new CollectMessage().setChatId(112233L).setName("test name")
        );
        Thread.sleep(500);
        verify(userRepository).save(userArgumentCaptor.capture());
        assertThat(userArgumentCaptor.getValue().getName()).isEqualTo("test name");
        assertThat(userArgumentCaptor.getValue().getChatId()).isEqualTo(112233L);
        assertThat(userArgumentCaptor.getValue().getSubscribe()).isEqualTo("subscribe");
    }

    @Test
    public void whenSubscribeUserWhenItAlreadySubscribeThenSandKafkaMessage() {
        when(userRepository.findById(112233L))
                .thenReturn(
                        Optional.of(
                                new User().setName("old name").setChatId(112233L).setSubscribe("subscribe")
                        )
                );
        rabbitTemplate.convertAndSend("users-service", "users.subscribe.one",
                new SubscribeMessage().setChatId(112233L)
        );

        consumer.subscribe(Collections.singletonList(telegramKafkaTopic));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000L));
        consumer.close();
        List<ConsumerRecord<String, String>> result = new ArrayList<>(100);
        records.forEach(result::add);
        List<String> messages = result.stream().map(ConsumerRecord::value).toList();

        assertThat(messages).contains("{\"chatId\":112233,\"message\":\"%s\"}".formatted(YOU_ARE_NOT_SUBSCRIBE));
    }

    @Test
    public void whenSubscribeUserWhenItNotSubscribeThenSandKafkaMessage() {
        when(userRepository.findById(112233L)).thenReturn(
                Optional.of(
                        new User().setName("old name").setChatId(112233L)
                )
        );
        rabbitTemplate.convertAndSend("users-service", "users.subscribe.one",
                new SubscribeMessage().setChatId(112233L)
        );

        consumer.subscribe(Collections.singletonList(telegramKafkaTopic));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000L));
        consumer.close();
        List<ConsumerRecord<String, String>> result = new ArrayList<>(100);
        records.forEach(result::add);
        List<String> messages = result.stream().map(ConsumerRecord::value).toList();
        verify(userRepository).save(userArgumentCaptor.capture());

        assertThat(userArgumentCaptor.getValue().getName()).isEqualTo("old name");
        assertThat(userArgumentCaptor.getValue().getChatId()).isEqualTo(112233L);
        assertThat(userArgumentCaptor.getValue().getSubscribe()).isEqualTo(TENGE);
        assertThat(messages).contains("{\"chatId\":112233,\"message\":\"%s\"}".formatted(YOU_ARE_SUBSCRIBE));
    }

    @Test
    public void whenSubscribeNotExistUserThenSandKafkaMessage() {
        when(userRepository.findById(223344L)).thenReturn(
                Optional.empty()
        );
        rabbitTemplate.convertAndSend("users-service", "users.subscribe.one",
                new SubscribeMessage().setChatId(223344L)
        );

        consumer.subscribe(Collections.singletonList(telegramKafkaTopic));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000L));
        consumer.close();
        List<ConsumerRecord<String, String>> result = new ArrayList<>(100);
        records.forEach(result::add);
        List<String> messages = result.stream().map(ConsumerRecord::value).toList();
        verify(userRepository).save(userArgumentCaptor.capture());

        assertThat(userArgumentCaptor.getValue().getName()).isEqualTo(NONAME);
        assertThat(userArgumentCaptor.getValue().getChatId()).isEqualTo(223344L);
        assertThat(userArgumentCaptor.getValue().getSubscribe()).isEqualTo(TENGE);
        assertThat(messages).contains("{\"chatId\":223344,\"message\":\"%s\"}".formatted(YOU_ARE_SUBSCRIBE));
    }


    @Test
    public void whenUnsubscribeUserWhenItAlreadyUnsubscribeThenSandKafkaMessage() {
        when(userRepository.findById(112233L))
                .thenReturn(
                        Optional.of(
                                new User().setName("old name").setChatId(112233L)
                        )
                );
        rabbitTemplate.convertAndSend("users-service", "users.unsubscribe.one",
                new SubscribeMessage().setChatId(112233L)
        );

        consumer.subscribe(Collections.singletonList(telegramKafkaTopic));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000L));
        consumer.close();
        List<ConsumerRecord<String, String>> result = new ArrayList<>(100);
        records.forEach(result::add);
        List<String> messages = result.stream().map(ConsumerRecord::value).toList();

        assertThat(messages).contains("{\"chatId\":112233,\"message\":\"%s\"}".formatted(YOU_ARE_NOT_UNSUBSCRIBE));
    }

    @Test
    public void whenUnsubscribeUserWhenItSubscribeThenSandKafkaMessage() {
        when(userRepository.findById(112233L)).thenReturn(
                Optional.of(
                        new User().setName("old name").setChatId(112233L).setSubscribe("sub")
                )
        );
        rabbitTemplate.convertAndSend("users-service", "users.unsubscribe.one",
                new SubscribeMessage().setChatId(112233L)
        );

        consumer.subscribe(Collections.singletonList(telegramKafkaTopic));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000L));
        consumer.close();
        List<ConsumerRecord<String, String>> result = new ArrayList<>(100);
        records.forEach(result::add);
        List<String> messages = result.stream().map(ConsumerRecord::value).toList();
        verify(userRepository).save(userArgumentCaptor.capture());

        assertThat(userArgumentCaptor.getValue().getName()).isEqualTo("old name");
        assertThat(userArgumentCaptor.getValue().getChatId()).isEqualTo(112233L);
        assertThat(userArgumentCaptor.getValue().getSubscribe()).isNull();
        assertThat(messages).contains("{\"chatId\":112233,\"message\":\"%s\"}".formatted(YOU_ARE_UNSUBSCRIBE));
    }

    @Test
    public void whenUnsubscribeNotExistUserThenSandKafkaMessage() {
        when(userRepository.findById(223344L)).thenReturn(
                Optional.empty()
        );
        rabbitTemplate.convertAndSend("users-service", "users.unsubscribe.one",
                new SubscribeMessage().setChatId(223344L)
        );

        consumer.subscribe(Collections.singletonList(telegramKafkaTopic));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000L));
        consumer.close();
        List<ConsumerRecord<String, String>> result = new ArrayList<>(100);
        records.forEach(result::add);
        List<String> messages = result.stream().map(ConsumerRecord::value).toList();

        assertThat(messages).contains("{\"chatId\":223344,\"message\":\"%s\"}".formatted(YOU_ARE_NOT_UNSUBSCRIBE));
    }

    @Test
    public void whenStatisticRequestThenMessageToKafka() {
        when(userRepository.findAll())
                .thenReturn(Arrays.asList(new User().setSubscribe(TENGE), new User().setSubscribe(RUB), new User()));
        rabbitTemplate.convertAndSend("users-service", "users.statistic.one",
                new StatisticMessage().setChatId(303775921L)
        );
        consumer.subscribe(Collections.singletonList(telegramKafkaTopic));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000L));
        consumer.close();

        List<ConsumerRecord<String, String>> result = new ArrayList<>(100);
        records.forEach(result::add);
        List<String> messages = result.stream().map(ConsumerRecord::value).collect(Collectors.toList());

        assertThat(messages)
                .contains("{\"chatId\":303775921,\"message\":\"Всего зарегистрировано пользователей: 3\\nИз них подписаны: 2\"}");
    }


    @Test
    public void whenStatisticRequestAndUsersNotFoundThenMessageToKafka() {
        when(userRepository.findAll())
                .thenReturn(Collections.emptyList());
        rabbitTemplate.convertAndSend("users-service", "users.statistic.one",
                new StatisticMessage().setChatId(303775921L)
        );
        consumer.subscribe(Collections.singletonList(telegramKafkaTopic));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000L));
        consumer.close();

        List<ConsumerRecord<String, String>> result = new ArrayList<>(100);
        records.forEach(result::add);
        List<String> messages = result.stream().map(ConsumerRecord::value).collect(Collectors.toList());

        assertThat(messages)
                .contains("{\"chatId\":303775921,\"message\":\"В системе нет пользователей \\uD83E\\uDEE5\"}");
    }

    @Test
    public void whenAllUsersRequestThenMessageToKafka() throws Exception {
        when(userRepository.findAll())
                .thenReturn(
                        Arrays.asList(
                                new User().setChatId(123L),
                                new User().setChatId(321L),
                                new User().setChatId(333L)
                        )
                );
        rabbitTemplate.convertAndSend("users-service", "users.getAllUsers.one",
                new UsersMessage().setId(123456789L)
        );
        Thread.sleep(500);
        final List<UsersMessage> messages = rabbitMQListenerTest.getMessages();
        Long[] expected = {123L, 321L, 333L};
        assertThat(messages.get(0).getUsers()).isEqualTo(expected);
        assertThat(messages.get(0).getId()).isEqualTo(123456789L);
    }
}