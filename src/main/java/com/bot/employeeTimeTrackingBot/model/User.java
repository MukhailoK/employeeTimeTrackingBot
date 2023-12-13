package com.bot.employeeTimeTrackingBot.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class User {
    private boolean isWorking;
    private boolean isSendReport;
    private String name;
    private String dateConnecting;
    private long chatId;
    private String nickName;
    private String fullName;
    private String dateLastReport;
    private double hours;
    private String locale;
}
