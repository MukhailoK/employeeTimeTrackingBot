package com.bot.employeeTimeTrackingBot.service;

import com.bot.employeeTimeTrackingBot.model.Building;
import com.bot.employeeTimeTrackingBot.repository.BuildingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;

    @Autowired
    public BuildingService(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    public List<Building> getAllActualBuilding() {
        return buildingRepository.getAllActualBuildings();

    }
}
