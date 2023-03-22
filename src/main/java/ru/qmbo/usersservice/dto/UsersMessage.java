package ru.qmbo.usersservice.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * UsersMessage
 *
 * @author Victor Egorov (qrioflat@gmail.com).
 * @version 0.1
 * @since 22.03.2023
 */
@Data
@Accessors(chain = true)
public class UsersMessage {
    private Long id;
    private Long[] users;
}
