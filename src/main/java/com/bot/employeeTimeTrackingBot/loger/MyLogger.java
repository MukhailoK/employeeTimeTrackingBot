package com.bot.employeeTimeTrackingBot.loger;

import org.slf4j.Logger;

public class MyLogger {
    private final Logger logger;

    public MyLogger(Logger logger) {
        this.logger = logger;
    }


    public void startMethodLog(String methodName) {
        logger.info(methodName + " started");
    }

    public void endMethodLog(String methodName) {
        logger.info(methodName + " stopped");
    }

    public void exceptionLog(String when, String message) {
        logger.error("exception when " + when + "->" + message);
    }

    public void info(String message){
        logger.info(message);
    }
}
