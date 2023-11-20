package com.bot.employeeTimeTrackingBot.repository;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.google.api.services.sheets.v4.SheetsRequest;

import java.util.List;

public interface BuildingRepository {
    List<Building> getAllBuildings();
    List<Building> getAllActualBuildings();
}
