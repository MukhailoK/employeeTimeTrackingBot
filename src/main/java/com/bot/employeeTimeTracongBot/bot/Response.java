package com.bot.employeeTimeTracongBot.bot;

import com.bot.employeeTimeTracongBot.lang.Language;
import com.bot.employeeTimeTracongBot.model.Building;
import com.bot.employeeTimeTracongBot.service.SheetsService;
import com.bot.employeeTimeTracongBot.utils.KeyboardUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class Response {
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    SheetsService sheetsService = new SheetsService();

    public SendMessage sendMainMenu(String chatId, Language language) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setReplyMarkup(KeyboardUtils.getMainMenuKeyboard(language));
        message.setText(language.chose());

//         Налаштуйте клавіатуру з кнопками
        return message;
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

    public DeleteMessage deleteLastBotMessage(Update update) {
        if (update.hasMessage() && update.getMessage().hasViaBot()) {
            Message botMessage = update.getMessage();
            Long chatId = botMessage.getChatId();
            Integer messageId = botMessage.getMessageId();

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

    public SendMessage sendRegistrationResponse(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Ви були успішно зареєстровані!");
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
}
