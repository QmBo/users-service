package ru.qmbo.usersservice.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * TelegramMessage
 *
 * @author Victor Egorov (qrioflat@gmail.com).
 * @version 0.1
 * @since 20.03.2023
 */
@Data
@Accessors(chain = true)
public class TelegramMessage {
    private Long chatId;
    private String message;

}
