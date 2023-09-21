package com.bot.employeeTimeTracongBot.bot;


import com.bot.employeeTimeTracongBot.lang.En;
import com.bot.employeeTimeTracongBot.lang.Language;
import com.bot.employeeTimeTracongBot.lang.Ua;
import com.bot.employeeTimeTracongBot.utils.KeyboardUtils;
import keys.Key;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class TimeTrackingBot extends TelegramLongPollingBot {
    Map<Integer, Integer> data = new HashMap<>();
    Integer day = 1;

    @Override
    public void onUpdateReceived(Update update) {
        String language = update.getMessage().getFrom().getLanguageCode();
        Language languageBot;
        System.out.println(language);
        languageBot = switch (language) {
            case "uk" -> new Ua();
            case "de" -> new En();
            default -> new En();
        };

        System.out.println(languageBot.hello());

        String user = update.getMessage().getFrom().getFirstName();
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = String.valueOf(update.getMessage().getChatId());
            if (messageText.equals("/start")) {
                sendMessage(languageBot.hello(), chatId);
                sendMessage(user + languageBot.responseAboutHours(), chatId);

            } else if (messageText.equals(languageBot.myStats())) {
                Collection<Integer> values = data.values();
                int sum = 0;
                for (Integer s : values) {
                    sum += s;
                }
                sendMessage((user + ", ти напрацював " + sum + " годин"), chatId);
                // Опрацьовуємо команду "Моя статистика"
                // Отримуємо дані з Google Таблиці і відправляємо їх користувачу
            } else if (messageText.equals(languageBot.buttonSend())) {

                // Опрацьовуємо команду "Відправити"
                // Записуємо дані в Google Таблицю
            }


            data.put(day++, Integer.valueOf(messageText));
            sendMessage(user + ", ти молодець ", chatId);

            System.out.println(update.getMessage().getText());
            System.out.println(data.size());
            System.out.println(data.size());
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
