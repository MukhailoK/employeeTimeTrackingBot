package com.bot.employeeTimeTrackingBot.transformer;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.Report;
import com.bot.employeeTimeTrackingBot.model.User;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SheetsMapper {

    public static List<Object> transformToData(User user) {
        return Arrays.asList(user.isAccess(),
                user.isSendReport(),
                user.getLocale() == null ? "" : user.getLocale(),
                user.getName() == null ? "" : user.getName(),
                user.getDateConnecting() == null ? "" : user.getDateConnecting(),
                user.getChatId() == 0L ? "" : user.getChatId(),
                user.getNickName() == null ? "" : user.getNickName(),
                user.getFullName() == null ? "" : user.getFullName()
        );
    }

    public static List<Object> transformToData(Report report) {
        return Arrays.asList(
                report.getDateStart() == null ? "" : report.getDateStart(),
                report.getDateEnd() == null ? "" : report.getDateEnd(),
                report.getChatId() == 0L ? "" : report.getChatId(),
                report.getUserName() == null ? "" : report.getUserName(),
                report.getBuilding().getAddress() == null ? "" : report.getBuilding().getAddress(),
                report.getHours() == 0 ? "" : report.getHours(),
                report.getPlaceUrl() == null ? "" : report.getPlaceUrl()
        );
    }

    public static Report transformToReportEntity(List<Object> cells) {
        Report report = new Report();
        report.setDateStart(String.valueOf(cells.get(0)));
        report.setDateEnd(String.valueOf(cells.get(1)));
        report.setChatId(Integer.parseInt(String.valueOf(cells.get(2))));
        report.setUserName(String.valueOf(cells.get(3)));
        report.setBuilding(new Building(String.valueOf(cells.get(4)), true));
        report.setHours(Double.parseDouble(String.valueOf(cells.get(5))));
        report.setPlaceUrl(String.valueOf(cells.get(6)));
        return report;
    }


    public static List<Object> transformToLog(User user) {
        return Collections.singletonList(user.toString());
    }

    public static User transformToEntity(List<Object> cells) {
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