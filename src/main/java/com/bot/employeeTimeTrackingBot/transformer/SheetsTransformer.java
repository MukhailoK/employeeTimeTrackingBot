package com.bot.employeeTimeTrackingBot.transformer;

import com.bot.employeeTimeTrackingBot.model.User;

import java.util.Arrays;
import java.util.List;

public class SheetsTransformer {
    public List<Object> transformToData(User user) {
        return Arrays.asList(user.isAccess(),
                user.isSendReport(),
                user.getLocale(),
                user.getName(),
                user.getDateConnecting(),
                user.getChatId(),
                user.getNickName(),
                user.getFullName(),
                user.getDateLastReport(),
                user.getHours()
        );
    }

    public User transformToEntity(List<Object> cells) {
        User user = new User();
        user.setAccess(Boolean.parseBoolean(cells.get(0).toString()));
        user.setSendReport(Boolean.parseBoolean(cells.get(1).toString()));
        int size = cells.size();
        if (size >= 3) {
            user.setLocale(String.valueOf(cells.get(2)));
        }
        if (size >= 4) {
            user.setName((String) cells.get(3));
        }
        if (size >= 5) {
            user.setDateConnecting((String) cells.get(4));
        }
        if (size >= 6) {
            if (cells.get(5).toString().isBlank()) {
                user.setChatId(0);
            } else {
                user.setChatId(Long.parseLong(cells.get(5).toString()));
            }
        }
        if (size >= 7) {
            user.setNickName((String) cells.get(6));
        }
        if (size >= 8) {
            user.setFullName((String) cells.get(7));
        }
        if (size >= 9) {
            user.setDateLastReport(cells.get(8).toString());
        }
        if (size >= 10) {
            String numberStr = String.valueOf(cells.get(9));
            numberStr = numberStr.replace(',', '.');
            user.setHours(Double.parseDouble(numberStr));
        }

        return user;
    }

}