package com.bot.employeeTimeTrackingBot.repository.repositoryImpl;

import com.bot.employeeTimeTrackingBot.data.SheetsName;
import com.bot.employeeTimeTrackingBot.repository.SheetRepository;
import com.bot.employeeTimeTrackingBot.service.SheetsService;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class SheetRepositoryImpl implements SheetRepository {
    @Value("${google.api.table.id}")
    private String tableId;

    private final Sheets sheets;

    public SheetRepositoryImpl(Sheets sheets) {
        this.sheets = sheets;
    }


    @Override
    public void writeNext(String sheetsName, String colum, String columSearch, List<Object> rowData) {
        String range = sheetsName + colum + (getLastRow(sheetsName + columSearch + columSearch.replace('!', ':')) + 1);
        ValueRange request = new ValueRange().setValues(Collections.singletonList(rowData)).setRange(range);
        BatchUpdateValuesRequest batchUpdateRequest = new BatchUpdateValuesRequest().setValueInputOption("RAW").setData(List.of(request));
        try {
            sheets.spreadsheets().values().batchUpdate(tableId, batchUpdateRequest).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getLastRow(String sheetName) {
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, sheetName).execute();
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

    @Override
    public boolean updateInto(List<Object> row, String updateRange, Sheets sheets, String tableId, SheetsService sheetsService) {
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