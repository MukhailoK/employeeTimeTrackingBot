package com.bot.employeeTimeTracongBot.lang;

public class Ua implements Language {

    @Override
    public String chose() {
        return "Оберіть дію:";
    }

    @Override
    public String hello() {
        return "Вітаю, я бот який створений " +
                "для записування та підрахунку" +
                " кількості робочих годин!";
    }

    @Override
    public String buttonSend() {
        return "Відправити";
    }

    @Override
    public String myStats() {
        return "Моя Статистика";
    }

    @Override
    public String responseAboutHours() {
        return ", вкажи скільки годин ти сьогодні працював";
    }
}
