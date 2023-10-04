package com.bot.employeeTimeTracongBot.service;

import com.bot.employeeTimeTracongBot.bot.TimeTrackingBot;
import com.bot.employeeTimeTracongBot.data.SheetsName;
import com.bot.employeeTimeTracongBot.google.MySheets;
import com.bot.employeeTimeTracongBot.model.User;
import com.bot.employeeTimeTracongBot.transformer.SheetsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserService {
    SheetsService sheetsService = new SheetsService();
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    SheetsTransformer transformer = new SheetsTransformer();
    MySheets mySheets = new MySheets();

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
            user.setFullName("New employee");
            user.setNickName(nickName);
            user.setAccess(false);
            user.setSendReport(false);
            user.setDateLastReport("0");
            user.setHours(0);
            user.setDateConnecting(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            user.setLocale(message.getFrom().getLanguageCode());
            sheetsService
                    .writeNext(SheetsName.LOGS, "!A", "!A",
                            transformer.transformToData(user));
            sheetsService
                    .writeNext(SheetsName.USERS, "!A", "!C",
                            transformer.transformToData(user));
            logger.info("user from telegram ->" + user);
            return user;
        } else
            return null;
    }

    private boolean checkAccess(User userFromTable) {
        boolean access = userFromTable.isAccess();
        logger.info("Access for user " + userFromTable.getName() + " is " + (access ? "accept" : "denied"));
        return access;
    }

    private boolean checkIsSendReports(User userFromTable) {
        boolean access = userFromTable.isSendReport();
        logger.info("Access to send reports for user " + userFromTable.getName() + " is " + (access ? "accept" : "denied"));
        return access;
    }

}
