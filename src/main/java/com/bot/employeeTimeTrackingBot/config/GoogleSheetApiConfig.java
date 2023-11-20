package com.bot.employeeTimeTrackingBot.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleSheetApiConfig {

    @Value("${google.api.credentials.path}")
    private String credentialsPath;
    @Bean
    public Sheets sheets() {
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        InputStream credentialsStream = GoogleSheetApiConfig.class.getResourceAsStream(credentialsPath);
        GoogleCredentials credentials;
        try {
            assert credentialsStream != null;
            credentials = ServiceAccountCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials)).setApplicationName("telegram bot").build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
