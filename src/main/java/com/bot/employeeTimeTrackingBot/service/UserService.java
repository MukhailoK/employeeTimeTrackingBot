package com.bot.employeeTimeTrackingBot.service;

import com.bot.employeeTimeTrackingBot.transformer.SheetsTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import com.bot.employeeTimeTrackingBot.bot.TimeTrackingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import com.bot.employeeTimeTrackingBot.data.SheetsName;
import com.bot.employeeTimeTrackingBot.model.User;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

@Service
public class UserService {
    private final SheetsTransformer transformer;
    private final SheetsService sheetsService;
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);

    @Autowired
    public UserService(SheetsTransformer transformer, SheetsService sheetsService) {
        this.transformer = transformer;
        this.sheetsService = sheetsService;
    }

    public void deleteUser(long chatId) {
        sheetsService.deleteUserFromTableByChatId(chatId);
    }

    public User registration(Message message) {
        long chatId = message.getChatId();
        String firstName = message.getFrom().getFirstName();
        String lastName = message.getFrom().getLastName();
        String nickName = message.getFrom().getUserName();

        String name = firstName + " " + (lastName != null ? lastName : "");
        name = name.trim();
        User user = new User();
        logger.info("User name -> " + name);
        User userFromTable = sheetsService.readUserFromTableByChatId(chatId);
        logger.info("User from Table -> " + userFromTable);
        if (userFromTable == null) {
            user.setName(name);
            user.setChatId(chatId);
            user.setNickName(nickName);
            user.setAccess(false);
            user.setSendReport(false);
            user.setDateConnecting(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            user.setLocale(message.getFrom().getLanguageCode());
            sheetsService
                    .writeNext(SheetsName.USERS, "!A", "!C",
                            transformer.transformToData(user));
            logger.info("user from telegram ->" + user);
            sheetsService
                    .writeNext(SheetsName.LOGS, "!A", "!A",
                            transformer.transformToLog(user));
            return user;
        } else
            return null;
    }

}
