package com.bot.employeeTimeTrackingBot.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

public interface SheetRepository {
    void writeNext(String sheetsName, String colum, String columSearch, List<Object> rowData);

    int getLastRow(String sheetName);

}
