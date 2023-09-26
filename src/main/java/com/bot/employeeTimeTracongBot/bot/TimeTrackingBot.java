package com.bot.employeeTimeTracongBot.bot;

import com.bot.employeeTimeTracongBot.google.SheetsService;
import com.bot.employeeTimeTracongBot.lang.En;
import com.bot.employeeTimeTracongBot.lang.Language;
import com.bot.employeeTimeTracongBot.lang.Ua;
import com.bot.employeeTimeTracongBot.utils.KeyboardUtils;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
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

import java.io.IOException;
import java.util.*;

@Component
public class TimeTrackingBot extends TelegramLongPollingBot {
     SheetsService sheetsService = new SheetsService();
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    Map<Integer, Integer> data = new HashMap<>();
    Integer day = 1;
    Language languageBot;
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {

            String messageText = update.getMessage().getText();
            String chatId = String.valueOf(update.getMessage().getChatId());
            String firstName = update.getMessage().getFrom().getFirstName();
            String lastName = update.getMessage().getFrom().getLastName();
            User user = new User();
            if (messageText.equals("/start")) {
                writeToTable();
                setBotLanguage(update);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setId(Long.valueOf(chatId));

                logger.atInfo().log("Chat id: " + chatId);

                sendMainMenu(chatId, languageBot);
                sendMessage(languageBot.hello(), chatId);
                sendMessage(firstName + languageBot.responseAboutHours(), chatId);

            } else if (messageText.equals(languageBot.myStats())) {
                Collection<Integer> values = data.values();
                int sum = 0;
                for (Integer s : values) {
                    sum += s;
                }
                if (sum<=0){
                    sendMessage("Message special for Dar'ya", languageBot.getLanguage());
                }else {
                    sendMessage(languageBot.getHoursMessage(sum), chatId);
                }
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
                logger.atInfo().log("\'" + update.getMessage().getText() + "\' is added to google table");
                System.out.println(data.size());
            }
        }
    }

    private void writeToTable() {
  Sheets sheets = sheetsService.sheetsService();
        Spreadsheet spreadsheets = null;
        try {
            spreadsheets = sheets.spreadsheets().create(new Spreadsheet()).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
// Отримання ID нової таблиці
        String spreadsheetId =spreadsheets.getSpreadsheetId();

        System.out.println("ID нової таблиці: " + spreadsheetId);
        String tbl = spreadsheets.getSpreadsheetUrl();
        System.out.println(tbl);
       List<Sheet> objects = spreadsheets.getSheets();
       System.out.println(objects.size());
//        String spreedSheetsId =
//        googleSheets.writeToGoogleSheet();

    }

    private void setBotLanguage(Update update) {
        String language = update.getMessage().getFrom().getLanguageCode();
        logger.atInfo().log("user language - " + language);
        languageBot = switch (language) {
            case "uk" -> new Ua();
            case "de" -> new En();
            default -> new En();
        };
        logger.atInfo().log("app language - " + languageBot.getLanguage());
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
        logger.atInfo().log("start sending notification");
        // Отримайте список користувачів бота і відправте їм повідомлення
        // Для прикладу, ми відправляємо повідомлення за допомогою методу SendMessage

        // Отримайте список користувачів (ідентифікатори чатів) із вашої бази даних або іншого джерела

        // Переберіть всі ідентифікатори чатів і відправте їм повідомлення


        sendMessage(languageBot.responseAboutHours(), "809245011");

        logger.atInfo().log("end sending notification");
    }

    private void sendMainMenu(String chatId, Language language) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setReplyMarkup(KeyboardUtils.getMainMenuKeyboard(language));
        message.setText(language.chose());

//         Налаштуйте клавіатуру з кнопками
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