package com.bot.employeeTimeTrackingBot.repository;

import com.bot.employeeTimeTrackingBot.model.Building;

import java.util.List;

public interface BuildingRepository {
    List<Building> getAllBuildings();

    List<Building> getAllActualBuildings();
}
