package ru.qmbo.usersservice.service;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class KafkaService {
    @Value("${telegram.chat-id}")
    private String adminChatId;
    @Value("${kafka.topic.telegram}")
    private String telegramTopic;
    private final KafkaTemplate<Integer, TelegramMessage> template;

    /**
     * Send message.
     *
     * @param message the message
     */
    public void sendMessage(TelegramMessage message) {
        this.template.send(telegramTopic, message);
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
