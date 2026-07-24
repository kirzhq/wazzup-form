package ru.kirzhq.wazzup.client;

import java.util.List;

import ru.kirzhq.wazzup.dto.WazzupContact;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.kirzhq.wazzup.dto.WazzupContactsResponse;
import ru.kirzhq.wazzup.dto.WazzupUser;
import ru.kirzhq.wazzup.exception.WazzupApiException;
import ru.kirzhq.wazzup.service.SettingsService;

@Component
public class WazzupApiClient {

    private final RestClient restClient;
    private final SettingsService settingsService;

    public WazzupApiClient(SettingsService settingsService) {
        this.settingsService = settingsService;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.wazzup24.com/v3")
                .build();
    }

    private String getBearerToken() {
        return "Bearer " + settingsService.getApiKey();
    }

    public WazzupUser[] getUsers() {
        try {
            return restClient.get()
                    .uri("/users")
                    .header("Authorization", getBearerToken())
                    .retrieve()
                    .body(WazzupUser[].class);
        } catch (RestClientException exception) {
            throw new WazzupApiException(
                    "Не удалось получить сотрудников из Wazzup",
                    exception
            );
        }
    }

    public WazzupUser getUserById(String userId) {
        try {
            WazzupUser user = restClient.get()
                    .uri("/users/{id}", userId)
                    .header("Authorization", getBearerToken())
                    .retrieve()
                    .body(WazzupUser.class);

            if (user == null) {
                throw new WazzupApiException(
                        "Wazzup вернул пустой ответ при получении сотрудника"
                );
            }

            return user;
        } catch (RestClientException exception) {
            throw new WazzupApiException(
                    "Не удалось получить сотрудника из Wazzup",
                    exception
            );
        }
    }

    public WazzupContactsResponse getContactsPage(int offset) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/contacts")
                            .queryParam("offset", offset)
                            .build())
                    .header("Authorization", getBearerToken())
                    .retrieve()
                    .body(WazzupContactsResponse.class);
        } catch (RestClientException exception) {
            throw new WazzupApiException(
                    "Не удалось получить контакты из Wazzup",
                    exception
            );
        }
    }

    public void saveContacts(List<WazzupContact> contacts) {
        try {
            restClient.post()
                    .uri("/contacts")
                    .header("Authorization", getBearerToken())
                    .body(contacts)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new WazzupApiException(
                    "Не удалось сохранить контакт в Wazzup",
                    exception
            );
        }
    }

    public WazzupContact getContactById(String contactId) {
        try {
            WazzupContact contact = restClient.get()
                    .uri("/contacts/{id}", contactId)
                    .header("Authorization", getBearerToken())
                    .retrieve()
                    .body(WazzupContact.class);

            if (contact == null) {
                throw new WazzupApiException(
                        "Wazzup вернул пустой ответ при получении контакта"
                );
            }

            return contact;
        } catch (RestClientException exception) {
            throw new WazzupApiException(
                    "Не удалось получить созданный контакт из Wazzup",
                    exception
            );
        }
    }

    public void deleteContact(String contactId) {
        try {
            restClient.delete()
                    .uri("/contacts/{id}", contactId)
                    .header("Authorization", getBearerToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new WazzupApiException(
                    "Не удалось удалить контакт из Wazzup",
                    exception
            );
        }
    }
}
