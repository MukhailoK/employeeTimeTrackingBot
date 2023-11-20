package com.bot.employeeTimeTrackingBot.bot;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.service.SheetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

import static com.bot.employeeTimeTrackingBot.bot.TimeTrackingBot.determineUserLocale;

@Component
public class BotResponseMapper {
    private final SheetsService sheetsService;

    @Autowired
    public BotResponseMapper(SheetsService sheetsService) {
        this.sheetsService = sheetsService;
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
        message.setChatId(String.valueOf(update.getMessage().getChatId()));
        message.setText(getString("registration_success", String.valueOf(determineUserLocale(update.getMessage().getFrom().getLanguageCode()))));
        return message;
    }

    public List<List<InlineKeyboardButton>> getRowsInLineWithBuildings() {
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

    public List<List<InlineKeyboardButton>> getInterfaceMenu(Update update) {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText(getString("accept", String.valueOf(determineUserLocale(update.getCallbackQuery().getFrom().getLanguageCode()))));
        button1.setCallbackData("accept");
        firstRow.add(button1);
        rowsInLine.add(firstRow);

        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText(getString("back", String.valueOf(determineUserLocale(update.getCallbackQuery().getFrom().getLanguageCode()))));
        button2.setCallbackData("back");
        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        secondRow.add(button2);
        rowsInLine.add(secondRow);
        return rowsInLine;
    }

    private String getString(String key, String locale) {
        Locale userLocale = determineUserLocale(locale);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", userLocale);
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            return "Message not found for key: " + key;
        }
    }

}
