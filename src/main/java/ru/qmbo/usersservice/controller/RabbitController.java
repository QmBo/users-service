package ru.qmbo.usersservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import ru.qmbo.usersservice.dto.CollectMessage;
import ru.qmbo.usersservice.dto.GetAllUsersMessage;
import ru.qmbo.usersservice.dto.StatisticMessage;
import ru.qmbo.usersservice.dto.SubscribeMessage;
import ru.qmbo.usersservice.service.UserService;

/**
 * RabbitController
 *
 * @author Victor Egorov (qrioflat@gmail.com).
 * @version 0.1
 * @since 22.03.2023
 */
@Controller
@Log4j2
@RequiredArgsConstructor
public class RabbitController {
    @Value("${telegram.chat-id}")
    private Long adminChatId;
    private final UserService userService;

    /**
     * Collect user.
     *
     * @param message the message
     */
    @RabbitListener(queues = "${rabbit.queue.input.collect}")
    public void collectUser(CollectMessage message) {
        log.info("CollectMessage: {}", message);
        this.userService.collectUser(message);
    }

    /**
     * Subscribe user.
     *
     * @param message the message
     */
    @RabbitListener(queues = "${rabbit.queue.input.subscribe}")
    public void subscribeUser(SubscribeMessage message) {
        log.info("SubscribeMessage id: {}", message);
        this.userService.subscribe(message.getChatId());
    }

    /**
     * Unsubscribe user.
     *
     * @param message the message
     */
    @RabbitListener(queues = "${rabbit.queue.input.unsubscribe}")
    public void unsubscribeUser(SubscribeMessage message) {
        log.info("SubscribeMessage: {}", message);
        this.userService.unsubscribe(message.getChatId());
    }

    /**
     * Statistic user.
     *
     * @param message the message
     */
    @RabbitListener(queues = "${rabbit.queue.input.statistic}")
    public void statisticUser(StatisticMessage message) {
        log.info("StatisticMessage: {}", message);
        if (this.adminChatId.compareTo(message.getChatId()) == 0) {
            this.userService.getStatistic();
        }
    }

    /**
     * Statistic user.
     *
     * @param message the message
     */
    @RabbitListener(queues = "${rabbit.queue.input.getAllUsers}")
    public void statisticUser(GetAllUsersMessage message) {
        this.userService.getAllUsers(message);
        log.info("GetAllUsersMessage: {}", message);
    }
}
