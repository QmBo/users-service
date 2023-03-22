package ru.qmbo.usersservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.qmbo.usersservice.dto.UsersMessage;

/**
 * The type Rabbit mq service.
 */
@Service
@RequiredArgsConstructor
public class RabbitMQService {
    private final RabbitTemplate rabbitTemplate;
    @Value("${rabbit.exchangeTopic}")
    private String exchange;
    @Value("${rabbit.routingKey}")
    private String routingKey;


    /**
     * Send users massages.
     *
     * @param message the message
     */
    public void sendUsersMassages(UsersMessage message) {
        this.rabbitTemplate.convertAndSend(this.exchange, this.routingKey, message);
    }
}
