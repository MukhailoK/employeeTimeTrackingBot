package com.bot.employeeTimeTrackingBot.service;

import com.bot.employeeTimeTrackingBot.model.Report;
import com.bot.employeeTimeTrackingBot.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Location;

@Service
public class ReportService {
    private final ReportRepository reportRepository;


    public ReportService(ReportRepository repository) {
        this.reportRepository = repository;

    }


    public boolean updateReport(long chatId, double hours) {
        return reportRepository.updateReport(chatId, hours);
    }

    public String getUrl(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String query = latitude + "," + longitude;
        return "https://www.google.com/maps/search/?api=1&query=" + query;
    }

    public void sendFirstReport(Report report) {
        reportRepository.sendFirstReport(report);
    }
}
