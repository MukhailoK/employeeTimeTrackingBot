package com.bot.employeeTimeTrackingBot;

import com.bot.employeeTimeTrackingBot.bot.TimeTrackingBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@SpringBootApplication
@EnableScheduling
public class EmployeeTimeTrackingBotApplication {

    public static void main(String[] args) throws TelegramApiException {
        ApplicationContext ctx = SpringApplication.run(EmployeeTimeTrackingBotApplication.class, args);
        TimeTrackingBot telegramBot = ctx.getBean(TimeTrackingBot.class);
        new TelegramBotsApi(DefaultBotSession.class).registerBot(telegramBot);
    }
}

