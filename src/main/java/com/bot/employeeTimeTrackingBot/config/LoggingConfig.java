package com.bot.employeeTimeTrackingBot.config;

import com.bot.employeeTimeTrackingBot.data.SheetsName;
import com.bot.employeeTimeTrackingBot.service.SheetsService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


@Aspect
@Component
@Slf4j
public class LoggingConfig {

    private final SheetsService sheetsService;

    public LoggingConfig(SheetsService sheetsService) {
        this.sheetsService = sheetsService;
    }

    @Pointcut("execution(public * com.bot.employeeTimeTrackingBot.bot.*.*(..))")
    public void botLog() {

    }

    @Pointcut("execution(public * com.bot.employeeTimeTrackingBot.service.*.*(..))")
    public void serviceLog() {

    }

    @Before("serviceLog()")
    public void doBeforeServiceLog(JoinPoint joinPoint){
        log.info("RUN SERVICE: \n SERVICE_METHOD: {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
//        sheetsService.writeNext(SheetsName.LOGS, "!A", "!A",
//                Arrays.asList("RUN SERVICE: \n SERVICE_METHOD: {}.{}",
//                        joinPoint.getSignature().getDeclaringTypeName(),
//                        joinPoint.getSignature().getName()));
    }

    @AfterReturning(returning = "returnObject", pointcut = "botLog()")
    public void doAfterReturn(Object returnObject){
        if (log.isInfoEnabled()){
            log.info("RETURN VALUE: {} \n END REQUEST!", returnObject);
        }
    }
}
