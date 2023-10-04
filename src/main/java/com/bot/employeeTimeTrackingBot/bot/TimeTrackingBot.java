package com.bot.employeeTimeTrackingBot.bot;

import com.bot.employeeTimeTrackingBot.service.SheetsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import com.bot.employeeTimeTrackingBot.service.UserService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import com.bot.employeeTimeTrackingBot.data.SheetsName;
import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.User;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import org.slf4j.Logger;

import java.util.*;

import keys.Key;

@Component
public class TimeTrackingBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    private final SheetsService sheetsService = new SheetsService();
    private final UserService userService = new UserService();
    private final Map<Long, Long> chatIdMap = new HashMap<>();
    private final Response response = new Response();
    private Building choseBuilding = null;
    private Message message = null;

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String locale = update.getMessage().getFrom().getLanguageCode();
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId(); // Отримати початковий chatId
            message = update.getMessage();
            User userFromTable = sheetsService.readUserFromTableByChatId(chatId);
            User user = new User();
            user.setName(update.getMessage().getFrom().getFirstName() +
                    " " + update.getMessage().getFrom().getLastName());
            if (messageText.equals("/start")) {
                logger.info("command -> /start");
                executeMessage(response
                        .sendMessageWithButton(Long.parseLong(String.valueOf(chatId))
                                , getString("start", locale), getString("yes", locale), "yes"));
                chatIdMap.put(chatId, chatId); // Зберегти початковий chatId
                if (userFromTable.isSendReport()) {
                    sendMorningDailyMessageToAllUsers();
                }
            }

            if (messageText.equals("/stop")) {
                logger.info("command -> stop");
                userService.deleteUser(message);
            }
        }
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String locale = update.getCallbackQuery().getFrom().getLanguageCode();
            if ("yes".equals(callbackData)) {
                logger.info("button 'Yes' pushed");
                long chatId = chatIdMap.get(update.getCallbackQuery().getMessage().getChatId()); // Отримати початковий chatId
                if (!sheetsService.isPresent(chatId)) {
                    User user = userService.registration(message);
                    if (user != null) {
                        executeMessage(response
                                .sendRegistrationResponse(update));
                    } else {
                        executeMessage(response
                                .sendMessage(getString("error", locale), chatId));
                    }
                } else {
                    executeMessage(response
                            .sendMessage(getString("workplace_already_registered", locale), chatId));
                }
                executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            }
        }
        User userFromTable;
        if (update.hasCallbackQuery() && "first".equals(update.getCallbackQuery().getData())) {
            String locale = update.getCallbackQuery().getFrom().getLanguageCode();
            userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            List<List<InlineKeyboardButton>> rowsInLine = response.getRowsInLineWithBuildings();
            executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            executeMessage(response.sendListOfObjects(getString("workplace_select", locale), userFromTable.getChatId(), rowsInLine));
        }

        List<Building> buildings = sheetsService.getAllActualBuilding();
        for (Building building : buildings) {
            if (update.hasCallbackQuery() && building.getAddress().equals(update.getCallbackQuery().getData())) {
                choseBuilding = building;
                userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
                executeMessage(response.sendListOfObjects(
                        getString("you_pick", userFromTable.getLocale()) + update.getCallbackQuery().getData(),
                        Long.parseLong(String.valueOf(userFromTable.getChatId())),
                        response.getInterfaceMenu(update)
                ));
                executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            }
        }

        if (update.hasCallbackQuery() && "accept".equals(update.getCallbackQuery().getData()) && choseBuilding != null) {
            userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            sendFirstReportToTable(userFromTable, choseBuilding, update);

        }
        if (update.hasCallbackQuery() && "back".equals(update.getCallbackQuery().getData())) {
            executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            String locale = update.getCallbackQuery().getFrom().getLanguageCode();
            userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());

            List<List<InlineKeyboardButton>> rowsInLine = response.getRowsInLineWithBuildings();

            executeMessage(response.sendListOfObjects(getString("workplace_select", locale), userFromTable.getChatId(), rowsInLine));

        }

        if (update.hasCallbackQuery() && "second".equals(update.getCallbackQuery().getData())) {
            executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            userFromTable = sheetsService
                    .readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            executeMessage(response
                    .sendMessage(getString("work_hours_prompt", userFromTable.getLocale())
                            , userFromTable.getChatId()));
        }
        try {
            if (update.hasMessage() && (Double.parseDouble(update.getMessage().getText()) > 0)) {
                userFromTable = sheetsService
                        .readUserFromTableByChatId(update.getMessage().getChatId());

                boolean isSend = sheetsService
                        .updateReport(userFromTable.getChatId(),
                                Double.parseDouble(update.getMessage().getText()));
                if (isSend) {
                    executeMessage(response
                            .sendMessage(getString("report_sent",
                                            userFromTable.getLocale()) +
                                            sheetsService
                                                    .getTotalMouthHoursForUser(update.getMessage().getChatId()),
                                    userFromTable.getChatId()));
                } else {
                    executeMessage(response
                            .sendMessage(getString("work_hours_prompt", userFromTable.getLocale()), userFromTable.getChatId()));
                }
            } else if (update.hasCallbackQuery()) {

                executeMessage(response
                        .sendMessage(getString("work_hours_prompt", update.getCallbackQuery().getFrom().getLanguageCode())
                                , update.getCallbackQuery().getMessage().getChatId()));
            }
        } catch (NumberFormatException e) {
            logger.error("number format exception");
        }
        if (update.hasCallbackQuery() && "ignor".equals(update.getCallbackQuery().getData())) {
            userFromTable = sheetsService
                    .readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            message = update.getCallbackQuery().getMessage();
            executeMessage(response
                    .deleteLastBotMessage(message));
            executeMessage(response
                    .sendMessage(getString("have_a_nice_day", userFromTable.getLocale()),
                            message.getChatId()));
        }
        if (update.hasMessage() && "/first".equals(update.getMessage().getText())) {
            sendMorningDailyMessageToAllUsers();
        }
        if (update.hasMessage() && "/second".equals(update.getMessage().getText())) {
            sendDailyMessageToAllUsers();
        }
    }

    private void sendFirstReportToTable(User userFromTable, Building building, Update update) {
        sheetsService.writeNext(SheetsName.REPORTS, "!A", "!A", new ArrayList<>(Arrays
                .asList(LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                        "",
                        Long.parseLong(String.valueOf(userFromTable.getChatId())),
                        userFromTable.getName(),
                        building.getAddress(),
                        "")));
        executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
        executeMessage(response.sendMessage(getString("have_a_good_workday", userFromTable.getLocale()), userFromTable.getChatId()));

    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("exception when execute -> " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void executeMessage(DeleteMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("exception when delete bot message -> " + e.getMessage());
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

    private void buildIgnoreButton(User user, List<List<InlineKeyboardButton>> rowsInLine, InlineKeyboardButton button1) {
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        firstRow.add(button1);
        rowsInLine.add(firstRow);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(getString("ignor", user.getLocale()));
        button2.setCallbackData("ignor");
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        secondRow.add(button2);
        rowsInLine.add(secondRow);
    }

    @Scheduled(cron = "0 0 5 * * ?")
    public void sendMorningDailyMessageToAllUsers() {
        logger.atInfo().log("start sending morning notification");
        logger.atInfo().log("Get all actual users from google sheet");
        List<User> userList = sheetsService.getAllActualUsers();

        for (User user : userList) {
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(getString("open_shift", user.getLocale()));
            button1.setCallbackData("first");
            buildIgnoreButton(user, rowsInLine, button1);

            String morningGreeting = getString("good_morning",
                    user.getName(),
                    Double.parseDouble(String.valueOf(sheetsService
                            .getTotalMouthHoursForUser(user.getChatId())))
                    , user.getLocale());
            executeMessage(response
                    .sendListOfObjects(morningGreeting,
                            user.getChatId(),
                            rowsInLine));
        }

        logger.atInfo().log("end sending notification");
    }

    @Scheduled(cron = "0 0 15 * * ?")
    public void sendDailyMessageToAllUsers() {
        logger.atInfo().log("start sending notification");
        logger.atInfo().log("Get all actual users from google sheet");
        List<User> userList = sheetsService.getAllActualUsers();

        for (User user : userList) {
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(getString("close_Shift", user.getLocale()));
            button1.setCallbackData("second");
            buildIgnoreButton(user, rowsInLine, button1);
            String eveningGreeting = getString("good_evening", user.getName(), Double.parseDouble(String.valueOf(sheetsService.getTotalMouthHoursForUser(user.getChatId()))), user.getLocale());

            executeMessage(response
                    .sendListOfObjects(eveningGreeting,
                            user.getChatId(),
                            rowsInLine));
        }

        logger.atInfo().log("end sending notification");
    }


    private String getString(String key, String name, double hours, String locale) {
        Locale userLocale = determineUserLocale(locale);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", userLocale);
        try {
            return String.format(resourceBundle.getString(key), name, hours);
        } catch (MissingResourceException e) {
            return "Message not found for key: " + key;
        }
    }

    private String getString(String key, String locale) {
        Locale userLocale = determineUserLocale(locale);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", userLocale);
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            return "Message not found for key: " + key;
        }
    }

    private Locale determineUserLocale(String locale) {
        return switch (locale) {
            case "uk" -> new Locale("uk");
            case "ru" -> new Locale("ru");
            case "de" -> new Locale("de");
            case "pl" -> new Locale("pl");
            case "ro" -> new Locale("ro");
            default -> new Locale("en");
        };
    }
}
