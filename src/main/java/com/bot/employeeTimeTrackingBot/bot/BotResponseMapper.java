package com.bot.employeeTimeTrackingBot.bot;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;


@Component
public class BotResponseMapper {
    private final UserService userService;

    @Autowired
    public BotResponseMapper(UserService userService) {
        this.userService = userService;
    }

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
            return new DeleteMessage(chatId.toString(), messageId);
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
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        message.setChatId(String.valueOf(chatId));
        String locale = userService.readUserFromTableByChatId(chatId).getLocale();
        message.setText(getString("registration_success", locale));
        return message;
    }

    public List<List<InlineKeyboardButton>> getRowsInLineWithBuildings(List<Building> buildings) {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
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

    public List<List<InlineKeyboardButton>> getInterfaceMenu(Update update) {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        String locale = userService.readUserFromTableByChatId(chatId).getLocale();

        button1.setText(getString("accept", locale));
        button1.setCallbackData("accept");
        firstRow.add(button1);
        rowsInLine.add(firstRow);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(getString("back", locale));
        button2.setCallbackData("back");
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        secondRow.add(button2);
        rowsInLine.add(secondRow);
        return rowsInLine;
    }

    public String getString(String key, String locale) {
        Locale userLocale = determineUserLocale(locale);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", userLocale);
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            return "Message not found for key: " + key;
        }
    }

    public String getString(String key, String name, double hours, String locale) {
        Locale userLocale = determineUserLocale(locale);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages",
                userLocale);
        try {
            return String.format(resourceBundle.getString(key), name, hours);
        } catch (MissingResourceException e) {
            return "Message not found for key: " + key;
        }
    }

    public static Locale determineUserLocale(String locale) {
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
