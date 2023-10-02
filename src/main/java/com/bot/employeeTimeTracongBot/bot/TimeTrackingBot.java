package com.bot.employeeTimeTracongBot.bot;

import com.bot.employeeTimeTracongBot.service.SheetsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import com.bot.employeeTimeTracongBot.service.UserService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import com.bot.employeeTimeTracongBot.data.SheetsName;
import com.bot.employeeTimeTracongBot.model.Building;
import com.bot.employeeTimeTracongBot.model.User;
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
    private Map<Long, Long> chatIdMap = new HashMap<>();
    private final Response response = new Response();
    User user = new User();
    Message message = null;

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId(); // Отримати початковий chatId
            message = update.getMessage();

            user.setName(update.getMessage().getFrom().getFirstName() +
                    " " + update.getMessage().getFrom().getLastName());
            if (messageText.equals("/start")) {
                logger.info("command -> /start");
                executeMessage(response
                        .sendMessageWithButton(Long.parseLong(String.valueOf(chatId))
                                , "Зареєструватися?", "Так", "yes"));
                chatIdMap.put(chatId, chatId); // Зберегти початковий chatId
            }
        }
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();

            if ("yes".equals(callbackData)) {
                long chatId = chatIdMap.get(update.getCallbackQuery().getMessage().getChatId()); // Отримати початковий chatId
                if (!sheetsService.isPresent(chatId)) {
                    User user = userService.registration(message);
                    if (user != null) {
                        executeMessage(response
                                .sendRegistrationResponse(String.valueOf(chatId)));
                    } else {
                        executeMessage(response
                                .sendMessage("Помилка!", chatId));
                    }
                } else {
                    executeMessage(response
                            .sendMessage("Тебе вже зареєстровано!", chatId));
                }
            }
        }
        User userFromTable;

        if (update.hasCallbackQuery() && "first".equals(update.getCallbackQuery().getData())) {
            userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            List<List<InlineKeyboardButton>> rowsInLine = response.getRowsInLine();

            executeMessage(response.sendListOfObjects("Вибери об'єкт: ", userFromTable.getChatId(), rowsInLine));
        }
        List<Building> buildings = sheetsService.getAllActualBuilding();
        for (Building building : buildings) {
            if (update.hasCallbackQuery() && building.getAddress().equals(update.getCallbackQuery().getData())) {
                userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
                sheetsService.writeNext(SheetsName.REPORTS, "!A", "!A", new ArrayList<>(Arrays
                        .asList(LocalDateTime.now()
                                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                                "",
                                Long.parseLong(String.valueOf(userFromTable.getChatId())),
                                userFromTable.getName(),
                                building.getAddress(),
                                "")));
                executeMessage(response.sendMessage("Бажаю гарного робочого дня!", userFromTable.getChatId()));
            }
        }
        if (update.hasCallbackQuery() && "second".equals(update.getCallbackQuery().getData())) {

            userFromTable = sheetsService.readUserFromTableByChatId(update.getCallbackQuery().getMessage().getChatId());
            executeMessage(response
                    .sendMessage("Вкажи свої години"
                            , userFromTable.getChatId()));
        }
        if (update.hasMessage() && (Double.parseDouble(update.getMessage().getText()) > 0)) {
            userFromTable = sheetsService.readUserFromTableByChatId(update.getMessage().getChatId());
            boolean isSend = sheetsService.updateReport(userFromTable.getChatId(), Double.parseDouble(update.getMessage().getText()));
            if (isSend) {
                executeMessage(response.sendMessage("Звіт успішно відправлено! \nУ цьому місяці: " + sheetsService.getTotalMouthHoursForUser(update.getMessage().getChatId()), userFromTable.getChatId()));
            } else {
                executeMessage(response.sendMessage("Вкажи свої години, приклад: 10", userFromTable.getChatId()));
            }
        }
        if (update.hasCallbackQuery() && "ignor".equals(update.getCallbackQuery().getData())) {
            response.deleteLastBotMessage(update);
        }
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

    @Scheduled(cron = "0 0 17 * * ?")
    public void sendDailyMessageToAllUsers() {
        logger.atInfo().log("start sending notification");

        List<User> userList = sheetsService.getAllActualUsers();

        for (User user : userList) {

            executeMessage(response
                    .sendMessageWithButton(user.getChatId(),
                            "Доброго вечора, " + user.getName() + " пропоную закрити зміну \n " +
                                    "У цьому місяці: " + sheetsService.getTotalMouthHoursForUser(user.getChatId()) + " годин"
                            , "💪 Закрити"
                            , "second"));
            executeMessage(response.sendMessageWithButton(user.getChatId(),
                    "Якщо ти сьогодні не працюєш, можеш натиснути ігнор"
                    , "✋ Ігнор"
                    , "ignor"));

        }

        logger.atInfo().log("end sending notification");
    }

    @Scheduled(cron = "0 0 7 * * ?")
    public void sendMorningDailyMessageToAllUsers() {
        logger.atInfo().log("start sending morning notification");
        List<User> userList = sheetsService.getAllActualUsers();

        for (User user : userList) {

            executeMessage(response
                    .sendMessageWithButton(user.getChatId(),
                            "Доброго ранку, " + user.getName() + " пропоную відкрити зміну \n " +
                                    "У цьому місяці: " + sheetsService.getTotalMouthHoursForUser(user.getChatId()) + " годин"
                            , "💪 Відкрити"
                            , "first"));
            executeMessage(response.sendMessageWithButton(user.getChatId(),
                    "Якщо ти сьогодні не працюєш, можеш натиснути ігнор"
                    , "✋ Ігнор"
                    , "ignor"));

        }

        logger.atInfo().log("end sending notification");
    }
}