package com.bot.employeeTimeTrackingBot.service;

import com.bot.employeeTimeTrackingBot.bot.TimeTrackingBot;
import com.bot.employeeTimeTrackingBot.data.SheetsName;
import com.bot.employeeTimeTrackingBot.google.MySheets;
import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.model.User;
import com.bot.employeeTimeTrackingBot.transformer.SheetsTransformer;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import keys.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SheetsService extends MySheets {
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    SheetsTransformer sheetsTransformer = new SheetsTransformer();
    private static final String spreadsheetId = Key.TABLE_ID;

    public void deleteUserFromTableByChatId(long chatId) {
        String range = "Користувачі!A2:J"; // Замініть "YourSheetName" на назву вашого аркуша та відповідні стовпці
        Sheets sheets = sheetsService();
        ValueRange response = null;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            logger.info("Values size -> " + 0);
        } else
            for (int i = 0; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row.size() >= 5) {
                    if (Long.parseLong(row.get(5).toString()) == chatId) {
                        logger.info("Found -> " + chatId);
                        DeleteDimensionRequest deleteRequest = new DeleteDimensionRequest();
                        deleteRequest.setRange(new DimensionRange()
                                .setSheetId(0)
                                .setDimension("ROWS") // Видаляємо рядок.
                                .setStartIndex(i + 1) // З індекса 0 (нумерація починається з 0).
                                .setEndIndex(i + 2)); //
                        Request request = new Request()
                                .setDeleteDimension(deleteRequest);
                        List<Request> requests = new ArrayList<>();
                        requests.add(request);
                        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest().setRequests(requests);
                        try {
                            sheetsService().spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            }
    }

    public User readUserFromTableByChatId(long chatId) {
        String range = "Користувачі!A2:J"; // Замініть "YourSheetName" на назву вашого аркуша та відповідні стовпці
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            logger.info("values is null");
            return null; // Таблиця пуста або немає даних
        }
        for (List<Object> row : values) {
            if (row.size() >= 5) {
                if (Long.parseLong(row.get(5).toString()) == chatId) {
                    logger.info("Found -> " + chatId);
                    return sheetsTransformer.transformToEntity(row);
                }
            }
        }
        logger.info("not found -> " + chatId);
        return null;
    }

    public void writeNext(String sheetsName, String colum, String columSearch, List<Object> rowData) {
        logger.info("Method write netx started");
        Sheets sheets = sheetsService();
        String range = sheetsName + colum + (getLastRow(sheetsName + columSearch + columSearch.replace('!', ':')) + 1); // Append to the next empty row
        ValueRange request = new ValueRange().setValues(Collections.singletonList(rowData)).setRange(range);
        BatchUpdateValuesRequest batchUpdateRequest = new BatchUpdateValuesRequest().setValueInputOption("RAW").setData(List.of(request));
        try {
            sheets.spreadsheets().values().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("Data appended successfully.");
    }

    private int getLastRow(String sheetName) {
        logger.info("method getLastRow is started");
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, sheetName).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            logger.info("method getLastRow found any row");
            return 0; // No data in the sheet
        } else {
            logger.info("method getLastRow found " + values.size() + " row");
            return values.size();
        }
    }

    public List<Building> getAllActualBuilding() {
        logger.info("method getAllActualBuilding started");

        String range = SheetsName.BUILDINGS + "!A2:B"; // Замініть "YourSheetName" на назву вашого аркуша та відповідні стовпці
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            logger.info("method getAllActualBuilding found " + 0 + " actual buildings");

            return null; // Таблиця пуста або немає даних
        }
        List<Building> buildingList = new ArrayList<>();
        for (List<Object> row : values) {
            if (Boolean.parseBoolean(row.get(1).toString())) {
                buildingList.add(new Building((String) row.get(0), Boolean.parseBoolean(row.get(1).toString())));
            }
        }
        logger.info("method getAllActualBuilding found " + buildingList.size() + " actual buildings");
        return buildingList;
    }

    public List<User> getAllActualUsers() {
        logger.info("method getAllActualUsers returned is started");
        String range = SheetsName.USERS + "!A2:J";
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            return null; // Таблиця пуста або немає даних
        }
        List<User> usersList = new ArrayList<>();
        for (List<Object> row : values) {
            if (row.size() >= 2 && Boolean.parseBoolean(row.get(1).toString())) {
                usersList.add(sheetsTransformer.transformToEntity(row));
                logger.info("Add -> " + row.get(2));
                System.out.println(row.get(9));
            }
        }
        logger.info("method getAllActualUsers returned userList");
        return usersList;
    }

    public boolean isPresent(long chatId) {
        String range = SheetsName.USERS + "!A2:J";
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        logger.info("Values size -> " + values.size());
        if (values.isEmpty()) {
            logger.info("method isPresent returned empty values");
            return false; // Таблиця пуста або немає даних
        }

        for (List<Object> row : values) {
            if (row.size() >= 6 && Long.parseLong(row.get(5).toString()) == chatId) {
                logger.info("method isPresent returned value");
                return true;
            }
        }
        logger.info("method isPresent returned false");
        return false;
    }

    public double getTotalMouthHoursForUser(long chatId) {
        String range = SheetsName.USERS + "!A2:J";
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values.isEmpty()) {
            logger.info("method getTotalMouthHoursForUser returned empty values");
            return 0; // Таблиця пуста або немає даних
        }

        for (List<Object> row : values) {
            if (row.size() >= 5 && Long.parseLong(row.get(5).toString()) == chatId) {
                logger.info("method getTotalMouthHoursForUser returned value -> total hours: " + row.get(9));
                String numberString = String.valueOf(row.get(9));
                numberString = numberString.replace(',', '.');
                return Double.parseDouble(numberString);
            }
        }
        logger.info("method getTotalMouthHoursForUser returned empty values");
        return 0;
    }

    public boolean updateReport(long chatId, double hours) {
        String range = SheetsName.REPORTS + "!A2:I";
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<List<Object>> values = response.getValues();
        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() >= 3 && Long.parseLong(row.get(2).toString()) == chatId) {
                if (row.get(1) == null || row.get(1).toString().isEmpty()) {
                    String range1 = "!A" + (2 + i); // Оновити комірку "Дата і час прийшов" у відповідному рядку
                    String updateRange = SheetsName.REPORTS + range1;
                    row.set(2, Long.parseLong(String.valueOf(row.get(2))));
                    row.set(1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                    row.add(5, hours);
                    ValueRange updateBody = new ValueRange().setValues(Collections.singletonList(row));
                    UpdateValuesResponse result;
                    try {
                        result = sheets.spreadsheets()
                                .values()
                                .update(spreadsheetId, updateRange, updateBody)
                                .setValueInputOption("RAW")
                                .execute();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    logger.info("Report update is " + (result.isEmpty() ? "failed" : "successful"));
                    return !result.isEmpty();
                }
            }
        }
        logger.info("Report update failed");
        return false;
    }
}
