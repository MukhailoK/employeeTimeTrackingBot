package com.bot.employeeTimeTrackingBot.repository.repositoryImpl;

import com.bot.employeeTimeTrackingBot.data.SheetsName;
import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.Report;
import com.bot.employeeTimeTrackingBot.model.User;
import com.bot.employeeTimeTrackingBot.repository.ReportRepository;
import com.bot.employeeTimeTrackingBot.service.SheetsService;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.bot.employeeTimeTrackingBot.transformer.SheetsMapper.transformToData;

@Repository
public class ReportRepositoryImpl implements ReportRepository {
    private final Sheets sheets;
    private final SheetsService sheetsService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Value("${google.api.table.id}")
    private String tableId;

    @Autowired
    public ReportRepositoryImpl(Sheets sheets, SheetsService sheetsService) {
        this.sheets = sheets;
        this.sheetsService = sheetsService;
    }

    @Override
    public boolean updateReport(long chatId, double hours) {
        String range = SheetsName.REPORTS + "!A2:F";
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<List<Object>> values = response.getValues();
        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() >= 3 && Long.parseLong(row.get(2).toString()) == chatId) {
                if (row.get(1) == null || row.get(1).toString().isEmpty()) {
                    String range1 = "!A" + (2 + i);
                    String updateRange = SheetsName.REPORTS + range1;
                    row.set(2, Long.parseLong(String.valueOf(row.get(2))));
                    row.set(1, ZonedDateTime.
                            now(ZoneId.of("Europe/Berlin")).format(dateTimeFormatter));
                    row.add(5, hours);
                    return sheetsService.updateInto(row, updateRange, sheets, tableId);
                }
            }
        }
        return false;
    }

    @Override
    public boolean updateReport(Report report) {
        String range = SheetsName.REPORTS + "!A2:H";
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<List<Object>> values = response.getValues();
        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() >= 3 && Long.parseLong(row.get(2).toString()) == report.getChatId()) {
                if (row.get(1) == null || row.get(1).toString().isEmpty()) {
                    String range1 = "!A" + (2 + i);
                    String updateRange = SheetsName.REPORTS + range1;
                    row.set(1, ZonedDateTime.
                            now(ZoneId.of("Europe/Berlin")).format(dateTimeFormatter));
                    row.set(5, report.getHours());
                    row.add(7, report.getLastPlaceUrl());
                    return sheetsService.updateInto(row, updateRange, sheets, tableId);
                }
            }
        }
        return false;

    }

    @Override
    public void sendFirstReportToTable(User userFromTable, Building building) {
        sheetsService.writeNext(SheetsName.REPORTS, "!A", "!A", new ArrayList<>(Arrays
                .asList(ZonedDateTime.
                                now(ZoneId.of("Europe/Berlin")).format(dateTimeFormatter),
                        "",
                        Long.parseLong(String.valueOf(userFromTable.getChatId())),
                        userFromTable.getName(),
                        building.getAddress(),
                        "")));
    }

    @Override
    public void sendFirstReport(Report report) {
        sheetsService.writeNext(SheetsName.REPORTS, "!A", "!A", transformToData(report));
    }


}
