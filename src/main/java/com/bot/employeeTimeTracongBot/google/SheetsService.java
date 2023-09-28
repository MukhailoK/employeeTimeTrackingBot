package com.bot.employeeTimeTracongBot.google;

import com.google.api.services.sheets.v4.model.*;
import com.bot.employeeTimeTracongBot.bot.TimeTrackingBot;
import com.bot.employeeTimeTracongBot.model.User;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import keys.Key;
import com.bot.employeeTimeTracongBot.transformer.SheetsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SheetsService {
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    String spreadsheetId = Key.TABLE_ID;
    private static final String APPLICATION_NAME = "Telegram-bot";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final SheetsTransformer sheetsTransformer = new SheetsTransformer();
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
            Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/telegram-bot-399807-753fca5cb5d2.json";

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = SheetsService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public Sheets sheetsService() {
        return createSheetsService();
    }

    private static Sheets createSheetsService() {
        // Встановіть фабрику JSON та створіть об'єкт JSON-ключа
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        InputStream credentialsStream = SheetsService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleCredentials credentials;
        try {
            credentials = ServiceAccountCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Побудуйте об'єкт Google Sheets з обліковим записом служби
        try {
            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName("telegram bot")
                    .build();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public User readUserDataFromTableByFullName(String fullName) {
        String range = "Користувачі!A2:I"; // Замініть "YourSheetName" на назву вашого аркуша та відповідні стовпці
        Sheets sheetsService = createSheetsService();
        ValueRange response = null;
        try {
            response = sheetsService.spreadsheets().values()
                    .get(Key.TABLE_ID, range)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        logger.info("Values size -> " + values.size());
        if (values == null || values.isEmpty()) {
            return null; // Таблиця пуста або немає даних
        }

        for (List<Object> row : values) {
//         Перевіряємо, чи нікнейм співпадає з введеним нікнеймом
            if (row.size() >= 3 && fullName.equals(row.get(2))) {
                logger.info("Found -> " + fullName);
                return sheetsTransformer.transformToEntity(row);
            }
        }
        logger.info("not found -> " + fullName);
        return null;
    }

    public void writeNext(String sheetsName, String colum, String columSearch, List<Object> rowData) {
        // Initialize the Sheets service
        Sheets sheetsService = createSheetsService();

        // Create a ValueRange object to hold the data you want to append

        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowData));

        // Define the range where you want to append the data
        String range = sheetsName + colum + (getLastRow(sheetsName + columSearch + columSearch.replace('!', ':')) + 1); // Append to the next empty row

        // Create the AppendValuesRequest

        ValueRange request = new ValueRange()
                .setValues(Collections.singletonList(rowData))
                .setRange(range);

        // Create the BatchUpdateValuesRequest
        BatchUpdateValuesRequest batchUpdateRequest = new BatchUpdateValuesRequest()
                .setValueInputOption("RAW")
                .setData(List.of(request));

        // Execute the request to append the data
        try {
            BatchUpdateValuesResponse response = sheetsService.spreadsheets().values()
                    .batchUpdate(Key.TABLE_ID, batchUpdateRequest)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Data appended successfully.");
    }

    private static int getLastRow(String sheetName) {
        // Get the last row with data in the sheet
        Sheets sheetsService = createSheetsService();
        ValueRange response;
        try {
            response = sheetsService.spreadsheets().values()
                    .get(Key.TABLE_ID, sheetName)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            return 0; // No data in the sheet
        } else {
            return values.size();
        }
    }

    public int getIndexOfRow(String sheetName, String name) {
        Sheets sheetsService = createSheetsService();
        ValueRange response;
        try {
            response = sheetsService.spreadsheets()
                    .values().get(Key.TABLE_ID, sheetName)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<List<Object>> values = response.getValues();
        for (int i = 0; i < values.size(); i++) {
            List<Object> objects = values.get(i);
            for (Object o : objects) {
                if (o.toString().equals(name)) {
                    return i+1;
                }
            }
        }
        return -1;
    }

    public boolean writeToTable(String sheetName, User user) {
        Sheets sheetsService = createSheetsService();
        String spreadsheetId = Key.TABLE_ID;
        try {
            sheetsService.spreadsheets().get(spreadsheetId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ValueRange body = new ValueRange()
                .setValues(Collections.singletonList(sheetsTransformer.transformToData(user)));

        try {
            UpdateValuesResponse result = sheetsService.spreadsheets().values()
                    .update(spreadsheetId, sheetName + "!A" + getIndexOfRow(sheetName, user.getName()) +":I", body)
                    .setValueInputOption("RAW")
                    .execute();
            return result.isEmpty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
