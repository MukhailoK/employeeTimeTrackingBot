package com.bot.employeeTimeTracongBot.service;

import com.bot.employeeTimeTracongBot.bot.TimeTrackingBot;
import com.bot.employeeTimeTracongBot.data.SheetsName;
import com.bot.employeeTimeTracongBot.google.MySheets;
import com.bot.employeeTimeTracongBot.lang.En;
import com.bot.employeeTimeTracongBot.lang.Language;
import com.bot.employeeTimeTracongBot.lang.Ua;
import com.bot.employeeTimeTracongBot.model.User;
import com.bot.employeeTimeTracongBot.transformer.SheetsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserService {
    SheetsService sheetsService = new SheetsService();
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    SheetsTransformer transformer = new SheetsTransformer();
    MySheets mySheets = new MySheets();
    private Language languageBot;

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
            user.setFullName(null);
            user.setNickName(nickName);
            user.setAccess(false);
            user.setSendReport(false);
            user.setDateLastReport(null);
            user.setHours(0);
            user.setDateConnecting(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
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

    private void setBotLanguage(Update update) {
        String language = update.getMessage().getFrom().getLanguageCode();
        logger.atInfo().log("user language - " + language);
        languageBot = switch (language) {
            case "uk" -> new Ua();
            default -> new En();
        };
        logger.atInfo().log("app language - " + languageBot.getLanguage());
    }

}
