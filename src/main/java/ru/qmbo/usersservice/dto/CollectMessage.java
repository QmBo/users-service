package ru.qmbo.usersservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * SubscribeMessage
 * 
 * @author Victor Egorov (qrioflat@gmail.com).
 * @version 0.1
 * @since 20.03.2023
 */
@Data
@Accessors(chain = true)
public class CollectMessage {
    @JsonAlias("chat_id")
    private Long chatId;
    private String name;
}
