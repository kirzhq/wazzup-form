package ru.kirzhq.wazzup.service;

import ru.kirzhq.wazzup.exception.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import ru.kirzhq.wazzup.client.WazzupApiClient;
import ru.kirzhq.wazzup.dto.LoginResponse;
import ru.kirzhq.wazzup.dto.WazzupUser;

@Service
public class AuthService {

    private final WazzupApiClient wazzupApiClient;

    public AuthService(WazzupApiClient wazzupApiClient) {
        this.wazzupApiClient = wazzupApiClient;
    }

    public LoginResponse login(String phone) {
        String normalizedPhone = normalizePhone(phone);

        WazzupUser[] users = wazzupApiClient.getUsers();

        if (users == null) {
            throw new IllegalStateException(
                    "Wazzup не вернул список сотрудников"
            );
        }

        WazzupUser user = findUserByPhone(users, normalizedPhone);

        return new LoginResponse(
                user.id(),
                user.name(),
                user.phone(),
                user.accountId()
        );
    }

    private WazzupUser findUserByPhone(
            WazzupUser[] users,
            String normalizedPhone
    ) {
        for (WazzupUser userSummary : users) {
            if (userSummary == null || userSummary.id() == null) {
                continue;
            }

            // В списке пользователей phone может отсутствовать.
            WazzupUser user = userSummary.phone() == null
                    ? wazzupApiClient.getUserById(userSummary.id())
                    : userSummary;

            if (user.phone() != null
                    && normalizePhone(user.phone()).equals(normalizedPhone)) {
                return user;
            }
        }

        throw new EmployeeNotFoundException(
                "Сотрудник с таким номером не найден"
        );
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");

        if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        }

        return digits;
    }
}
