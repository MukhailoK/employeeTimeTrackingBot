package com.bot.employeeTimeTracongBot.service;

import com.bot.employeeTimeTracongBot.bot.TimeTrackingBot;
import com.bot.employeeTimeTracongBot.data.SheetsName;
import com.bot.employeeTimeTracongBot.google.MySheets;
import com.bot.employeeTimeTracongBot.model.Building;
import com.bot.employeeTimeTracongBot.model.User;
import com.bot.employeeTimeTracongBot.transformer.SheetsTransformer;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

public class SheetsService extends MySheets {
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    SheetsTransformer sheetsTransformer = new SheetsTransformer();

    public User readUserFromTableByChatId(long chatId) {
        String range = "Користувачі!A2:I"; // Замініть "YourSheetName" на назву вашого аркуша та відповідні стовпці
        Sheets sheets = sheetsService();
        ValueRange response = null;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        logger.info("Values size -> " + values.size());
        if (values == null || values.isEmpty()) {
            return null; // Таблиця пуста або немає даних
        }

        for (List<Object> row : values) {
//         Перевіряємо, чи нікнейм співпадає з введеним нікнеймом
            if (row.size() >= 5) {
                logger.info("row -> " + row.get(4));
                logger.info("chatId -> " + chatId);
                if (Long.parseLong(row.get(4).toString()) == chatId) {
                    logger.info("Found -> " + chatId);
                    return sheetsTransformer.transformToEntity(row);
                }
            }
        }
        logger.info("not found -> " + chatId);
        return null;
    }

    public boolean writeNext(String sheetsName, String colum, String columSearch, List<Object> rowData) {
        // Initialize the Sheets service
        Sheets sheets = sheetsService();

        // Create a ValueRange object to hold the data you want to append

        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));

        // Define the range where you want to append the data
        String range = sheetsName + colum + (getLastRow(sheetsName + columSearch + columSearch.replace('!', ':')) + 1); // Append to the next empty row

        // Create the AppendValuesRequest

        ValueRange request = new ValueRange().setValues(Collections.singletonList(rowData)).setRange(range);

        // Create the BatchUpdateValuesRequest
        BatchUpdateValuesRequest batchUpdateRequest = new BatchUpdateValuesRequest().setValueInputOption("RAW").setData(List.of(request));

        // Execute the request to append the data
        try {
            BatchUpdateValuesResponse response = sheets.spreadsheets().values().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("Data appended successfully.");
        return true;
    }

    private int getLastRow(String sheetName) {
        // Get the last row with data in the sheet
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, sheetName).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            return 0; // No data in the sheet
        } else {
            return values.size();
        }
    }

    public int getIndexOfRow(String sheetName, String name) {
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, sheetName).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<List<Object>> values = response.getValues();
        for (int i = 0; i < values.size(); i++) {
            List<Object> objects = values.get(i);
            for (Object o : objects) {
                if (o.toString().equals(name)) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    public boolean writeToTable(String sheetName, User user) {
        Sheets sheets = sheetsService();
        try {
            // Перевірка, чи існує таблиця з таким spreadsheetId
            sheets.spreadsheets().get(spreadsheetId).execute();
        } catch (IOException e) {
            throw new RuntimeException("Помилка при отриманні таблиці: " + e.getMessage());
        }

        ValueRange body = new ValueRange().setValues(Collections.singletonList(sheetsTransformer.transformToData(user)));

        try {
            // Оновлення даних в таблиці
            UpdateValuesResponse result = sheets.spreadsheets()
                    .values()
                    .update(spreadsheetId,
                            sheetName + "!A" + getIndexOfRow(sheetName, user.getName()) + ":I",
                            body).setValueInputOption("RAW")
                    .execute();

            return result.isEmpty();
        } catch (IOException e) {
            throw new RuntimeException("Помилка при оновленні таблиці: " + e.getMessage());
        }
    }

    public List<Building> getAllActualBuilding() {
        String range = SheetsName.BUILDINGS + "!A2:B"; // Замініть "YourSheetName" на назву вашого аркуша та відповідні стовпці
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        logger.info("Values size -> " + values.size());
        if (values == null || values.isEmpty()) {
            return null; // Таблиця пуста або немає даних
        }
        List<Building> buildingList = new ArrayList<>();
        for (List<Object> row : values) {
            if (Boolean.parseBoolean(row.get(1).toString())) {
                buildingList.add(new Building((String) row.get(0), Boolean.parseBoolean(row.get(1).toString())));
                logger.info("Add -> " + row.get(0));
            }
        }
        return buildingList;
    }

    public List<User> getAllActualUsers() {
        String range = SheetsName.USERS + "!A2:I";
        Sheets sheets = sheetsService();
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        logger.info("Values size -> " + values.size());
        if (values == null || values.isEmpty()) {
            return null; // Таблиця пуста або немає даних
        }
        List<User> usersList = new ArrayList<>();
        for (List<Object> row : values) {
            if (row.size() >= 2 && Boolean.parseBoolean(row.get(1).toString())) {
                usersList.add(sheetsTransformer.transformToEntity(row));
                logger.info("Add -> " + row.get(2));
            }
        }
        return usersList;
    }

    public boolean isPresent(long chatId) {
        String range = SheetsName.USERS + "!A2:I";
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
            return false; // Таблиця пуста або немає даних
        }

        for (List<Object> row : values) {
            if (row.size() >= 5 && Long.parseLong(row.get(4).toString()) == chatId) {
                logger.info("Present chat Id -> " + row.get(4));
                return true;
            }
        }
        return false;
    }

    public int getTotalMouthHoursForUser(long chatId) {
        String range = SheetsName.USERS + "!A2:I";
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
            return 0; // Таблиця пуста або немає даних
        }

        for (List<Object> row : values) {
            if (row.size() >= 5 && Long.parseLong(row.get(4).toString()) == chatId) {
                logger.info("total Hours -> " + row.get(8));
                return Integer.parseInt(row.get(8).toString());
            }
        }
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
                    row.set(1, String.valueOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))));
                    row.add(5, hours);

                    // Оновити лише конкретний рядок у таблиці
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
