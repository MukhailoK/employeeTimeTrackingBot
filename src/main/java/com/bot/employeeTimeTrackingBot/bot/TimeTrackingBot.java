package com.bot.employeeTimeTrackingBot.bot;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.Report;
import com.bot.employeeTimeTrackingBot.model.User;
import com.bot.employeeTimeTrackingBot.service.BuildingService;
import com.bot.employeeTimeTrackingBot.service.ReportService;
import com.bot.employeeTimeTrackingBot.service.UserService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bot.employeeTimeTrackingBot.bot.BotResponseMapper.ADDRESS_PREFIX;
import static com.bot.employeeTimeTrackingBot.bot.BotResponseMapper.USER_PREFIX;
import static com.bot.employeeTimeTrackingBot.data.Admin.ADMIN_CHAT_ID;
import static com.bot.employeeTimeTrackingBot.data.Admin.OWNER_CHAT_ID;

@Component
@Slf4j
public class TimeTrackingBot extends TelegramLongPollingBot {
    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.bot.name}")
    private String name;

    // private static final LocalTime TIME_START_SETTING_REPORT = LocalTime.of(18, 0);
    // private static final LocalTime TIME_STOP_GETTING_REPORT = LocalTime.of(23, 59, 59);
    private static final String CHRONO_START_MORNING_NOTIFICATION = "0 0 7 * * MON-SAT";
    private static final String CHRONO_START_SECOND_MORNING_NOTIFICATION = "0 10 7 * * MON-SAT";
    private static final String CHRONO_START_EVENING_NOTIFICATION = "0 30 17 * * MON-FRI";

    private static final String CHRONO_START_SATURDAY_AFTERNOON_NOTIFICATION = "0 0 14 * * SAT";
    private Map<Long, Report> reportDrafts = new HashMap<>();
    private Map<Long, Building> choseBuildings = new HashMap<>();
    private Map<Long, Message> messages = new HashMap<>();
    private final UserService userService;
    private final ReportService reportService;
    private final BuildingService buildingService;
    private final BotResponseMapper botResponseMapper;


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

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    //Head bot method

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        log.info("onUpdateReceived(Update update)");
        registration(update);
        adminCommandCatcher(update);
        morningReportsCatcher(update);
        eveningReportsCatcher(update);
        log.info("onUpdateReceived(Update update)");
    }

    public void deleteLastMessageAndRequestLocation(Update update) throws TelegramApiException {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        reportDrafts.put(chatId, new Report());
        User user = userService.readUserFromTableByChatId(chatId);
        executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(botResponseMapper.getString("transmit_location_qst", user.getLocale()));
        sendMessage.setReplyMarkup(botResponseMapper.createLocationRequestKeyboard(user.getLocale()));
        execute(sendMessage);
    }

    public void removeKeyboard(Update update) throws TelegramApiException {
        long chatId = update.getMessage().getChatId();
        reportDrafts.put(chatId, new Report());
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(botResponseMapper.createRemoveKeyboard());
        sendMessage.setText("☕");
        execute(sendMessage);
    }


    public void executeMessage(SendMessage message) throws TelegramApiException {

        log.info("executeMessage(SendMessage message)");
        execute(message);

        log.info("executeMessage(SendMessage message)");
    }

    public void executeMessage(DeleteMessage message) throws TelegramApiException {
        log.info("executeMessage(DeleteMessage message)");
        execute(message);

        log.info("executeMessage(DeleteMessage message)");
    }

    public void adminCommandCatcher(Update update) throws TelegramApiException {
        if (update.hasMessage() && (update.getMessage().getChatId() == ADMIN_CHAT_ID || update.getMessage().getChatId() == OWNER_CHAT_ID) && "/admin".equals(update.getMessage().getText())) {
            long chatId = update.getMessage().getChatId();
            String locale = userService.readUserFromTableByChatId(chatId).getLocale();
            log.info("adminCommandCatcher(Update update)");
            executeMessage(botResponseMapper.sendListOfObjects(
                    botResponseMapper.getString("hello_admin", locale), chatId, botResponseMapper.buildAdminMenu(update)));
            return;
        }
        if (update.hasCallbackQuery() && "/first".equals(update.getCallbackQuery().getData())) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String locale = userService.readUserFromTableByChatId(chatId).getLocale();
            log.info("admin initiated sending 'open report'");
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            executeMessage(botResponseMapper.sendMessage(
                    botResponseMapper.getString("respond_open_shift", locale), chatId));
            sendMorningDailyMessageToAllUsers();
            return;
        }
        if (update.hasCallbackQuery() && "/second".equals(update.getCallbackQuery().getData())) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String locale = userService.readUserFromTableByChatId(chatId).getLocale();
            log.info("admin initiated sending 'close report'");
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            executeMessage(botResponseMapper.sendMessage(
                    botResponseMapper.getString("respond_close_shift", locale), chatId));
            sendDailyMessageToAllUsers();
            return;
        }

        if (update.hasCallbackQuery() && "workersList".equals(update.getCallbackQuery().getData())) {
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            sendAllUsers(update);
            return;
        }
        if (update.hasCallbackQuery() && "back_toUserList".equals(update.getCallbackQuery().getData())) {
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            sendAllUsers(update);
            return;
        }
        if (update.hasCallbackQuery() && "back_toAdminMenu".equals(update.getCallbackQuery().getData())) {
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String locale = userService.readUserFromTableByChatId(chatId).getLocale();
            executeMessage(botResponseMapper.sendListOfObjects(
                    botResponseMapper.getString("hello_admin", locale), chatId, botResponseMapper.buildAdminMenu(update)));
            return;
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData().startsWith("open")) {
            log.info(update.getCallbackQuery().getData());
            String response = update.getCallbackQuery().getData().replaceAll("^open", "").trim();
            long chatID = Long.parseLong(response);
            User user = userService.readUserFromTableByChatId(chatID);
            sendOpenShiftRequestToUser(user);
            return;

        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData().startsWith("close")) {
            log.info(update.getCallbackQuery().getData());
            String response = update.getCallbackQuery().getData().replaceAll("^close", "").trim();
            long chatID = Long.parseLong(response);
            User user = userService.readUserFromTableByChatId(chatID);
            sendCloseShiftRequestToUser(user);
            return;
        }

        if (update.hasCallbackQuery() && update.getCallbackQuery().getData().startsWith(USER_PREFIX)) {
            User user = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));

            executeMessage(botResponseMapper.sendListOfObjects(botResponseMapper.getString("user_select", user.getLocale() + user.getName())
                    , user.getChatId(), botResponseMapper.buildChosenUserMenu(update)));
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

        if (update.hasMessage() && update.getMessage().hasLocation() && !userService.readUserFromTableByChatId(update.getMessage().getChatId()).isWorking()) {
            removeKeyboard(update);

            reportDrafts.get(update.getMessage().getChatId())
                    .setFirstPlaceUrl(reportService.getUrl(update.getMessage()
                            .getLocation()));
            executeMessage(botResponseMapper.sendMessageWithButton(update.getMessage().getChatId(), "get list of objects", "list", "obj"));
            return;
        }
        if (update.hasCallbackQuery() && "obj".equals(update.getCallbackQuery().getData()) &&
                !userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId()).isWorking()) {
            executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
            List<Building> buildings = buildingService.getAllActualBuilding();
            sendAllBuildings(update, buildings);
            return;
        }
        if (update.hasCallbackQuery() && update.getCallbackQuery().getData().startsWith(ADDRESS_PREFIX)) {
            List<Building> buildings = buildingService.getAllActualBuilding();
            populateSelectedBuilding(update, buildings);
            return;
        }
        if (update.hasCallbackQuery() && "accept".equals(update.getCallbackQuery().getData()) &&
                choseBuildings.get(update.getCallbackQuery().getMessage().getChatId()) != null) {
            populateAndSubmitReport(update);
            return;
        }

        if (update.hasCallbackQuery() && "back".equals(update.getCallbackQuery().getData())) {
            List<Building> buildings = buildingService.getAllActualBuilding();
            returnToPreviousMenu(update, buildings);
        }
    }

    private void eveningReportsCatcher(Update update) throws Exception {
        if (update.hasCallbackQuery() && "second".equals(update.getCallbackQuery().getData())) {
            deleteLastMessageAndRequestLocation(update);
            return;
        }
        if (update.hasCallbackQuery() && "ignor".equals(update.getCallbackQuery().getData())) {
            sayBye(update);
            return;
        }
        if (update.hasMessage() && update.getMessage().hasLocation() && userService.readUserFromTableByChatId(update.getMessage().getChatId()).isWorking()) {
            removeKeyboard(update);
            responseCloseReport(update);
            reportDrafts.get(update.getMessage().getChatId()).setLastPlaceUrl(reportService.getUrl(update.getMessage().getLocation()));
            reportDrafts.get(update.getMessage().getChatId()).setChatId(update.getMessage().getChatId());
            return;
        }
        if (update.hasMessage() && update.getMessage().getText().length() <= 3 &&
                (Double.parseDouble(update.getMessage().getText()) > 0)) {
            reportDrafts.get(update.getMessage().getChatId()).setHours(Double.parseDouble(update.getMessage().getText()));
            updateReport(update);
        }
    }

    private void responseCloseReport(Update update) throws TelegramApiException {
        User userFromTable;
        log.info("eveningReportsCatcher(Update update)");
        userFromTable = userService
                .readUserFromTableByChatId(update.getMessage().getChatId());
        if (userFromTable.isWorking()) {
            executeMessage(botResponseMapper
                    .sendMessage(botResponseMapper.getString("work_hours_prompt", userFromTable.getLocale())
                            , userFromTable.getChatId()));
        }
    }

    private void sayBye(Update update) throws TelegramApiException {
        User userFromTable;
        log.info("eveningReportsCatcher(Update update)");
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        userFromTable = userService
                .readUserFromTableByChatId(chatId);
        messages.put(chatId, update.getCallbackQuery().getMessage());
        executeMessage(botResponseMapper
                .deleteLastBotMessage(messages.get(chatId)));
        executeMessage(botResponseMapper
                .sendMessage(botResponseMapper.getString("have_a_nice_day", userFromTable.getLocale()),
                        chatId));
        log.info("eveningReportsCatcher(Update update)");
    }

    private void updateReport(Update update) throws TelegramApiException {
        User userFromTable;
        userFromTable = userService
                .readUserFromTableByChatId(update.getMessage().getChatId());
        if (userFromTable.isWorking()
            //uncomment if u need a time window for sending reports
//                        && LocalTime.now().isBefore(TIME_STOP_GETTING_REPORT)
//                        && LocalTime.now().isAfter(TIME_START_SETTING_REPORT)
        ) {

            boolean isSend = reportService
                    .updateReport(reportDrafts.get(update.getMessage().getChatId()));
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
                    log.info("eveningReportsCatcher(Update update)");
                    reportDrafts.put(userFromTable.getChatId(), null);
                }
            } else {
                executeMessage(botResponseMapper
                        .sendMessage(botResponseMapper.getString("error",
                                        userFromTable.getLocale()),
                                userFromTable.getChatId()));
            }
        }
    }

    private void returnToPreviousMenu(Update update, List<Building> buildings) throws TelegramApiException {
        User userFromTable;
        executeMessage(botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage()));
        String locale = update.getCallbackQuery().getFrom().getLanguageCode();
        userFromTable = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());

        List<List<InlineKeyboardButton>> rowsInLine = botResponseMapper.getRowsInLineWithBuildings(buildings);

        executeMessage(botResponseMapper.sendListOfObjects(botResponseMapper.getString("workplace_select", locale),
                userFromTable.getChatId(), rowsInLine));
    }

    private void populateAndSubmitReport(Update update) throws TelegramApiException {
        User userFromTable;
        userFromTable = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!userFromTable.isWorking()) {
            if (userService.changeFlag(update.getCallbackQuery().getMessage().getChatId())) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                DeleteMessage deleteMessage = botResponseMapper.deleteLastBotMessage(update.getCallbackQuery().getMessage());
                executeMessage(deleteMessage);

                SendMessage sendMessage = botResponseMapper.sendMessage(botResponseMapper.getString("opening_work_day",
                        userFromTable.getLocale()), userFromTable.getChatId());

                executeMessage(sendMessage);

                reportDrafts.get(chatId).setUserName(userFromTable.getName());
                reportDrafts.get(chatId).setDateStart(ZonedDateTime.
                        now(ZoneId.of("Europe/Berlin")).format(dateTimeFormatter));
                reportDrafts.get(chatId).setBuilding(choseBuildings.get(chatId));
                reportDrafts.get(chatId).setChatId(userFromTable.getChatId());

                reportService.sendFirstReport(reportDrafts.get(chatId));

                sendMessage = botResponseMapper.sendMessage(botResponseMapper.getString("have_a_good_workday",
                        userFromTable.getLocale()), userFromTable.getChatId());

                executeMessage(sendMessage);

                log.info("morningReportsCatcher(Update update)");
            }
        }
    }

    private void populateSelectedBuilding(Update update, List<Building> buildings) throws TelegramApiException {
        User userFromTable;
        for (Building building : buildings) {
            if (update.hasCallbackQuery() && (ADDRESS_PREFIX + building.getAddress()).equals(update.getCallbackQuery().getData())) {
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                choseBuildings.put(chatId, building);
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


    private void sendAllBuildings(Update update, List<Building> buildings) throws TelegramApiException {
        User userFromTable = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());

        String locale = userFromTable.getLocale();
        List<List<InlineKeyboardButton>> rowsInLine = botResponseMapper.getRowsInLineWithBuildings(buildings);
        executeMessage(botResponseMapper.sendListOfObjects(botResponseMapper.getString("workplace_select", locale)
                , userFromTable.getChatId(), rowsInLine));
    }

    private void sendAllUsers(Update update) throws TelegramApiException {
        User userFromTable = userService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());

        String locale = userFromTable.getLocale();
        List<List<InlineKeyboardButton>> rowsInLine = botResponseMapper.getRowsInLineWithUsers(locale);
        executeMessage(botResponseMapper.sendListOfObjects(botResponseMapper.getString("user_select", locale)
                , userFromTable.getChatId(), rowsInLine));
    }

    private void registration(Update update) throws TelegramApiException {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String locale = update.getMessage().getFrom().getLanguageCode();
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            messages.put(chatId, update.getMessage());
            User user = new User();
            user.setName(update.getMessage().getFrom().getFirstName() +
                    " " + update.getMessage().getFrom().getLastName());

            //start bot (get first command /start)
            if (messageText.equals("/start")) {
                log.info(" registration(Update update)");
                log.info("command -> /start");
                executeMessage(botResponseMapper
                        .sendMessageWithButton(Long.parseLong(String.valueOf(chatId)),
                                botResponseMapper.getString("start", locale),
                                botResponseMapper.getString("yes", locale),
                                "yes"));
            }

            //stop bot (get command /stop) and delete user data from Google sheet by chat id
            else if (messageText.equals("/stop")) {
                log.info(" registration(Update update)");
                log.info("command -> stop");
                executeMessage(botResponseMapper.sendMessage("your data has been deleted", chatId));
                userService.deleteUser(chatId);
                log.info("registration(Update update)");
                return;
            }
        }

        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String locale = update.getCallbackQuery().getFrom().getLanguageCode();
            if ("yes".equals(callbackData)) {
                log.info("button 'Yes' pushed");
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                if (!userService.isPresent(chatId)) {
                    User user = userService.registration(messages.get(chatId));
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
                log.info("registration(Update update)");
            }
        }
    }

    @Scheduled(cron = CHRONO_START_MORNING_NOTIFICATION, zone = "Europe/Berlin")
    public void sendFirstMorningDailyMessageToAllUsers() throws TelegramApiException {
        sendMorningDailyMessageToAllUsers();
    }

    @Scheduled(cron = CHRONO_START_SECOND_MORNING_NOTIFICATION, zone = "Europe/Berlin")
    public void sendSecondMorningDailyMessageToAllUsers() throws TelegramApiException {
        sendMorningDailyMessageToAllUsers();
    }

    public void sendMorningDailyMessageToAllUsers() throws TelegramApiException {
        log.info("sendMorningDailyMessageToAllUsers()");
        log.info("Get all actual users from google sheet");
        List<User> userList = userService.getAllActualUsers().stream().filter(user -> !user.isWorking()).toList();

        for (User user : userList) {
            sendOpenShiftRequestToUser(user);
        }

        log.info("sendMorningDailyMessageToAllUsers()");
    }

    private void sendOpenShiftRequestToUser(User user) throws TelegramApiException {
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

    @Scheduled(cron = CHRONO_START_EVENING_NOTIFICATION, zone = "Europe/Berlin")
    public void sendEveningMessageMonByFri() throws TelegramApiException {
        sendDailyMessageToAllUsers();
    }

    @Scheduled(cron = CHRONO_START_SATURDAY_AFTERNOON_NOTIFICATION, zone = "Europe/Berlin")
    public void sendAfterNoonSatMessage() throws TelegramApiException {
        sendDailyMessageToAllUsers();
    }

    public void sendDailyMessageToAllUsers() throws TelegramApiException {
        log.info("sendDailyMessageToAllUsers()");
        log.info("Get all actual users from google sheet");
        List<User> userList = userService.getAllActualUsers().stream().filter(User::isWorking).toList();

        for (User user : userList) {
            sendCloseShiftRequestToUser(user);
        }
        log.info("sendDailyMessageToAllUsers()");
    }

    private void sendCloseShiftRequestToUser(User user) throws TelegramApiException {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(botResponseMapper.getString("close_shift", user.getLocale()));
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
}