package ru.qmbo.usersservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.qmbo.usersservice.dto.TelegramMessage;

/**
 * KafkaService
 *
 * @author Victor Egorov (qrioflat@gmail.com).
 * @version 0.1
 * @since 08.12.2022
 */
@Service
public class KafkaService {

    private final String adminChatId;
    private final String topic;
    private final KafkaTemplate<Integer, TelegramMessage> template;

    /**
     * Instantiates a new Kafka service.
     *
     * @param adminChatId the admin chat id
     * @param topic       kafka topic
     * @param template    the template
     */
    public KafkaService(@Value("${telegram.chat-id}") String adminChatId, @Value("${kafka.topic}")String topic,
                        KafkaTemplate<Integer, TelegramMessage> template) {
        this.adminChatId = adminChatId;
        this.topic = topic;
        this.template = template;
    }

    /**
     * Send message.
     *
     * @param message the message
     */
    public void sendMessage(TelegramMessage message) {
        this.template.send(topic, message);
    }

    /**
     * Send message to admin.
     *
     * @param message the message
     */
    public void sendMessageToAdmin(TelegramMessage message) {
        this.sendMessage(message.setChatId(Long.parseLong(this.adminChatId)));
    }
}
