package com.bot.employeeTimeTrackingBot.service;

import com.bot.employeeTimeTrackingBot.bot.TimeTrackingBot;
import com.bot.employeeTimeTrackingBot.model.User;
import com.bot.employeeTimeTrackingBot.repository.UserRepository;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(TimeTrackingBot.class);
    private final UserRepository repository;

    @Autowired
    public UserService(UserRepository repository) {

        this.repository = repository;
    }

    public void deleteUser(long chatId) {
        repository.delete(chatId);
    }

    public User registration(Message message) {
        long chatId = message.getChatId();
        String firstName = message.getFrom().getFirstName();
        String lastName = message.getFrom().getLastName();
        String nickName = message.getFrom().getUserName();

        String name = firstName + " " + (lastName != null ? lastName : "");
        name = name.trim();
        User user = new User();
        logger.info("User name -> " + name);
        if (!isPresent(chatId)) {
            user.setName(name);
            user.setChatId(chatId);
            user.setNickName(nickName);
            user.setWorking(false);
            user.setSendReport(false);
            user.setDateConnecting(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            user.setLocale(message.getFrom().getLanguageCode());

            return repository.create(user);
        } else
            return null;
    }

    public User readUserFromTableByChatId(Long chatId) {
        Optional<User> user = repository.readUserFromTableByChatId(chatId);
        try {
            if (user.isPresent()) {
                return user.get();
            }
            throw new NotFoundException("user with chatId " + chatId + " not found");
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean changeFlag(Long chatId) {
        return repository.changeFlag(chatId);
    }

    public double getTotalMouthHoursForUser(Long chatId) {
        return repository.getTotalMouthHoursForUser(chatId);
    }

    public boolean isPresent(long chatId) {
        return repository.readUserFromTableByChatId(chatId).isPresent();
    }

    public List<User> getAllActualUsers() {
        return repository.getAllActualUsers();
    }

    public List<User> getAllWorkingUsers() {
        return repository.getAllWorkingUsers();
    }
}
