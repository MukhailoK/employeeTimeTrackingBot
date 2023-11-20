package com.bot.employeeTimeTrackingBot.repository;

import com.bot.employeeTimeTrackingBot.model.User;

import java.util.List;

public interface UserRepository {
    List<User> getAllActualUsers();

    List<User> getAllWorkingUsers();

    User registerNewUser(User newUser);

    boolean deleteUserByChatId(long chatId);



}
