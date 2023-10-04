package com.bot.employeeTimeTracongBot.bot;

import com.bot.employeeTimeTracongBot.model.Building;
import com.bot.employeeTimeTracongBot.service.SheetsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class Response {
    private static final Logger logger = LoggerFactory.getLogger(Response.class);
    private static final Locale defaultLocale = Locale.ENGLISH; // За замовчуванням використовуємо англійську мову
    private static final ResourceBundle defaultResourceBundle = ResourceBundle.getBundle("messages", defaultLocale);
    private static final ResourceBundle ukrainianResourceBundle = ResourceBundle.getBundle("messages", new Locale("uk"));
    private static final ResourceBundle russianResourceBundle = ResourceBundle.getBundle("messages", new Locale("ru"));
    SheetsService sheetsService = new SheetsService();

    public SendMessage sendListOfObjects(String message_, long chatId, List<List<InlineKeyboardButton>> rowsInline) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(message_);
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        return message;
    }

    public SendMessage sendMessage(String message_, long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(message_);
        return message;
    }

    public DeleteMessage deleteLastBotMessage(Message message) {
        if (message != null && message.getFrom().getIsBot()) {
            Long chatId = message.getChatId();
            Integer messageId = message.getMessageId();

            DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);

            return deleteMessage;
        }
        return null;
    }

    public SendMessage sendMessageWithButton(long chatId, String message_, String title, String resultPushButton) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(message_);
        message.setReplyMarkup(createButton(title, resultPushButton));
        return message;

    }

    public InlineKeyboardMarkup createButton(String title, String result) {
        InlineKeyboardButton registerButton = new InlineKeyboardButton();
        registerButton.setText(title);
        registerButton.setCallbackData(result);

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(registerButton);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        return markup;
    }

    public SendMessage sendRegistrationResponse(Update update) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(update.getMessage().getChatId()));
        message.setText(getString("registration_success", getLanguage(update)));
        return message;
    }

    public List<List<InlineKeyboardButton>> getRowsInLine() {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<Building> buildings = sheetsService.getAllActualBuilding();
        for (Building building : buildings) {
            List<InlineKeyboardButton> rowLine = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(building.getAddress());
            button.setCallbackData(building.getAddress());
            rowLine.add(button);
            rowsInLine.add(rowLine);
        }
        return rowsInLine;
    }

    private String getString(String key, Locale locale) {
        ResourceBundle resourceBundle = defaultResourceBundle;
        if (locale.equals(new Locale("uk"))) {
            resourceBundle = ukrainianResourceBundle;
        } else if (locale.equals(new Locale("ru"))) {
            resourceBundle = russianResourceBundle;
        }
        return resourceBundle.getString(key);
    }

    private Locale getLanguage(Update update) {
        if (update.getMessage() != null && update.getMessage().getFrom() != null && update.getMessage().getFrom().getLanguageCode() != null) {
            String languageCode = update.getMessage().getFrom().getLanguageCode();
            if ("uk".equals(languageCode)) {
                return new Locale("uk");
            } else if ("ru".equals(languageCode)) {
                return new Locale("ru");
            }
        }

        // За замовчуванням використовуємо англійську мову
        return Locale.ENGLISH;
    }
}
