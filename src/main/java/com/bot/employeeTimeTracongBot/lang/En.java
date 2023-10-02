package com.bot.employeeTimeTracongBot.lang;

public class En implements Language {
    @Override
    public String chose() {
        return "Chose action:";
    }

    @Override
    public String hello() {
        return "Hello, I'm a bot for writing and calculate work hours!";
    }

    @Override
    public String buttonSettings() {
        return "Settings";
    }

    @Override
    public String myStats() {
        return "My statistic";
    }

    @Override
    public String responseAboutHours() {
        return ", tell me how many hours did you work today";
    }

    @Override
    public String getLanguage() {
        return "en";
    }

    @Override
    public String getHoursMessage(int hours) {
        return "You have " + hours + " hours, nice Job!";
    }

    @Override
    public String greatJob() {
        return "Great job! " +
                "I'll write your hours";
    }
}