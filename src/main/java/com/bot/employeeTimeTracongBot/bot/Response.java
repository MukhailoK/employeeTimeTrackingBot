package com.bot.employeeTimeTracongBot.bot;

import com.bot.employeeTimeTracongBot.lang.Language;
import com.bot.employeeTimeTracongBot.utils.KeyboardUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

public class Response {
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);


    public SendMessage sendMainMenu(String chatId, Language language) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setReplyMarkup(KeyboardUtils.getMainMenuKeyboard(language));
        message.setText(language.chose());

//         Налаштуйте клавіатуру з кнопками
        return message;
    }
    public SendMessage sendListOfObjects(String chatId, List<List<InlineKeyboardButton>> rowsInline, List<InlineKeyboardButton> rowInline) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Кнопки-посилання");
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
//        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
//        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton butt = new InlineKeyboardButton();
        butt.setText("Google");
        butt.setUrl("https://www.google.com");
        rowInline.add(butt);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        // Відправляємо повідомлення
       return message;
    }
    public SendMessage sendMessage(String message_, String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(message_);
      return message;
    }

}
