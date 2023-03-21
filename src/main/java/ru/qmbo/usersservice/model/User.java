package ru.qmbo.usersservice.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * User
 *
 * @author Victor Egorov (qrioflat@gmail.com).
 * @version 0.1
 * @since 20.03.2023
 */
@Accessors(chain = true)
@Data
@Document
public class User {
    @Id
    private Long chatId;
    private String name;
    private String subscribe;
}
