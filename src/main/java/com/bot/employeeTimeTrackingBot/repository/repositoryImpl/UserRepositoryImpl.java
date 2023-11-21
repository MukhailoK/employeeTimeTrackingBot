package com.bot.employeeTimeTrackingBot.repository.repositoryImpl;

import com.bot.employeeTimeTrackingBot.data.SheetsName;
import com.bot.employeeTimeTrackingBot.model.User;
import com.bot.employeeTimeTrackingBot.repository.UserRepository;
import com.bot.employeeTimeTrackingBot.service.SheetsService;
import com.bot.employeeTimeTrackingBot.transformer.SheetsMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.bot.employeeTimeTrackingBot.data.SheetsName.USERS;
import static com.bot.employeeTimeTrackingBot.data.SheetsName.USERS_RANGE;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final Sheets sheets;
    private final SheetsService sheetsService;
    @Value("${google.api.table.id}")
    private String tableId;

    @Autowired
    public UserRepositoryImpl(Sheets sheets, SheetsService sheetsService) {
        this.sheets = sheets;
        this.sheetsService = sheetsService;
    }

    @Override
    public List<User> getAllActualUsers() {
        String range = USERS + "!A2:J";
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, range).execute();
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
                usersList.add(SheetsMapper.transformToEntity(row));
            }
        }
        return usersList;
    }

    @Override
    public List<User> getAllWorkingUsers() {
        String range = USERS + "!A2:J";
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            return null; // Таблиця пуста або немає даних
        }
        List<User> usersList = new ArrayList<>();
        for (List<Object> row : values) {
            if (row.size() >= 2 && Boolean.parseBoolean(row.get(0).toString())) {
                usersList.add(SheetsMapper.transformToEntity(row));
            }
        }
        return usersList;
    }

    @Override
    public User create(User user) {
        sheetsService
                .writeNext(SheetsName.USERS, "!A", "!C",
                        SheetsMapper.transformToData(user));
        sheetsService
                .writeNext(SheetsName.LOGS, "!A", "!A",
                        SheetsMapper.transformToLog(user));
        return user;

    }

    @Override
    public void delete(long chatId) {
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, USERS_RANGE).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<List<Object>> values = response.getValues();
        if (values != null && !values.isEmpty()) {
            for (int i = 0; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row.size() >= 5) {
                    if (Long.parseLong(row.get(5).toString()) == chatId) {
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
                            sheets.spreadsheets().batchUpdate(tableId, batchUpdateRequest).execute();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            }
        }
    }

    @Override
    public boolean changeFlag(long chatId) {
        String range = USERS + "!A2:J";
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<List<Object>> values = response.getValues();
        if (values.isEmpty()) {
            return false; // Таблиця пуста або немає даних
        }

        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() >= 5 && Long.parseLong(row.get(5).toString()) == chatId) {
                User user = SheetsMapper.transformToEntity(row);
                user.setAccess(!user.isAccess());
                row = SheetsMapper.transformToData(user);
                range = "!A" + (2 + i); // Оновити комірку "Дата і час прийшов" у відповідному рядку
                String updateRange = USERS + range;
                ValueRange updateBody = new ValueRange().setValues(Collections.singletonList(row));
                UpdateValuesResponse result;
                try {
                    result = sheets.spreadsheets()
                            .values()
                            .update(tableId, updateRange, updateBody)
                            .setValueInputOption("RAW")
                            .execute();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                sheetsService.writeNext(SheetsName.LOGS, "!A", "!A",
                        new ArrayList<>(Collections.singleton(row.toString())));
                return !result.isEmpty();
            }
        }
        return false;
    }

    @Override
    public double getTotalMouthHoursForUser(long chatId) {
        String range = USERS + "!A2:J";
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values.isEmpty()) {
            return 0;
        }

        for (List<Object> row : values) {
            if (row.size() >= 5 && Long.parseLong(row.get(5).toString()) == chatId) {
                String numberString = String.valueOf(row.get(9));
                numberString = numberString.replace(',', '.');
                return Double.parseDouble(numberString);
            }
        }
        return 0;
    }


    @Override
    public Optional<User> readUserFromTableByChatId(long chatId) {
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, USERS_RANGE).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            return Optional.empty(); // Таблиця пуста або немає даних
        }
        for (List<Object> row : values) {
            if (row.size() >= 5) {
                if (Long.parseLong(row.get(5).toString()) == chatId) {
                    return Optional.of(SheetsMapper.transformToEntity(row));
                }
            }
        }
        return Optional.empty();
    }
}
