package com.bot.employeeTimeTrackingBot.config;

import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import com.bot.employeeTimeTrackingBot.service.SheetsService;
import org.springframework.context.annotation.Configuration;
import com.bot.employeeTimeTrackingBot.bot.TimeTrackingBot;
import com.bot.employeeTimeTrackingBot.service.UserService;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import com.bot.employeeTimeTrackingBot.bot.Response;
import org.springframework.context.annotation.Bean;
import lombok.SneakyThrows;

@Configuration
public class TelegramConfiguration {

    @Bean
    public TimeTrackingBot timeTrackingBot(SheetsService sheetsService, UserService userService, Response response) {
        return new TimeTrackingBot(sheetsService, userService, response);
    }

    @SneakyThrows
    @Bean
    public TelegramBotsApi botsApi(TimeTrackingBot trackingBot) {
        return new TelegramBotsApi(DefaultBotSession.class);
    }
}
