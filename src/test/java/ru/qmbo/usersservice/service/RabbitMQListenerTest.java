package ru.qmbo.usersservice.service;

import lombok.Getter;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.qmbo.usersservice.dto.UsersMessage;

import java.util.ArrayList;
import java.util.List;

@Getter
@Component
public class RabbitMQListenerTest {
    private final List<UsersMessage> messages = new ArrayList<>();

    @RabbitListener(queues = "${rabbit.queue.output.getAllUsers}")
    public void userListener(UsersMessage message) {
        this.messages.add(message);
    }
}
