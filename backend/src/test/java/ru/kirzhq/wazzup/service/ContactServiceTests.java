package ru.kirzhq.wazzup.service;

import org.junit.jupiter.api.Test;
import ru.kirzhq.wazzup.client.WazzupApiClient;
import ru.kirzhq.wazzup.dto.WazzupContact;
import ru.kirzhq.wazzup.dto.WazzupContactData;
import ru.kirzhq.wazzup.dto.WazzupContactsResponse;
import ru.kirzhq.wazzup.dto.UpdateContactRequest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactServiceTests {

    private final WazzupApiClient client = mock(WazzupApiClient.class);
    private final ContactService service = new ContactService(client);

    @Test
    void loadsEveryPageAndSearchesBeyondFirstHundredContacts() {
        List<WazzupContact> firstPage = IntStream.range(0, 100)
                .mapToObj(index -> contact(
                        "id-" + index,
                        "Контакт " + index,
                        "7999000%04d".formatted(index)
                ))
                .toList();
        List<WazzupContact> secondPage = List.of(
                contact("id-100", "Нужный клиент", "78881234567")
        );
        when(client.getContactsPage(0))
                .thenReturn(new WazzupContactsResponse(101L, firstPage));
        when(client.getContactsPage(100))
                .thenReturn(new WazzupContactsResponse(101L, secondPage));

        WazzupContactsResponse response = service.getContacts("нужный");

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.data()).extracting(WazzupContact::id)
                .containsExactly("id-100");
        verify(client).getContactsPage(0);
        verify(client).getContactsPage(100);
    }

    @Test
    void searchesPhoneIgnoringFormatting() {
        WazzupContact contact =
                contact("id-1", "Клиент", "79991234567");
        when(client.getContactsPage(0))
                .thenReturn(new WazzupContactsResponse(1L, List.of(contact)));

        WazzupContactsResponse response =
                service.getContacts("+7 (999) 123-45-67");

        assertThat(response.data()).containsExactly(contact);
    }

    @Test
    void deletesContactById() {
        service.deleteContact(" contact-1 ");

        verify(client).deleteContact("contact-1");
    }

    @Test
    void updatesAllEditableContactFields() {
        WazzupContact existing =
                contact("contact-1", "Старое имя", "79991234567");
        when(client.getContactById("contact-1")).thenReturn(existing);

        WazzupContact result = service.updateContact(
                "contact-1",
                new UpdateContactRequest(
                        "Новое имя",
                        "+7 (999) 000-11-22",
                        "telegram"
                )
        );

        assertThat(result.name()).isEqualTo("Новое имя");
        assertThat(result.contactData().getFirst().chatType())
                .isEqualTo("telegram");
        assertThat(result.contactData().getFirst().phone())
                .isEqualTo("79990001122");
        verify(client).saveContacts(argThat(contacts ->
                contacts.size() == 1
                        && contacts.getFirst().id().equals("contact-1")
        ));
    }

    private WazzupContact contact(String id, String name, String phone) {
        return new WazzupContact(
                id,
                "user-1",
                name,
                List.of(new WazzupContactData(
                        "whatsapp",
                        phone,
                        null,
                        null
                )),
                null
        );
    }
}
