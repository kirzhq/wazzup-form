package ru.kirzhq.wazzup.service;

import ru.kirzhq.wazzup.exception.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import ru.kirzhq.wazzup.client.WazzupApiClient;
import ru.kirzhq.wazzup.dto.LoginResponse;
import ru.kirzhq.wazzup.dto.WazzupUser;

import java.util.Arrays;

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

        WazzupUser user = Arrays.stream(users)
                .filter(item -> item.phone() != null)
                .filter(item ->
                        normalizePhone(item.phone()).equals(normalizedPhone)
                )
                .findFirst()
                .orElseThrow(() ->
                        new EmployeeNotFoundException(
                                "Сотрудник с таким номером не найден"
                        )
                );

        return new LoginResponse(
                user.id(),
                user.name(),
                user.phone(),
                user.accountId()
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