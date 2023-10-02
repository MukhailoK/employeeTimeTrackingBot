package com.bot.employeeTimeTracongBot.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class User {
    @Setter
    private boolean access;
    @Setter
    private boolean isSendReport;
    private String name;
    private String dateConnecting;
    private long chatId;
    private String nickName;
    private String fullName;
    private String dateLastReport;
    private double hours;

    public User(boolean access, boolean isSendReport, String name, String dateConnecting, int chatId, String nickName, String fullName, String dateLastReport, double hours) {
        this.access = access;
        this.isSendReport = isSendReport;
        this.name = name;
        this.dateConnecting = dateConnecting;
        this.chatId = chatId;
        this.nickName = nickName;
        this.fullName = fullName;
        this.dateLastReport = dateLastReport;
        this.hours = hours;
    }
}
