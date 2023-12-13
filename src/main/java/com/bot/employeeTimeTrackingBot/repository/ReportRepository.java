package com.bot.employeeTimeTrackingBot.repository;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.Report;
import com.bot.employeeTimeTrackingBot.model.User;

import java.util.List;

import static com.bot.employeeTimeTrackingBot.transformer.SheetsMapper.transformToData;

public interface ReportRepository {
    boolean updateReport(long chatId, double hours);
    boolean updateReport(Report report);

    void sendFirstReportToTable(User userFromTable, Building building);

     void sendFirstReport(Report report);

}
