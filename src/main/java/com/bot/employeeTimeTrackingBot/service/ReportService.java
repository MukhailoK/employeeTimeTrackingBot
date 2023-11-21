package com.bot.employeeTimeTrackingBot.service;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.User;
import com.bot.employeeTimeTrackingBot.repository.ReportRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ReportRepository reportRepository;

    public ReportService(ReportRepository repository) {
        this.reportRepository = repository;
    }

    public void createFirstReport(User userFromTable, Building building) {
        reportRepository.sendFirstReportToTable(userFromTable, building);
    }

    public boolean updateReport(long chatId, double hours) {
        return reportRepository.updateReport(chatId, hours);
    }
}
