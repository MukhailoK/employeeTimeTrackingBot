package com.bot.employeeTimeTracongBot.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MyMessage {
    private final MessageSource messageSource;

    @Autowired
    public MyMessage(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getGreetingMessage(String locale) {
        return messageSource.getMessage("greeting.message", null, new Locale(locale));
    }
    public String getRegistration(String locale){
        return messageSource.getMessage("registration.ok.message", null, new Locale(locale));
    }
}
