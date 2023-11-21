package com.bot.employeeTimeTrackingBot.repository;

import com.bot.employeeTimeTrackingBot.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> getAllActualUsers();

    List<User> getAllWorkingUsers();

    User create(User newUser);

    void delete(long chatId);

    boolean changeFlag(long chatId);

    double getTotalMouthHoursForUser(long chatId);

    Optional<User> readUserFromTableByChatId(long chatId);
}
