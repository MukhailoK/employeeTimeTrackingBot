package com.bot.employeeTimeTrackingBot;

import com.bot.employeeTimeTrackingBot.bot.TimeTrackingBot;
import lombok.AllArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramBot;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.beans.beancontext.BeanContext;


@SpringBootApplication
@EnableScheduling
@PropertySource("classpath:application.properties")
public class EmployeeTimeTrackingBotApplication {

    public static void main(String[] args) throws TelegramApiException {
        ApplicationContext ctx = SpringApplication.run(EmployeeTimeTrackingBotApplication.class, args);
        TelegramBotsApi telegramBotsApi = ctx.getBean(TelegramBotsApi.class);
        TimeTrackingBot telegramBot = ctx.getBean(TimeTrackingBot.class);
        telegramBotsApi.registerBot(telegramBot);
    }
}

