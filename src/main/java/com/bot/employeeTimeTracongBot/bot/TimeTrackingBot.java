package com.bot.employeeTimeTracongBot.bot;


import com.bot.employeeTimeTracongBot.lang.En;
import com.bot.employeeTimeTracongBot.lang.Language;
import com.bot.employeeTimeTracongBot.lang.Ua;
import com.bot.employeeTimeTracongBot.utils.KeyboardUtils;
import keys.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
public class TimeTrackingBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    Set<String> listOfChatIds = new HashSet<>();
    Map<Integer, Integer> data = new HashMap<>();
    Integer day = 1;

    @Override
    public void onUpdateReceived(Update update) {
        String language = update.getMessage().getFrom().getLanguageCode();
        Language languageBot;
        logger.atInfo().log("user language - " + language);
        languageBot = switch (language) {
            case "uk" -> new Ua();
            case "de" -> new En();
            default -> new En();
        };
        logger.atInfo().log("app language - " + languageBot.getLanguage());


        String firstName = update.getMessage().getFrom().getFirstName();
        String lastName = update.getMessage().getFrom().getLastName();
        String chatId = String.valueOf(update.getMessage().getChatId());
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setId(Long.valueOf(chatId));

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            listOfChatIds.add(chatId);
            System.out.println(listOfChatIds.size());
            System.out.println(chatId);

            if (messageText.equals("/start")) {
                sendMainMenu(chatId, languageBot);
                sendMessage(languageBot.hello(), chatId);
                sendMessage(firstName + languageBot.responseAboutHours(), chatId);

            } else if (messageText.equals(languageBot.myStats())) {
                Collection<Integer> values = data.values();
                int sum = 0;
                for (Integer s : values) {
                    sum += s;
                }
                sendMessage(languageBot.getHoursMessage(sum), chatId);
                // Опрацьовуємо команду "Моя статистика"
                // Отримуємо дані з Google Таблиці і відправляємо їх користувачу
            } else {
                languageBot.buttonSettings();
                // Опрацьовуємо команду "Відправити"
                // Записуємо дані в Google Таблицю
            }

            if (messageText.matches("^\\d+$")) {
                data.put(day++, Integer.valueOf(messageText));
                sendMessage(firstName + " " + languageBot.greatJob(), chatId);
                System.out.println(update.getMessage().getText());
                System.out.println(data.size());
                System.out.println(data.size());
            }
        }
    }

    private void sendMessage(String message_, String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(message_);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0 0 19 * * ?")
    public void sendDailyMessageToAllUsers() {
        System.out.println("Метод викликається");
        // Отримайте список користувачів бота і відправте їм повідомлення
        // Для прикладу, ми відправляємо повідомлення за допомогою методу SendMessage

        // Отримайте список користувачів (ідентифікатори чатів) із вашої бази даних або іншого джерела

        // Переберіть всі ідентифікатори чатів і відправте їм повідомлення


        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId("809245011");
        sendMessage.setText("повідомлення кожні 5 секунд");
        System.out.println("повідомлення кожні 5 секунд");

        // Відправте повідомлення
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    private void sendMainMenu(String chatId, Language language) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(language.chose());
        message.setReplyMarkup(KeyboardUtils.getMainMenuKeyboard(language));
        // Налаштуйте клавіатуру з кнопками
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
