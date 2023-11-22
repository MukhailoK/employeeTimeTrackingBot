package unit;

import com.bot.employeeTimeTrackingBot.model.User;
import com.bot.employeeTimeTrackingBot.repository.UserRepository;
import com.bot.employeeTimeTrackingBot.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    public void testGetAllActualUsers() {
        List<User> mockUsers = Arrays.asList(
                new User(true,
                        true,
                        "testName",
                        LocalDateTime.now().minusDays(10).toString(),
                        1234564L,
                        "testNickName",
                        "test fullName",
                        LocalDateTime.now().toString(),
                        10,
                        "uk"),
                new User(true,
                        true,
                        "testName2",
                        LocalDateTime.now().minusDays(10).toString(),
                        1234565L,
                        "testNickName2",
                        "test fullName2",
                        LocalDateTime.now().toString(),
                        10,
                        "en"));
        when(userRepository.getAllActualUsers()).thenReturn(mockUsers);


        List<User> result = userService.getAllActualUsers();


        assertEquals(mockUsers, result);

        verify(userRepository, times(1)).getAllActualUsers();
    }

    @Test
    public void testGetAllWorkingUsers() {
        List<User> mockUsers = Arrays.asList(new User(false,
                        false,
                        "testName",
                        LocalDateTime.now().minusDays(10).toString(),
                        1234564L,
                        "testNickName",
                        "test fullName",
                        LocalDateTime.now().toString(),
                        10,
                        "uk"),
                new User(true,
                        true,
                        "testName2",
                        LocalDateTime.now().minusDays(10).toString(),
                        1234565L,
                        "testNickName2",
                        "test fullName2",
                        LocalDateTime.now().toString(),
                        10,
                        "en"));
        when(userRepository.getAllWorkingUsers()).thenReturn(mockUsers);

        List<User> result = userService.getAllWorkingUsers();

        assertEquals(mockUsers, result);

        verify(userRepository, times(1)).getAllWorkingUsers();
    }
}
