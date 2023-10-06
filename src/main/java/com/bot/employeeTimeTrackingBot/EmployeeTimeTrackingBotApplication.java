package com.bot.employeeTimeTrackingBot;

import com.bot.employeeTimeTrackingBot.bot.TimeTrackingBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@SpringBootApplication
@EnableScheduling
@PropertySource("classpath:application.properties")
public class EmployeeTimeTrackingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeTimeTrackingBotApplication.class, args);
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new TimeTrackingBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}

