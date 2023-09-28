package com.bot.employeeTimeTracongBot.bot;

import com.bot.employeeTimeTracongBot.data.SheetsName;
import com.bot.employeeTimeTracongBot.google.SheetsService;
import com.bot.employeeTimeTracongBot.lang.En;
import com.bot.employeeTimeTracongBot.lang.Language;
import com.bot.employeeTimeTracongBot.lang.Ua;
import com.bot.employeeTimeTracongBot.model.Building;
import com.bot.employeeTimeTracongBot.model.User;
import com.bot.employeeTimeTracongBot.transformer.SheetsTransformer;
import keys.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class TimeTrackingBot extends TelegramLongPollingBot {
    private final Response response = new Response();
    private final SheetsTransformer transformer = new SheetsTransformer();
    private SheetsService sheetsService = new SheetsService();
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    Language languageBot = new En();
    private List<Building> buildingList = new ArrayList<>();


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = String.valueOf(update.getMessage().getChatId());

            User user = registration(update);
            assert user != null;
            User userFromTable = sheetsService.readUserDataFromTableByFullName(user.getName());

            if (messageText.equals("/start") &&
                    checkAccess(user, userFromTable)) {
                logger.info("command -> /start");
                logger.info("chat user id from Table -> " + userFromTable.getChatId());

                setBotLanguage(update);

                logger.atInfo().log("Chat id: " + chatId);
                executeMessage(response
                        .sendMessage(languageBot.hello(), chatId));
                executeMessage(response
                        .sendMessage(userFromTable.getFullName() + languageBot.responseAboutHours(), chatId));
            }
            if (messageText.matches("^\\d+$")
                    && userFromTable.isSendReport()) {
                user.setHours(Integer.parseInt(messageText));
                user.setDateLastReport(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

                sheetsService.writeNext(SheetsName.REPORTS, "!A", "!A", new ArrayList<>(Arrays
                        .asList(LocalDateTime.now()
                                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm:ss")),
                                LocalDate.now()
                                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                                chatId,
                                userFromTable.getName(),
                                "",
                                messageText)));

                logger.info("send to google sheet: " + user.getName() + " " + user.getHours());
                executeMessage(response.sendMessage(userFromTable.getFullName() + languageBot.greatJob(), chatId));
            }
        }
    }

    private User registration(Update update) {
        String chatId = String.valueOf(update.getMessage().getChatId());
        String firstName = update.getMessage().getFrom().getFirstName();
        String lastName = update.getMessage().getFrom().getLastName();
        String nickName = update.getMessage().getFrom().getUserName();

        String name = firstName + " " + (lastName != null ? lastName : "");
        name = name.trim();
        User user = new User();
        logger.info(name);
        User userFromTable = sheetsService.readUserDataFromTableByFullName(name);
        if (userFromTable != null) {
            user.setName(name);
            logger.info("User from Table -> " + userFromTable);
            user.setChatId(Integer.parseInt(chatId));
            user.setFullName(userFromTable.getFullName());
            user.setNickName(nickName);
            user.setAccess(userFromTable.isAccess());
            user.setSendReport(userFromTable.isSendReport());
            user.setDateLastReport(null);
            sheetsService.writeNext(SheetsName.LOGS, "!A", "!A", transformer.transformToData(user));
            if (userFromTable.getDateConnecting() == null) {
                user.setDateConnecting(LocalDate.now().toString());
            } else {
                user.setDateConnecting(userFromTable.getDateConnecting());
            }
            sheetsService.writeToTable(SheetsName.USERS, user);
            logger.info("user from telegram ->" + user);
            return user;
        } else
            return null;
    }

    private boolean checkAccess(User user, User userFromTable) {
        return (userFromTable.getNickName() != null &&
                user.getNickName() != null &&
                user.getNickName().equals(userFromTable.getNickName())) ||
                (userFromTable.getChatId() != 0 &&
                        user.getChatId() != 0 &&
                        user.getChatId() == userFromTable.getChatId()) ||
                (userFromTable.getName() != null &&
                        user.getName() != null &&
                        user.getName().equals(userFromTable.getName())) &&
                        userFromTable.isAccess();
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

    @Scheduled(cron = "0 0 19 * * ?")
    public void sendDailyMessageToAllUsers() {
        logger.atInfo().log("start sending notification");
        // Отримайте список користувачів бота і відправте їм повідомлення
        // Для прикладу, ми відправляємо повідомлення за допомогою методу SendMessage

        // Отримайте список користувачів (ідентифікатори чатів) із вашої бази даних або іншого джерела

        // Переберіть всі ідентифікатори чатів і відправте їм повідомлення


        executeMessage(response.sendMessage(languageBot.responseAboutHours(), "809245011"));

        logger.atInfo().log("end sending notification");
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("exception when execute -> " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    @Override
    public String getBotUsername() {
        return Key.TELEGRAM_BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return Key.TELEGRAM_TOKEN;
    }
}