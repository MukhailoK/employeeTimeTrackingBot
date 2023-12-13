package com.bot.employeeTimeTrackingBot.service;

import com.bot.employeeTimeTrackingBot.data.SheetsName;
import com.bot.employeeTimeTrackingBot.model.Report;
import com.bot.employeeTimeTrackingBot.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Location;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final SheetsService sheetsService;


    public ReportService(ReportRepository repository, SheetsService sheetsService) {
        this.reportRepository = repository;

        this.sheetsService = sheetsService;
    }


    public boolean updateReport(long chatId, double hours) {
        sheetsService.writeNext(SheetsName.LOGS, "!A", "!A",
                Collections.singletonList(new ArrayList<>().addAll(Arrays.asList(LocalDateTime.now(), "chatId = '" + chatId, "' send hours = '" + hours + "'"))));
        return reportRepository.updateReport(chatId, hours);
    }

    public boolean updateReport(Report report){
        return reportRepository.updateReport(report);
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
