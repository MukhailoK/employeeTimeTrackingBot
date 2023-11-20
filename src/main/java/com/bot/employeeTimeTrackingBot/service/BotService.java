//package com.bot.employeeTimeTrackingBot.service;
//
//import com.bot.employeeTimeTrackingBot.bot.Response;
//import com.bot.employeeTimeTrackingBot.bot.TimeTrackingBot;
//import com.bot.employeeTimeTrackingBot.data.SheetsName;
//import com.bot.employeeTimeTrackingBot.model.Building;
//import com.bot.employeeTimeTrackingBot.model.User;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.telegram.telegrambots.bots.TelegramLongPollingBot;
//import org.telegram.telegrambots.meta.api.objects.Message;
//import org.telegram.telegrambots.meta.api.objects.Update;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//@Service
//public class BotService {
//    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
//    private final Map<Long, Long> chatIdMap = new HashMap<>();
//    private final SheetsService sheetsService;
//    private final UserService userService;
//    private Building choseBuilding = null;
//    private final Response response;
//    private Message message = null;
//
//
//    @Autowired
//    public BotService(SheetsService sheetsService, UserService userService, Response response) {
//        this.sheetsService = sheetsService;
//        this.userService = userService;
//        this.response = response;
//
//    }
//
//    public void startEveningDailyMessageByHand(Update update) {
//        if (update.hasMessage() && "/second".equals(update.getMessage().getText())) {
//            sendDailyMessageToAllUsers();
//        }
//    }
//
//    public void startMorningDailyMessageByHand(Update update) {
//        if (update.hasMessage() && "/first".equals(update.getMessage().getText())) {
//            sendMorningDailyMessageToAllUsers();
//        }
//    }
//
//    public void eveningReportsCatcher(Update update) {
//        User userFromTable;
//        if (update.hasCallbackQuery() && "second".equals(update.getCallbackQuery().getData())) {
//            executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
//            userFromTable = sheetsService
//                    .readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
//            executeMessage(response
//                    .sendMessage(getString("work_hours_prompt", userFromTable.getLocale())
//                            , userFromTable.getChatId()));
//        }
//        try {
//            if (update.hasMessage() && (Double.parseDouble(update.getMessage().getText()) > 0)) {
//                userFromTable = sheetsService
//                        .readUserFromTableByChatId(update.getMessage().getChatId());
//                userFromTable.setAccess(false);
//                boolean isSend = sheetsService
//                        .updateReport(userFromTable.getChatId(),
//                                Double.parseDouble(update.getMessage().getText()));
//                if (isSend) {
//                    executeMessage(response
//                            .sendMessage(getString("report_sent",
//                                            userFromTable.getLocale()) +
//                                            sheetsService
//                                                    .getTotalMouthHoursForUser(update.getMessage().getChatId()),
//                                    userFromTable.getChatId()));
//                } else {
//                    executeMessage(response
//                            .sendMessage(getString("work_hours_prompt", userFromTable.getLocale())
//                                    , userFromTable.getChatId()));
//                }
//            }
//        } catch (NumberFormatException e) {
//            logger.error("number format exception");
//        }
//        if (update.hasCallbackQuery() && "ignor".equals(update.getCallbackQuery().getData())) {
//            userFromTable = sheetsService
//                    .readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
//            message = update.getCallbackQuery().getMessage();
//            executeMessage(response
//                    .deleteLastBotMessage(message));
//            executeMessage(response
//                    .sendMessage(getString("have_a_nice_day", userFromTable.getLocale()),
//                            message.getChatId()));
//        }
//    }
//
//    public void morningReportsCatcher(Update update) {
//        User userFromTable;
//        if (update.hasCallbackQuery() && "first".equals(update.getCallbackQuery().getData())) {
//            userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
//            String locale = userFromTable.getLocale();
//            List<List<InlineKeyboardButton>> rowsInLine = response.getRowsInLineWithBuildings();
//            executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
//            executeMessage(response.sendListOfObjects(getString("workplace_select", locale)
//                    , userFromTable.getChatId(), rowsInLine));
//        }
//
//        List<Building> buildings = sheetsService.getAllActualBuilding();
//        for (Building building : buildings) {
//            if (update.hasCallbackQuery() && building.getAddress().equals(update.getCallbackQuery().getData())) {
//                choseBuilding = building;
//                userFromTable = sheetsService
//                        .readUserFromTableByChatId(update.getCallbackQuery()
//                                .getMessage()
//                                .getChatId());
//                executeMessage(response.sendListOfObjects(
//                        getString("you_pick", userFromTable.getLocale()) + update.getCallbackQuery().getData(),
//                        Long.parseLong(String.valueOf(userFromTable.getChatId())),
//                        response.getInterfaceMenu(update)
//                ));
//                executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
//            }
//        }
//
//        if (update.hasCallbackQuery() && "accept".equals(update.getCallbackQuery().getData()) && choseBuilding != null) {
//            userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
//            userFromTable.setAccess(true);
//            sendFirstReportToTable(userFromTable, choseBuilding, update);
//
//        }
//        if (update.hasCallbackQuery() && "back".equals(update.getCallbackQuery().getData())) {
//            executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
//            String locale = update.getCallbackQuery().getFrom().getLanguageCode();
//            userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
//
//            List<List<InlineKeyboardButton>> rowsInLine = response.getRowsInLineWithBuildings();
//
//            executeMessage(response.sendListOfObjects(getString("workplace_select", locale), userFromTable.getChatId(), rowsInLine));
//
//        }
//    }
//
//    public void registration(Update update) {
//        if (update.hasMessage() && update.getMessage().hasText()) {
//            String locale = update.getMessage().getFrom().getLanguageCode();
//            String messageText = update.getMessage().getText();
//            long chatId = update.getMessage().getChatId(); // Отримати початковий chatId
//            message = update.getMessage();
//            sheetsService.readUserFromTableByChatId(chatId);
//            User user = new User();
//            user.setName(update.getMessage().getFrom().getFirstName() +
//                    " " + update.getMessage().getFrom().getLastName());
//
//            //start bot (get first command /start)
//            if (messageText.equals("/start")) {
//                logger.info("command -> /start");
//                executeMessage(response
//                        .sendMessageWithButton(Long.parseLong(String.valueOf(chatId))
//                                , getString("start", locale), getString("yes", locale), "yes"));
//                chatIdMap.put(chatId, chatId); // Зберегти початковий chatId
//            }
//
//            //stop bot (get command /stop) and delete user data from google sheet by chat id
//            else if (messageText.equals("/stop")) {
//                logger.info("command -> stop");
//                userService.deleteUser(message.getChatId());
//            }
//        }
//
//        if (update.hasCallbackQuery()) {
//            String callbackData = update.getCallbackQuery().getData();
//            String locale = update.getCallbackQuery().getFrom().getLanguageCode();
//            if ("yes".equals(callbackData)) {
//                logger.info("button 'Yes' pushed");
//                long chatId = chatIdMap.get(update.getCallbackQuery().getMessage().getChatId()); // Отримати початковий chatId
//                if (!sheetsService.isPresent(chatId)) {
//                    User user = userService.registration(message);
//                    if (user != null) {
//                        executeMessage(response
//                                .sendRegistrationResponse(update));
//                    } else {
//                        executeMessage(response
//                                .sendMessage(getString("error", locale), chatId));
//                    }
//                } else {
//                    executeMessage(response
//                            .sendMessage(getString("workplace_already_registered", locale), chatId));
//                }
//                executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
//            }
//        }
//    }
//
//    private void sendFirstReportToTable(User userFromTable, Building building, Update update) {
//        sheetsService.writeNext(SheetsName.REPORTS, "!A", "!A", new ArrayList<>(Arrays
//                .asList(LocalDateTime.now()
//                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
//                        "",
//                        Long.parseLong(String.valueOf(userFromTable.getChatId())),
//                        userFromTable.getName(),
//                        building.getAddress(),
//                        "")));
//        executeMessage(response.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
//        executeMessage(response.sendMessage(getString("have_a_good_workday", userFromTable.getLocale()), userFromTable.getChatId()));
//
//    }
//
//
//
//    @Scheduled(cron = "0 0 5 * * ?")
//    public void sendMorningDailyMessageToAllUsers() {
//        logger.atInfo().log("start sending morning notification");
//        logger.atInfo().log("Get all actual users from google sheet");
//        List<User> userList = sheetsService.getAllActualUsers();
//
//        for (User user : userList) {
//            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
//
//            InlineKeyboardButton button1 = new InlineKeyboardButton();
//            button1.setText(getString("open_shift", user.getLocale()));
//            button1.setCallbackData("first");
//            buildIgnoreButton(user, rowsInLine, button1);
//
//            String morningGreeting = getString("good_morning",
//                    user.getName(),
//                    Double.parseDouble(String.valueOf(sheetsService
//                            .getTotalMouthHoursForUser(user.getChatId())))
//                    , user.getLocale());
//            executeMessage(response
//                    .sendListOfObjects(morningGreeting,
//                            user.getChatId(),
//                            rowsInLine));
//        }
//
//        logger.atInfo().log("end sending notification");
//    }
//
//    @Scheduled(cron = "0 0 15 * * ?")
//    public void sendDailyMessageToAllUsers() {
//        logger.atInfo().log("start sending notification");
//        logger.atInfo().log("Get all actual users from google sheet");
//        List<User> userList = sheetsService.getAllActualUsers().stream().filter(User::isAccess).toList();
//
//        for (User user : userList) {
//            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
//
//            InlineKeyboardButton button1 = new InlineKeyboardButton();
//            button1.setText(getString("close_Shift", user.getLocale()));
//            button1.setCallbackData("second");
//            buildIgnoreButton(user, rowsInLine, button1);
//            String eveningGreeting = getString("good_evening", user.getName(),
//                    Double.parseDouble(String.valueOf(sheetsService.getTotalMouthHoursForUser(user.getChatId()))),
//                    user.getLocale());
//
//            executeMessage(response
//                    .sendListOfObjects(eveningGreeting,
//                            user.getChatId(),
//                            rowsInLine));
//        }
//
//        logger.atInfo().log("end sending notification");
//    }
//
//    private void buildIgnoreButton(User user,
//                                   List<List<InlineKeyboardButton>> rowsInLine,
//                                   InlineKeyboardButton button1) {
//        List<InlineKeyboardButton> firstRow = new ArrayList<>();
//        firstRow.add(button1);
//        rowsInLine.add(firstRow);
//
//        InlineKeyboardButton button2 = new InlineKeyboardButton();
//        button2.setText(getString("ignor", user.getLocale()));
//        button2.setCallbackData("ignor");
//        List<InlineKeyboardButton> secondRow = new ArrayList<>();
//        secondRow.add(button2);
//        rowsInLine.add(secondRow);
//    }
//
//
//    private String getString(String key, String name, double hours, String locale) {
//        Locale userLocale = determineUserLocale(locale);
//        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages",
//                userLocale);
//        try {
//            return String.format(resourceBundle.getString(key), name, hours);
//        } catch (MissingResourceException e) {
//            return "Message not found for key: " + key;
//        }
//    }
//
//    private String getString(String key, String locale) {
//        Locale userLocale = determineUserLocale(locale);
//        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages",
//                userLocale);
//        try {
//            return resourceBundle.getString(key);
//        } catch (MissingResourceException e) {
//            return "Message not found for key: " + key;
//        }
//    }
//
//    private Locale determineUserLocale(String locale) {
//        return switch (locale) {
//            case "uk" -> new Locale("uk");
//            case "ru" -> new Locale("ru");
//            case "de" -> new Locale("de");
//            case "pl" -> new Locale("pl");
//            case "ro" -> new Locale("ro");
//            default -> new Locale("en");
//        };
//    }
//}
