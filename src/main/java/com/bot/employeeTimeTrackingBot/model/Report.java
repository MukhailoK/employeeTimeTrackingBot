package com.bot.employeeTimeTrackingBot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Report {
    private String dateStart;
    private String dateEnd;
    private long chatId;
    private String userName;
    private Building building;
    private double hours;
    private String FirstPlaceUrl;
    private String LastPlaceUrl;
}
