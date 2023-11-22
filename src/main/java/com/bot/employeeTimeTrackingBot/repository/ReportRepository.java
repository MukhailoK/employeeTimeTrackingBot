package com.bot.employeeTimeTrackingBot.repository;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.User;

public interface ReportRepository {
    boolean updateReport(long chatId, double hours);

    void sendFirstReportToTable(User userFromTable, Building building);
}
