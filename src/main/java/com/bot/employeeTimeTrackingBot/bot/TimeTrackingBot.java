package com.bot.employeeTimeTrackingBot.bot;

import com.bot.employeeTimeTrackingBot.loger.MyLogger;
import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.Report;
import com.bot.employeeTimeTrackingBot.model.User;
import com.bot.employeeTimeTrackingBot.service.BuildingService;
import com.bot.employeeTimeTrackingBot.service.ReportService;
import com.bot.employeeTimeTrackingBot.service.UserService;
import lombok.SneakyThrows;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bot.employeeTimeTrackingBot.bot.BotResponseMapper.ADDRESS_PREFIX;
import static com.bot.employeeTimeTrackingBot.data.Admin.ADMIN_CHAT_ID;

@Component
public class TimeTrackingBot extends TelegramLongPollingBot {
    // private static final LocalTime TIME_START_SETTING_REPORT = LocalTime.of(18, 0);
    // private static final LocalTime TIME_STOP_GETTING_REPORT = LocalTime.of(23, 59, 59);
    private static final String CHRONO_START_MORNING_NOTIFICATION = "0 0 7 * * ?";
    private static final String CHRONO_START_EVENING_NOTIFICATION = "0 0 17 * * ?";
    //    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    private Map<Long, Report> reportDrafts = new HashMap<>();
    private final UserService userService;
    private final ReportService reportService;
    private final BuildingService buildingService;
    private final BotResponseMapper botResponseMapper;
    private final MyLogger logger;
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
        this.logger = new MyLogger(LoggerFactory.getLogger(TimeTrackingBot.class));
    }


    //Head bot method
    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        logger.startMethodLog("onUpdateReceived(Update update)");
        registration(update);
        adminCommandCatcher(update);
        morningReportsCatcher(update);
        eveningReportsCatcher(update);
        logger.endMethodLog("onUpdateReceived(Update update)");
    }

    public void deleteLastMessageAndRequestLocation(Update update) throws TelegramApiException {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        reportDrafts.put(chatId, new Report());
        executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Дозвольте отримати ваше місцезнаходження?↙️↘️");
        sendMessage.setReplyMarkup(botResponseMapper.createLocationRequestKeyboard());
        execute(sendMessage);
    }

    public void removeKeyboard(Update update) throws TelegramApiException {
        long chatId = update.getMessage().getChatId();
        reportDrafts.put(chatId, new Report());
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Дякую!");
        sendMessage.setReplyMarkup(botResponseMapper.createRemoveKeyboard());
        execute(sendMessage);
    }

    @Override
    public String getBotUsername() {
        logger.startMethodLog("getBotUsername()");
        return name;
    }

    @Override
    public String getBotToken() {
        logger.startMethodLog("getBotToken()");
        return token;
    }


    public void executeMessage(SendMessage message) {
        try {
            logger.startMethodLog("executeMessage(SendMessage message)");
            execute(message);
        } catch (TelegramApiException e) {
            logger.exceptionLog("execute", e.getMessage());
            throw new RuntimeException(e);
        }
        logger.endMethodLog("executeMessage(SendMessage message)");
    }

    public void executeMessage(DeleteMessage message) {
        try {
            logger.startMethodLog("executeMessage(DeleteMessage message)");
            execute(message);
        } catch (TelegramApiException e) {
            logger.exceptionLog("delete bot message", e.getMessage());
            throw new RuntimeException(e);
        }
        logger.endMethodLog("executeMessage(DeleteMessage message)");
    }

    public void adminCommandCatcher(Update update) {
        if (update.hasMessage() && update.getMessage().getChatId() == ADMIN_CHAT_ID && "/admin".equals(update.getMessage().getText())) {
            long chatId = update.getMessage().getChatId();
            String locale = userService.readUserFromTableByChatId(chatId).getLocale();
            logger.startMethodLog("adminCommandCatcher(Update update)");
            executeMessage(botResponseMapper.sendListOfObjects(
                    botResponseMapper.getString("hello_admin", locale), chatId, botResponseMapper.getAdminMenu(update)));
            return;
        }
        if (update.hasCallbackQuery() && "/first".equals(update.getCallbackQuery().getData())) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String locale = userService.readUserFromTableByChatId(chatId).getLocale();
            logger.info("admin initiated sending 'open report'");
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            executeMessage(botResponseMapper.sendMessage(
                    botResponseMapper.getString("respond_open_shift", locale), chatId));
            sendMorningDailyMessageToAllUsers();
            return;
        }
        if (update.hasCallbackQuery() && "/second".equals(update.getCallbackQuery().getData())) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String locale = userService.readUserFromTableByChatId(chatId).getLocale();
            logger.info("admin initiated sending 'close report'");
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            executeMessage(botResponseMapper.sendMessage(
                    botResponseMapper.getString("respond_close_shift", locale), chatId));
            sendDailyMessageToAllUsers();
            return;
        }
        if (update.hasCallbackQuery() && "/exit".equals(update.getCallbackQuery().getData())) {
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
        }
    }

    public void morningReportsCatcher(Update update) throws TelegramApiException {
        if (update.hasCallbackQuery() && "first".equals(update.getCallbackQuery().getData())) {
            deleteLastMessageAndRequestLocation(update);
            return;
        }

        if (update.hasMessage() && update.getMessage().hasLocation()) {
            List<Building> buildings = buildingService.getAllActualBuilding();
            removeKeyboard(update);
            populateUrlWithLocation(update, buildings);
            return;
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData().startsWith(ADDRESS_PREFIX)) {
            List<Building> buildings = buildingService.getAllActualBuilding();
            populateSelectedBuilding(update, buildings);
            return;
        }
        if (update.hasCallbackQuery() && "accept".equals(update.getCallbackQuery().getData()) && choseBuilding != null) {
            populateAndSubmitReport(update);
            return;
        }

        if (update.hasCallbackQuery() && "back".equals(update.getCallbackQuery().getData())) {
            List<Building> buildings = buildingService.getAllActualBuilding();
            returnToPreviousMenu(update, buildings);
        }
    }

    private void eveningReportsCatcher(Update update) {
        if (update.hasCallbackQuery() && "second".equals(update.getCallbackQuery().getData())) {
            responseCloseReport(update);
            return;
        }
        if (update.hasCallbackQuery() && "ignor".equals(update.getCallbackQuery().getData())) {
            sayBye(update);
            return;
        }
        try {
            if (update.hasMessage() && update.getMessage().getText().length() <= 3 &&
                    (Double.parseDouble(update.getMessage().getText()) > 0)) {

                updateReport(update);
            }
        } catch (Exception e) {
            logger.exceptionLog("eveningReportsCatcher(Update update)", e.getMessage());
        }
    }

    private void responseCloseReport(Update update) {
        User userFromTable;
        logger.startMethodLog("eveningReportsCatcher(Update update)");
        userFromTable = userService
                .readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
        if (userFromTable.isAccess()) {
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            executeMessage(botResponseMapper
                    .sendMessage(botResponseMapper.getString("work_hours_prompt", userFromTable.getLocale())
                            , userFromTable.getChatId()));
        }
    }

    private void sayBye(Update update) {
        User userFromTable;
        logger.startMethodLog("eveningReportsCatcher(Update update)");
        userFromTable = userService
                .readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
        message = update.getCallbackQuery().getMessage();
        executeMessage(botResponseMapper
                .deleteLastBotMessage(message));
        executeMessage(botResponseMapper
                .sendMessage(botResponseMapper.getString("have_a_nice_day", userFromTable.getLocale()),
                        message.getChatId()));
        logger.endMethodLog("eveningReportsCatcher(Update update)");
    }

    private void updateReport(Update update) {
        User userFromTable;
        userFromTable = userService
                .readUserFromTableByChatId(update.getMessage().getChatId());
        if (userFromTable.isAccess()
            //uncomment if u need a time window for sending reports
//                        && LocalTime.now().isBefore(TIME_STOP_GETTING_REPORT)
//                        && LocalTime.now().isAfter(TIME_START_SETTING_REPORT)
        ) {
            boolean isSend = reportService
                    .updateReport(userFromTable.getChatId(),
                            Double.parseDouble(update.getMessage().getText()));
            executeMessage(botResponseMapper
                    .sendMessage(botResponseMapper.getString("sending",
                                    userFromTable.getLocale()),
                            userFromTable.getChatId()));
            if (isSend) {
                if (userService.changeFlag(update.getMessage().getChatId())) {
                    executeMessage(botResponseMapper
                            .sendMessage(botResponseMapper.getString("report_sent",
                                            userFromTable.getLocale()) +
                                            userService.getTotalMouthHoursForUser(update.getMessage().getChatId()),
                                    userFromTable.getChatId()));
                    logger.endMethodLog("eveningReportsCatcher(Update update)");
                }
            } else {
                executeMessage(botResponseMapper
                        .sendMessage(botResponseMapper.getString("error",
                                        userFromTable.getLocale()),
                                userFromTable.getChatId()));
            }
        }
    }

    private void returnToPreviousMenu(Update update, List<Building> buildings) {
        User userFromTable;
        executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
        String locale = update.getCallbackQuery().getFrom().getLanguageCode();
        userFromTable = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());

        List<List<InlineKeyboardButton>> rowsInLine = botResponseMapper.getRowsInLineWithBuildings(buildings);

        executeMessage(botResponseMapper.sendListOfObjects(botResponseMapper.getString("workplace_select", locale),
                userFromTable.getChatId(), rowsInLine));
    }

    private void populateAndSubmitReport(Update update) {
        User userFromTable;
        userFromTable = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!userFromTable.isAccess()) {
            if (userService.changeFlag(update.getCallbackQuery().getMessage().getChatId())) {
                DeleteMessage deleteMessage = botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage());
                executeMessage(deleteMessage);

                SendMessage sendMessage = botResponseMapper.sendMessage(botResponseMapper.getString("opening_work_day",
                        userFromTable.getLocale()), userFromTable.getChatId());

                executeMessage(sendMessage);

                reportDrafts.get(chatId).setUserName(userFromTable.getName());
                reportDrafts.get(chatId).setDateStart(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                reportDrafts.get(chatId).setBuilding(choseBuilding);
                reportDrafts.get(chatId).setChatId(userFromTable.getChatId());

                reportService.sendFirstReport(reportDrafts.get(chatId));

                sendMessage = botResponseMapper.sendMessage(botResponseMapper.getString("have_a_good_workday",
                        userFromTable.getLocale()), userFromTable.getChatId());

                executeMessage(sendMessage);

                logger.endMethodLog("morningReportsCatcher(Update update)");
            }
        }
    }

    private void populateSelectedBuilding(Update update, List<Building> buildings) {
        User userFromTable;
        for (Building building : buildings) {
            if (update.hasCallbackQuery() && (ADDRESS_PREFIX + building.getAddress()).equals(update.getCallbackQuery().getData())) {
                choseBuilding = building;
                userFromTable = userService
                        .readUserFromTableByChatId(update.getCallbackQuery()
                                .getMessage()
                                .getChatId());
                executeMessage(botResponseMapper.sendListOfObjects(
                        botResponseMapper.getString("you_pick",
                                userFromTable.getLocale()) + building.getAddress(),
                        Long.parseLong(String.valueOf(userFromTable.getChatId())),
                        botResponseMapper.getInterfaceMenu(update)
                ));
                executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            }
        }
    }

    private void populateUrlWithLocation(Update update, List<Building> buildings) {
        User userFromTable = userService.readUserFromTableByChatId(update.getMessage().getChatId());

        logger.startMethodLog("morningReportsCatcher(Update update)");

        String locale = userFromTable.getLocale();
        List<List<InlineKeyboardButton>> rowsInLine = botResponseMapper.getRowsInLineWithBuildings(buildings);
        executeMessage(botResponseMapper.sendListOfObjects(botResponseMapper.getString("workplace_select", locale)
                , userFromTable.getChatId(), rowsInLine));
        reportDrafts.get(update.getMessage().getChatId())
                .setPlaceUrl(reportService.getUrl(update.getMessage()
                        .getLocation()));

    }

    private void registration(Update update) {
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
                logger.startMethodLog(" registration(Update update)");
                logger.info("command -> /start");
                executeMessage(botResponseMapper
                        .sendMessageWithButton(Long.parseLong(String.valueOf(chatId)),
                                botResponseMapper.getString("start", locale),
                                botResponseMapper.getString("yes", locale),
                                "yes"));
            }

            //stop bot (get command /stop) and delete user data from Google sheet by chat id
            else if (messageText.equals("/stop")) {
                logger.startMethodLog(" registration(Update update)");
                logger.info("command -> stop");
                executeMessage(botResponseMapper.sendMessage("your data has been deleted", message.getChatId()));
                userService.deleteUser(message.getChatId());
                logger.endMethodLog("registration(Update update)");
                return;
            }
        }

        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String locale = update.getCallbackQuery().getFrom().getLanguageCode();
            if ("yes".equals(callbackData)) {
                logger.info("button 'Yes' pushed");
                long chatId = update.getCallbackQuery().getMessage().getChatId();
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
                logger.endMethodLog("registration(Update update)");
            }
        }
    }

    @Scheduled(cron = CHRONO_START_MORNING_NOTIFICATION, zone = "GMT+1:00")
    private void sendMorningDailyMessageToAllUsers() {
        logger.startMethodLog("sendMorningDailyMessageToAllUsers()");
        logger.info("Get all actual users from google sheet");
        List<User> userList = userService.getAllActualUsers().stream().filter(user -> !user.isAccess()).toList();

        for (User user : userList) {
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(botResponseMapper.getString("open_shift", user.getLocale()));
            button1.setCallbackData("first");
            botResponseMapper.buildIgnoreButton(user, rowsInLine, button1);

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

        logger.endMethodLog("sendMorningDailyMessageToAllUsers()");
    }

    @Scheduled(cron = CHRONO_START_EVENING_NOTIFICATION, zone = "GMT+1:00")
    private void sendDailyMessageToAllUsers() {
        logger.startMethodLog("sendDailyMessageToAllUsers()");
        logger.info("Get all actual users from google sheet");
        List<User> userList = userService.getAllActualUsers().stream().filter(User::isAccess).toList();

        for (User user : userList) {
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText(botResponseMapper.getString("close_Shift", user.getLocale()));
            button1.setCallbackData("second");
            botResponseMapper.buildIgnoreButton(user, rowsInLine, button1);
            String eveningGreeting = botResponseMapper.getString("good_evening", user.getName(),
                    Double.parseDouble(String.valueOf(userService.getTotalMouthHoursForUser(user.getChatId()))),
                    user.getLocale());

            executeMessage(botResponseMapper
                    .sendListOfObjects(eveningGreeting,
                            user.getChatId(),
                            rowsInLine));
        }
        logger.endMethodLog("sendDailyMessageToAllUsers()");
    }
}