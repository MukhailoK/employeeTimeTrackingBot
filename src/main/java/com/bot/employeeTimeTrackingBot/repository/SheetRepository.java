package com.bot.employeeTimeTrackingBot.repository;

import com.bot.employeeTimeTrackingBot.service.SheetsService;
import com.google.api.services.sheets.v4.Sheets;

import java.util.List;

public interface SheetRepository {
    void writeNext(String sheetsName, String colum, String columSearch, List<Object> rowData);

    int getLastRow(String sheetName);

    boolean updateInto(List<Object> row, String updateRange, Sheets sheets, String tableId);
}
