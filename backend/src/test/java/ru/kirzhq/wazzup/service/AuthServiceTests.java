package ru.kirzhq.wazzup.service;

import org.junit.jupiter.api.Test;
import ru.kirzhq.wazzup.client.WazzupApiClient;
import ru.kirzhq.wazzup.dto.LoginResponse;
import ru.kirzhq.wazzup.dto.WazzupUser;
import ru.kirzhq.wazzup.exception.EmployeeNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTests {

    private final WazzupApiClient client = mock(WazzupApiClient.class);
    private final AuthService service = new AuthService(client);

    @Test
    void loadsUserDetailsWhenListDoesNotContainPhone() {
        WazzupUser summary = new WazzupUser(null, "user-1", "Кирилл", null);
        WazzupUser details =
                new WazzupUser(10L, "user-1", "Кирилл", "79991234567");
        when(client.getUsers()).thenReturn(new WazzupUser[]{summary});
        when(client.getUserById("user-1")).thenReturn(details);

        LoginResponse response = service.login("+7 (999) 123-45-67");

        assertThat(response.id()).isEqualTo("user-1");
        assertThat(response.phone()).isEqualTo("79991234567");
        verify(client).getUserById("user-1");
    }

    @Test
    void rejectsPhoneThatDoesNotBelongToAnActiveUser() {
        WazzupUser user =
                new WazzupUser(null, "user-1", "Кирилл", "79991234567");
        when(client.getUsers()).thenReturn(new WazzupUser[]{user});

        assertThatThrownBy(() -> service.login("79990000000"))
                .isInstanceOf(EmployeeNotFoundException.class);
    }
}
