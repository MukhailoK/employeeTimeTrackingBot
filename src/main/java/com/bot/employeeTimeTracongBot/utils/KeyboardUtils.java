package com.bot.employeeTimeTracongBot.utils;

import com.bot.employeeTimeTracongBot.lang.Language;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardUtils {

    public static ReplyKeyboardMarkup getMainMenuKeyboard(Language language) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setSelective(true);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        // Додайте кнопку "Моя статистика"
        row.add(new KeyboardButton(language.myStats()));

        // Додайте кнопку "Відправити"
        row.add(new KeyboardButton(language.buttonSettings()));

        // Додайте рядок кнопок до клавіатури
        keyboard.add(row);

        markup.setKeyboard(keyboard);

        return markup;
    }
}
