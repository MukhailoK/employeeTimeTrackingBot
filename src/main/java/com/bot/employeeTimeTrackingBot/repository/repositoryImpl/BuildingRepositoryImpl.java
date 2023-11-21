package com.bot.employeeTimeTrackingBot.repository.repositoryImpl;

import com.bot.employeeTimeTrackingBot.data.SheetsName;
import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.repository.BuildingRepository;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class BuildingRepositoryImpl implements BuildingRepository {
    private final Sheets sheets;
    @Value("${google.api.table.id}")
    private String tableId;

    public BuildingRepositoryImpl(Sheets sheets) {
        this.sheets = sheets;
    }

    @Override
    public List<Building> getAllBuildings() {
        return null;
    }

    @Override
    public List<Building> getAllActualBuildings() {
        String range = SheetsName.BUILDINGS + "!A2:B";
        ValueRange response;
        try {
            response = sheets.spreadsheets().values().get(tableId, range).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {

            throw new NullPointerException("Table is empty or no matches"); // Таблиця пуста або немає даних
        }
        List<Building> buildingList = new ArrayList<>();
        for (List<Object> row : values) {
            if (Boolean.parseBoolean(row.get(1).toString())) {
                buildingList.add(new Building((String) row.get(0), Boolean.parseBoolean(row.get(1).toString())));
            }
        }
        return buildingList;
    }
}
