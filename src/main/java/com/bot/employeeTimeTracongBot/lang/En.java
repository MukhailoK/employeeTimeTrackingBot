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
}