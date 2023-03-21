package ru.qmbo.usersservice.controller;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Controller;
import ru.qmbo.usersservice.dto.CollectMessage;
import ru.qmbo.usersservice.dto.GetAllUsersMessage;
import ru.qmbo.usersservice.dto.StatisticMessage;
import ru.qmbo.usersservice.dto.SubscribeMessage;
import ru.qmbo.usersservice.service.UserService;

@Controller
@Log4j2
@AllArgsConstructor
public class RabbitController {
    private final UserService userService;

    @RabbitListener(queues = "${rabbit.queue.input.collect}")
    public void collectUser(CollectMessage message) {
        log.info("CollectMessage: {}", message);
        this.userService.collectUser(message);
    }
    @RabbitListener(queues = "${rabbit.queue.input.subscribe}")
    public void subscribeUser(SubscribeMessage message) {
        log.info("SubscribeMessage id: {}", message);
        this.userService.subscribe(message.getChatId());
    }
    @RabbitListener(queues = "${rabbit.queue.input.unsubscribe}")
    public void unsubscribeUser(SubscribeMessage message) {
        log.info("SubscribeMessage: {}", message);
    }
    @RabbitListener(queues = "${rabbit.queue.input.statistic}")
    public void statisticUser(StatisticMessage message) {
        log.info("StatisticMessage: {}", message);
    }
    @RabbitListener(queues = "${rabbit.queue.input.getAllUsers}")
    public void statisticUser(GetAllUsersMessage message) {
        log.info("GetAllUsersMessage: {}", message);
    }
}
