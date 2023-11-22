package com.bot.employeeTimeTrackingBot.bot;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.User;
import com.bot.employeeTimeTrackingBot.service.BuildingService;
import com.bot.employeeTimeTrackingBot.service.ReportService;
import com.bot.employeeTimeTrackingBot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class TimeTrackingBot extends TelegramLongPollingBot {
    private static final LocalTime TIME_START_SETTING_REPORT = LocalTime.of(18, 0);
    private static final LocalTime TIME_STOP_GETTING_REPORT = LocalTime.of(23, 59, 59);
    private static final String CHRONO_START_MORNING_NOTIFICATION = "0 0 7 * * ?";
    private static final String CHRONO_START_EVENING_NOTIFICATION = "0 0 17 * * ?";
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    private final UserService userService;
    private final ReportService reportService;
    private final BuildingService buildingService;
    private final BotResponseMapper botResponseMapper;
    Building choseBuilding = null;
    private Message message = null;

    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.bot.name}")
    private String name;


    @Autowired
    public TimeTrackingBot(UserService userService,
                           ReportService reportService,
                           BuildingService buildingService,
                           BotResponseMapper botResponseMapper) {
        this.userService = userService;
        this.reportService = reportService;
        this.buildingService = buildingService;
        this.botResponseMapper = botResponseMapper;
    }


    //Head bot method
    @Override
    public void onUpdateReceived(Update update) {
        registration(update);
        morningReportsCatcher(update);
        eveningReportsCatcher(update);
        startMorningDailyMessageByHand(update);
        startEveningDailyMessageByHand(update);
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }


    public void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("exception when execute -> " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void executeMessage(DeleteMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("exception when delete bot message -> " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void startEveningDailyMessageByHand(Update update) {
        if (update.hasMessage() && "/second".equals(update.getMessage().getText())) {
            sendDailyMessageToAllUsers();
        }
    }

    public void startMorningDailyMessageByHand(Update update) {
        if (update.hasMessage() && "/first".equals(update.getMessage().getText())) {
            sendMorningDailyMessageToAllUsers();
        }
    }

    public void eveningReportsCatcher(Update update) {
        User userFromTable;
        if (update.hasCallbackQuery() && "second".equals(update.getCallbackQuery().getData())) {
            userFromTable = userService
                    .readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            if (userFromTable.isAccess()) {
                executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
                executeMessage(botResponseMapper
                        .sendMessage(botResponseMapper.getString("work_hours_prompt", userFromTable.getLocale())
                                , userFromTable.getChatId()));
            }
        }
        try {
            if (update.hasMessage() && (Double.parseDouble(update.getMessage().getText()) > 0)) {
                userFromTable = userService
                        .readUserFromTableByChatId(update.getMessage().getChatId());
                if (userFromTable.isAccess()
                        && LocalTime.now().isBefore(TIME_STOP_GETTING_REPORT)
                        && LocalTime.now().isAfter(TIME_START_SETTING_REPORT)
                ) {
                    boolean isSend = reportService
                            .updateReport(userFromTable.getChatId(),
                                    Double.parseDouble(update.getMessage().getText()));
                    if (isSend) {
                        if (userService.changeFlag(update.getMessage().getChatId())) {
                            executeMessage(botResponseMapper
                                    .sendMessage(botResponseMapper.getString("report_sent",
                                                    userFromTable.getLocale()) +
                                                    userService.getTotalMouthHoursForUser(update.getMessage().getChatId()),
                                            userFromTable.getChatId()));
                            return;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            logger.info("IllegalArgumentException evening report");
            return;
        }

        if (update.hasCallbackQuery() && "ignor".equals(update.getCallbackQuery().getData())) {
            userFromTable = userService
                    .readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            message = update.getCallbackQuery().getMessage();
            executeMessage(botResponseMapper
                    .deleteLastBotMessage(message));
            executeMessage(botResponseMapper
                    .sendMessage(botResponseMapper.getString("have_a_nice_day", userFromTable.getLocale()),
                            message.getChatId()));
        }

    }

    public void morningReportsCatcher(Update update) {
        User userFromTable;
        List<Building> buildings = buildingService.getAllActualBuilding();

        if (update.hasCallbackQuery() && "first".equals(update.getCallbackQuery().getData())) {
            userFromTable = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            String locale = userFromTable.getLocale();
            List<List<InlineKeyboardButton>> rowsInLine = botResponseMapper.getRowsInLineWithBuildings(buildings);
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            executeMessage(botResponseMapper.sendListOfObjects(botResponseMapper.getString("workplace_select", locale)
                    , userFromTable.getChatId(), rowsInLine));
        }
        for (Building building : buildings) {
            if (update.hasCallbackQuery() && building.getAddress().equals(update.getCallbackQuery().getData())) {
                choseBuilding = building;
                userFromTable = userService
                        .readUserFromTableByChatId(update.getCallbackQuery()
                                .getMessage()
                                .getChatId());
                executeMessage(botResponseMapper.sendListOfObjects(
                        botResponseMapper.getString("you_pick",
                                userFromTable.getLocale()) + update.getCallbackQuery().getData(),
                        Long.parseLong(String.valueOf(userFromTable.getChatId())),
                        botResponseMapper.getInterfaceMenu(update)
                ));
                executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            }
        }
        if (update.hasCallbackQuery() && "accept".equals(update.getCallbackQuery().getData()) && choseBuilding != null) {
            userFromTable = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            if (!userFromTable.isAccess()) {
                if (userService.changeFlag(update.getCallbackQuery().getMessage().getChatId())) {
                    reportService.createFirstReport(userFromTable, choseBuilding);
                    executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
                    executeMessage(botResponseMapper.sendMessage(botResponseMapper.getString("have_a_good_workday",
                            userFromTable.getLocale()), userFromTable.getChatId()));
                    return;
                }
            }
        }
        if (update.hasCallbackQuery() && "back".equals(update.getCallbackQuery().getData())) {
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            String locale = update.getCallbackQuery().getFrom().getLanguageCode();
            userFromTable = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());

            List<List<InlineKeyboardButton>> rowsInLine = botResponseMapper.getRowsInLineWithBuildings(buildings);

            executeMessage(botResponseMapper.sendListOfObjects(botResponseMapper.getString("workplace_select", locale),
                    userFromTable.getChatId(), rowsInLine));

        }
    }

    public void registration(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String locale = update.getMessage().getFrom().getLanguageCode();
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            message = update.getMessage();
            User user = new User();
            user.setName(update.getMessage().getFrom().getFirstName() +
                    " " + update.getMessage().getFrom().getLastName());

            //start bot (get first command /start)
            if (messageText.equals("/start")) {
                logger.info("command -> /start");
                executeMessage(botResponseMapper
                        .sendMessageWithButton(Long.parseLong(String.valueOf(chatId)),
                                botResponseMapper.getString("start", locale),
                                botResponseMapper.getString("yes", locale),
                                "yes"));
            }

            //stop bot (get command /stop) and delete user data from google sheet by chat id
            else if (messageText.equals("/stop")) {
                logger.info("command -> stop");
                executeMessage(botResponseMapper.sendMessage("your data has been deleted", message.getChatId()));
                userService.deleteUser(message.getChatId());
                return;
            }
        }

        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String locale = update.getCallbackQuery().getFrom().getLanguageCode();
            if ("yes".equals(callbackData)) {
                logger.info("button 'Yes' pushed");
                long chatId = update.getCallbackQuery().getMessage().getChatId(); // Отримати початковий chatId
                if (!userService.isPresent(chatId)) {
                    User user = userService.registration(message);
                    if (user != null) {
                        executeMessage(botResponseMapper
                                .sendRegistrationResponse(update));
                    } else {
                        executeMessage(botResponseMapper
                                .sendMessage(botResponseMapper.getString("error", locale), chatId));
                    }
                } else {
                    executeMessage(botResponseMapper
                            .sendMessage(botResponseMapper.getString("workplace_already_registered", locale), chatId));
                }
                executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            }
        }
    }

    @Scheduled(cron = CHRONO_START_MORNING_NOTIFICATION, zone = "GMT+1:00")
    public void sendMorningDailyMessageToAllUsers() {
        logger.atInfo().log("start sending morning notification");
        logger.atInfo().log("Get all actual users from google sheet");
        List<User> userList = userService.getAllActualUsers();

        for (User user : userList) {
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(botResponseMapper.getString("open_shift", user.getLocale()));
            button1.setCallbackData("first");
            buildIgnoreButton(user, rowsInLine, button1);

            String morningGreeting = botResponseMapper.getString("good_morning",
                    user.getName(),
                    Double.parseDouble(String.valueOf(userService
                            .getTotalMouthHoursForUser(user.getChatId())))
                    , user.getLocale());
            executeMessage(botResponseMapper
                    .sendListOfObjects(morningGreeting,
                            user.getChatId(),
                            rowsInLine));
        }

        logger.atInfo().log("end sending notification");
    }

    @Scheduled(cron = CHRONO_START_EVENING_NOTIFICATION, zone = "GMT+1:00")
    public void sendDailyMessageToAllUsers() {
        logger.atInfo().log("start sending notification");
        logger.atInfo().log("Get all actual users from google sheet");
        List<User> userList = userService.getAllActualUsers().stream().filter(User::isAccess).toList();

        for (User user : userList) {
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(botResponseMapper.getString("close_Shift", user.getLocale()));
            button1.setCallbackData("second");
            buildIgnoreButton(user, rowsInLine, button1);
            String eveningGreeting = botResponseMapper.getString("good_evening", user.getName(),
                    Double.parseDouble(String.valueOf(userService.getTotalMouthHoursForUser(user.getChatId()))),
                    user.getLocale());

            executeMessage(botResponseMapper
                    .sendListOfObjects(eveningGreeting,
                            user.getChatId(),
                            rowsInLine));
        }

        logger.atInfo().log("end sending notification");
    }

    private void buildIgnoreButton(User user,
                                   List<List<InlineKeyboardButton>> rowsInLine,
                                   InlineKeyboardButton button1) {
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        firstRow.add(button1);
        rowsInLine.add(firstRow);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(botResponseMapper.getString("ignor", user.getLocale()));
        button2.setCallbackData("ignor");
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        secondRow.add(button2);
        rowsInLine.add(secondRow);
    }
}