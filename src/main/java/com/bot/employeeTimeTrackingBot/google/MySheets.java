package com.bot.employeeTimeTrackingBot.google;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import java.util.Collections;

public class MySheets {
    private static final String CREDENTIALS_FILE_PATH = "/telegram-bot-399807-753fca5cb5d2.json";

    public Sheets sheetsService() {
        return createSheetsService();
    }

    private static Sheets createSheetsService() {
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        InputStream credentialsStream = MySheets.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleCredentials credentials;
        try {
            assert credentialsStream != null;
            credentials = ServiceAccountCredentials.fromStream(credentialsStream).createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, new HttpCredentialsAdapter(credentials)).setApplicationName("telegram bot").build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}


