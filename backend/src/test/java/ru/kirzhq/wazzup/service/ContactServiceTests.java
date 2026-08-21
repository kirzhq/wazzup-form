package ru.kirzhq.wazzup.service;

import org.junit.jupiter.api.Test;
import ru.kirzhq.wazzup.client.WazzupApiClient;
import ru.kirzhq.wazzup.dto.WazzupContact;
import ru.kirzhq.wazzup.dto.WazzupContactData;
import ru.kirzhq.wazzup.dto.WazzupContactsResponse;
import ru.kirzhq.wazzup.dto.UpdateContactRequest;
import ru.kirzhq.wazzup.dto.CreateContactRequest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

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

        WazzupContactsResponse response =
                service.getContacts("нужный", null);

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
                service.getContacts(null, "+7 (999) 123-45-67");

        assertThat(response.data()).containsExactly(contact);
    }

    @Test
    void searchesByAnyPhoneFragment() {
        WazzupContact matching = new WazzupContact(
                "id-1",
                "user-1",
                "Клиент Telegram",
                List.of(new WazzupContactData(
                        "telegram", "494628845", "litovec", "79119289893"
                )),
                null
        );
        when(client.getContactsPage(0))
                .thenReturn(new WazzupContactsResponse(1L, List.of(matching)));

        WazzupContactsResponse response = service.getContacts(null, "92898");

        assertThat(response.data()).containsExactly(matching);
    }

    @Test
    void nameWithDigitDoesNotSearchThatDigitInPhone() {
        WazzupContact exactName =
                contact("id-1", "Тест2", "79000000000");
        WazzupContact digitOnlyInPhone =
                contact("id-2", "Другой клиент", "79991234567");
        when(client.getContactsPage(0)).thenReturn(
                new WazzupContactsResponse(
                        2L,
                        List.of(exactName, digitOnlyInPhone)
                )
        );

        WazzupContactsResponse response =
                service.getContacts("Тест2", null);

        assertThat(response.data()).containsExactly(exactName);
    }

    @Test
    void combinesNameAndPhoneFilters() {
        WazzupContact matching =
                contact("id-1", "Иван Петров", "79991234567");
        WazzupContact wrongPhone =
                contact("id-2", "Иван Сидоров", "78881234567");
        when(client.getContactsPage(0)).thenReturn(
                new WazzupContactsResponse(
                        2L,
                        List.of(matching, wrongPhone)
                )
        );

        WazzupContactsResponse response =
                service.getContacts("Иван", "999");

        assertThat(response.data()).containsExactly(matching);
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
                        "telegram",
                        null
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

    @Test
    void automaticImportNeverRenamesAnExistingContact() {
        WazzupContact existing = new WazzupContact(
                "contact-1",
                "user-1",
                "Проверенное имя",
                List.of(new WazzupContactData(
                        "telegram", "494628845", "litovec", "79119289893"
                )),
                null
        );
        when(client.getContactsPage(0))
                .thenReturn(new WazzupContactsResponse(1L, List.of(existing)));

        int changed = service.ensureChatContacts(List.of(
                new ContactService.ChatContactCandidate(
                        "telegram", "494628845", "wrong", null, "Елена"
                )
        ));

        assertThat(changed).isZero();
        verify(client, never()).saveContacts(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void createsWhatsappContactWithResponsibleUserAndPhoneChatId() {
        when(client.getContactById(anyString())).thenAnswer(invocation ->
                new WazzupContact(
                        invocation.getArgument(0), "user-1", "Новый клиент",
                        List.of(new WazzupContactData(
                                "whatsapp", "79991234567", null, null
                        )), null
                ));

        WazzupContact created = service.createContact(new CreateContactRequest(
                "Новый клиент", "+7 (999) 123-45-67", "user-1", "whatsapp"
        ));

        assertThat(created.name()).isEqualTo("Новый клиент");
        verify(client).saveContacts(argThat(contacts -> {
            WazzupContact contact = contacts.getFirst();
            return contact.responsibleUserId().equals("user-1")
                    && contact.contactData().getFirst().chatType().equals("whatsapp")
                    && contact.contactData().getFirst().chatId().equals("79991234567");
        }));
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
