package com.bot.employeeTimeTrackingBot.service;

import com.bot.employeeTimeTrackingBot.repository.SheetRepository;
import com.google.api.services.sheets.v4.Sheets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class SheetsService {
    private final SheetRepository sheetRepository;

    @Autowired
    public SheetsService(SheetRepository sheetRepository) {
        this.sheetRepository = sheetRepository;
    }

    public void writeNext(String sheetsName, String colum, String columSearch, List<Object> rowData) {
        sheetRepository.writeNext(sheetsName, colum, columSearch, rowData);
    }

    public int getLastRow(String sheetName) {
        return sheetRepository.getLastRow(sheetName);
    }

    public boolean updateInto(List<Object> row, String updateRange, Sheets sheets, String tableId, SheetsService sheetsService) {
        return sheetRepository.updateInto(row, updateRange, sheets, tableId, sheetsService);
    }

}
