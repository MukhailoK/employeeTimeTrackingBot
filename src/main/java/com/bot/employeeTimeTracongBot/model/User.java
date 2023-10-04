package com.bot.employeeTimeTracongBot.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor
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
    private String locale;
}
