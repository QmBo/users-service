package ru.qmbo.usersservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import ru.qmbo.usersservice.dto.CollectMessage;
import ru.qmbo.usersservice.dto.GetAllUsersMessage;
import ru.qmbo.usersservice.dto.TelegramMessage;
import ru.qmbo.usersservice.dto.UsersMessage;
import ru.qmbo.usersservice.model.User;
import ru.qmbo.usersservice.reposytory.UserRepository;

import java.util.List;
import java.util.Objects;

/**
 * UserService
 *
 * @author Victor Egorov (qrioflat@gmail.com).
 * @version 0.1
 * @since 22.03.2023
 */
@Service
@AllArgsConstructor
@Log4j2
public class UserService {
    public static final String YOU_ARE_SUBSCRIBE = "Вы подписались на рассылку!";
    public static final String YOU_ARE_NOT_SUBSCRIBE = "Вы уже подписаны на рассылку!";
    public static final String TENGE = "tenge";
    public static final String RUB = "rub";
    public static final String NONAME = "Anonymous";
    public static final String NEW_USER_SUBSCRIBE_AT_TENGE = "New user subscribe at tenge =)";
    public static final String USER_UNSUBSCRIBE_AT_TENGE = "User unsubscribe at tenge =(";
    public static final String YOU_ARE_UNSUBSCRIBE = "Вы отписались от рассылки!";
    public static final String YOU_ARE_NOT_UNSUBSCRIBE = "Вы не были подписаны на рассылку!";
    public static final String STATISTIC = "Всего зарегистрировано пользователей: %s\nИз них подписаны: %s";
    public static final String USERS_NOT_FOUND = "В системе нет пользователей \uD83E\uDEE5";

    private final UserRepository repository;
    private final KafkaService kafkaService;
    private final RabbitMQService rabbitMQService;

    /**
     * Collect user.
     *
     * @param message the message
     */
    public void collectUser(CollectMessage message) {
        this.repository.findById(message.getChatId())
                .ifPresentOrElse(
                        user -> this.repository.save(user.setName(message.getName())),
                        () -> {
                            this.repository.save(new User().setName(message.getName()).setChatId(message.getChatId()));
                            log.info("New User collect: {}({})", message.getName(), message.getChatId());
                        }
                );
    }

    /**
     * Subscribe.
     *
     * @param chatId the chat id
     */
    public void subscribe(Long chatId) {
        this.repository.findById(chatId).ifPresentOrElse(
                user -> {
                    if (user.getSubscribe() != null) {
                        kafkaService.sendMessage(
                                new TelegramMessage()
                                        .setMessage(YOU_ARE_NOT_SUBSCRIBE)
                                        .setChatId(chatId)
                        );
                    } else {
                        log.info(NEW_USER_SUBSCRIBE_AT_TENGE);
                        this.repository.save(user.setSubscribe(TENGE));
                        kafkaService.sendMessage(
                                new TelegramMessage()
                                        .setMessage(YOU_ARE_SUBSCRIBE)
                                        .setChatId(chatId)
                        );
                    }
                },
                () -> {
                    log.info(NEW_USER_SUBSCRIBE_AT_TENGE);
                    final User user = new User().setChatId(chatId).setName(NONAME).setSubscribe(TENGE);
                    this.repository.save(user);
                    kafkaService.sendMessage(
                            new TelegramMessage()
                                    .setMessage(YOU_ARE_SUBSCRIBE)
                                    .setChatId(chatId)
                    );
                }
        );
    }

    /**
     * Unsubscribe.
     *
     * @param chatId the chat id
     */
    public void unsubscribe(Long chatId) {
        this.repository.findById(chatId).ifPresentOrElse(
                user -> {
                    if (user.getSubscribe() != null) {
                        log.info(USER_UNSUBSCRIBE_AT_TENGE);
                        this.repository.save(user.setSubscribe(null));
                        kafkaService.sendMessage(
                                new TelegramMessage()
                                        .setMessage(YOU_ARE_UNSUBSCRIBE)
                                        .setChatId(chatId)
                        );
                    } else {
                        kafkaService.sendMessage(
                                new TelegramMessage()
                                        .setMessage(YOU_ARE_NOT_UNSUBSCRIBE)
                                        .setChatId(chatId)
                        );
                    }
                },
                () -> {
                    kafkaService.sendMessage(
                            new TelegramMessage()
                                    .setMessage(YOU_ARE_NOT_UNSUBSCRIBE)
                                    .setChatId(chatId)
                    );
                }
        );
    }

    /**
     * Gets statistic.
     */
    public void getStatistic() {
        String result = USERS_NOT_FOUND;
        final List<User> users = this.repository.findAll();
        if (!users.isEmpty()) {
            final long subscribers = users
                    .stream()
                    .map(User::getSubscribe)
                    .filter(Objects::nonNull)
                    .count();
            result = STATISTIC.formatted(users.size(), subscribers);
        }
        this.kafkaService.sendMessageToAdmin(new TelegramMessage().setMessage(result));
    }

    /**
     * Gets all users.
     *
     * @param message the message
     */
    public void getAllUsers(GetAllUsersMessage message) {
        final Long[] usersIds = this.repository.findAll().stream().map(User::getChatId).toArray(Long[]::new);
        this.rabbitMQService.sendUsersMassages(new UsersMessage().setId(message.getId()).setUsers(usersIds));
    }
}
