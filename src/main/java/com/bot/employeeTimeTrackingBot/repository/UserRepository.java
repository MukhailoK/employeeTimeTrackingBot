package com.bot.employeeTimeTrackingBot.repository;

import com.bot.employeeTimeTrackingBot.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository {
    List<User> getAllActualUsers();

    List<User> getAllWorkingUsers();

    User create(User newUser);

    boolean delete(long chatId);


}
