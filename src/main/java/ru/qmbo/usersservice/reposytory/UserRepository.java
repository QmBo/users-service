package ru.qmbo.usersservice.reposytory;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.qmbo.usersservice.model.User;

/**
 * UserRepository
 *
 * @author Victor Egorov (qrioflat@gmail.com).
 * @version 0.1
 * @since 20.03.2023
 */
public interface UserRepository extends MongoRepository<User, Long> {

}
