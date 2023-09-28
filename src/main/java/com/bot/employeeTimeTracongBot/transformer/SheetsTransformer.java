package com.bot.employeeTimeTracongBot.transformer;

import com.bot.employeeTimeTracongBot.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SheetsTransformer {
    public List<Object> transformToData(User user) {
        return Arrays.asList(user.isAccess(),
                user.isSendReport(),
                user.getName(),
                user.getDateConnecting(),
                user.getChatId(),
                user.getNickName(),
                user.getFullName(),
                user.getDateLastReport(),
                user.getHours());
    }

    public User transformToEntity(List<Object> cells) {
        User user = new User();
        user.setAccess(Boolean.parseBoolean(cells.get(0).toString()));
        user.setSendReport(Boolean.parseBoolean(cells.get(1).toString()));
        user.setName((String) cells.get(2));
        int size = cells.size();
        if (size >= 4) {
            user.setDateConnecting((String) cells.get(3));
        }
        if (size >= 5) {
            if (cells.get(4).toString().isBlank()) {
                user.setChatId(0);
            } else {
                user.setChatId(Integer.parseInt(cells.get(4).toString()));
            }
        }
        if (size >= 6) {
            user.setNickName((String) cells.get(5));
        }
        if (size >= 7) {
            user.setFullName((String) cells.get(6));
        }
        if (size >= 8) {
            user.setDateLastReport(cells.get(7).toString());
        }
        if (size >= 9) {
            user.setHours(Integer.parseInt(String.valueOf(cells.get(8))));
        }
        return user;
    }

}