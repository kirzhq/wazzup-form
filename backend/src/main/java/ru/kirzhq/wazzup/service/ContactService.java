package ru.kirzhq.wazzup.service;


import ru.kirzhq.wazzup.dto.RenameContactRequest;
import ru.kirzhq.wazzup.exception.ContactNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.kirzhq.wazzup.client.WazzupApiClient;
import ru.kirzhq.wazzup.dto.CreateContactRequest;
import ru.kirzhq.wazzup.dto.WazzupContact;
import ru.kirzhq.wazzup.dto.WazzupContactData;
import ru.kirzhq.wazzup.dto.WazzupContactsResponse;
import ru.kirzhq.wazzup.dto.UpdateContactRequest;
import ru.kirzhq.wazzup.exception.WazzupApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ContactService {
    private static final int CONTACT_READ_ATTEMPTS = 3;
    private static final long CONTACT_READ_DELAY_MS = 200;
    private static final int PAGE_SIZE = 100;
    private static final Set<String> PHONE_CHAT_TYPES = Set.of(
            "whatsapp",
            "telegram",
            "viber",
            "max"
    );

    /**
     * Защита от бесконечного цикла, если Wazzup начнёт
     * возвращать некорректные данные пагинации.
     */
    private static final int MAX_PAGES = 10_000;

    private final WazzupApiClient wazzupApiClient;

    public ContactService(WazzupApiClient wazzupApiClient) {
        this.wazzupApiClient = wazzupApiClient;
    }

    /**
     * Получает полный список контактов.
     *
     * Фильтры имени и телефона применяются независимо.
     * Если заполнены оба, контакт должен соответствовать обоим.
     */
    public WazzupContactsResponse getContacts(
            String name,
            String phone
    ) {
        List<WazzupContact> allContacts = getAllContacts();

        if (!StringUtils.hasText(name) && !StringUtils.hasText(phone)) {
            return new WazzupContactsResponse(
                    (long) allContacts.size(),
                    allContacts
            );
        }

        List<WazzupContact> filteredContacts = searchContacts(
                allContacts,
                name,
                phone
        );

        return new WazzupContactsResponse(
                (long) filteredContacts.size(),
                filteredContacts
        );
    }

    /**
     * Создаёт новый контакт для поддерживаемой телефонной сети в Wazzup.
     */
    public WazzupContact createContact(CreateContactRequest request) {
        String name = request.name().trim();
        String phone = normalizePhone(request.phone());
        String responsibleUserId = request.responsibleUserId().trim();
        String chatType = normalizeChatType(request.chatType());

        validatePhone(phone);

        String contactId = UUID.randomUUID().toString();

        WazzupContactData contactData = createContactData(
                chatType,
                phone
        );

        WazzupContact contact = new WazzupContact(
                contactId,
                responsibleUserId,
                name,
                List.of(contactData),
                null
        );

        /*
         * Wazzup принимает массив контактов,
         * поэтому даже один контакт отправляем списком.
         */
        wazzupApiClient.saveContacts(List.of(contact));

        return getCreatedContact(contactId);
    }

    /**
     * Загружает все страницы контактов из Wazzup.
     */
    private List<WazzupContact> getAllContacts() {
        List<WazzupContact> contacts = new ArrayList<>();

        int offset = 0;
        int pageNumber = 0;
        long totalCount = Long.MAX_VALUE;

        while (contacts.size() < totalCount) {
            if (pageNumber >= MAX_PAGES) {
                throw new WazzupApiException(
                        "Превышено допустимое количество страниц контактов"
                );
            }

            WazzupContactsResponse response =
                    wazzupApiClient.getContactsPage(offset);

            if (response == null) {
                throw new WazzupApiException(
                        "Wazzup вернул пустой ответ со списком контактов"
                );
            }

            boolean totalCountKnown = response.count() != null;
            if (totalCountKnown) {
                totalCount = response.count();
            }

            List<WazzupContact> page = response.data();

            if (page == null || page.isEmpty()) {
                break;
            }

            contacts.addAll(page);

            /*
             * Увеличиваем offset на реальное число записей,
             * полученное от Wazzup.
             */
            offset += page.size();
            pageNumber++;

            /*
             * Если Wazzup вернул меньше 100 записей,
             * значит это последняя страница.
             */
            if (!totalCountKnown && page.size() < PAGE_SIZE) {
                break;
            }
        }

        return contacts;
    }

    /**
     * Выполняет поиск по имени и телефону.
     */
    private List<WazzupContact> searchContacts(
            List<WazzupContact> contacts,
            String name,
            String phone
    ) {
        String normalizedName = StringUtils.hasText(name)
                ? name.trim().toLowerCase(Locale.ROOT)
                : "";
        String normalizedPhone = StringUtils.hasText(phone)
                ? normalizePhone(phone)
                : "";

        return contacts.stream()
                .filter(contact ->
                        (normalizedName.isBlank()
                                || matchesName(contact, normalizedName))
                                && (normalizedPhone.isBlank()
                                || matchesPhone(contact, normalizedPhone))
                )
                .toList();
    }

    /**
     * Проверяет совпадение по имени контакта.
     */
    private boolean matchesName(
            WazzupContact contact,
            String search
    ) {
        if (contact == null || contact.name() == null) {
            return false;
        }

        return contact.name()
                .toLowerCase(Locale.ROOT)
                .contains(search);
    }

    /**
     * Проверяет совпадение по номеру телефона.
     *
     * Номер может находиться как в phone,
     * так и в chatId.
     */
    private boolean matchesPhone(
            WazzupContact contact,
            String searchPhone
    ) {
        if (contact == null
                || searchPhone.isBlank()
                || contact.contactData() == null) {
            return false;
        }

        return contact.contactData()
                .stream()
                .anyMatch(data ->
                        data != null
                                && (
                                containsPhone(
                                        data.phone(),
                                        searchPhone
                                )
                                        || containsPhone(
                                        data.chatId(),
                                        searchPhone
                                )
                        )
                );
    }

    /**
     * Проверяет, содержит ли номер искомую последовательность цифр.
     */
    private boolean containsPhone(
            String value,
            String searchPhone
    ) {
        if (value == null) {
            return false;
        }

        return normalizePhone(value).contains(searchPhone);
    }

    /**
     * Нормализует номер телефона.
     *
     * Оставляет только цифры.
     * Российский номер, начинающийся с 8,
     * преобразует в формат с 7.
     */
    private String normalizePhone(String value) {
        if (value == null) {
            return "";
        }

        String digits = value.replaceAll("\\D", "");

        if (digits.length() == 11 && digits.startsWith("8")) {
            return "7" + digits.substring(1);
        }

        return digits;
    }

    /**
     * Проверяет длину номера телефона.
     */
    private void validatePhone(String phone) {
        if (phone.length() < 10 || phone.length() > 15) {
            throw new IllegalArgumentException(
                    "Номер телефона должен содержать от 10 до 15 цифр"
            );
        }
    }

    /**
     * Определяет тип мессенджера.
     *
     * Поддерживаются сети, в которых контакт можно идентифицировать
     * по номеру телефона.
     */
    private String normalizeChatType(String chatType) {
        if (!StringUtils.hasText(chatType)) {
            return "whatsapp";
        }

        String normalizedChatType = chatType
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!PHONE_CHAT_TYPES.contains(normalizedChatType)) {
            throw new IllegalArgumentException(
                    "Поддерживаются WhatsApp, Telegram, Viber и MAX"
            );
        }

        return normalizedChatType;
    }

    /**
     * Формирует контактные данные для отправки в Wazzup.
     */
    private WazzupContactData createContactData(
            String chatType,
            String phone
    ) {
        String phoneField = "telegram".equals(chatType)
                || "max".equals(chatType)
                ? phone
                : null;

        return new WazzupContactData(chatType, phone, null, phoneField);
    }


    private WazzupContact getCreatedContact(String contactId) {
        WazzupApiException lastException = null;

        for (int attempt = 1; attempt <= CONTACT_READ_ATTEMPTS; attempt++) {
            try {
                return wazzupApiClient.getContactById(contactId);
            } catch (WazzupApiException exception) {
                lastException = exception;

                if (attempt < CONTACT_READ_ATTEMPTS) {
                    sleepBeforeRetry();
                }
            }
        }

        throw new WazzupApiException(
                "Контакт был отправлен в Wazzup, но его не удалось прочитать после сохранения",
                lastException
        );
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(CONTACT_READ_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new WazzupApiException(
                    "Ожидание получения созданного контакта было прервано",
                    exception
            );
        }
    }

    public WazzupContact renameContact(
            String contactId,
            RenameContactRequest request
    ) {
        if (!StringUtils.hasText(contactId)) {
            throw new IllegalArgumentException(
                    "ID контакта не должен быть пустым"
            );
        }

        String normalizedContactId = contactId.trim();
        String newName = request.name().trim();

        WazzupContact existingContact = findContactById(
                normalizedContactId
        );

        WazzupContact updatedContact = new WazzupContact(
                existingContact.id(),
                existingContact.responsibleUserId(),
                newName,
                existingContact.contactData(),
                existingContact.uri()
        );

        wazzupApiClient.saveContacts(List.of(updatedContact));

        return updatedContact;
    }

    public void deleteContact(String contactId) {
        if (!StringUtils.hasText(contactId)) {
            throw new IllegalArgumentException(
                    "ID контакта не должен быть пустым"
            );
        }

        wazzupApiClient.deleteContact(contactId.trim());
    }

    public WazzupContact updateContact(
            String contactId,
            UpdateContactRequest request
    ) {
        if (!StringUtils.hasText(contactId)) {
            throw new IllegalArgumentException(
                    "ID контакта не должен быть пустым"
            );
        }

        String phone = normalizePhone(request.phone());
        String chatType = normalizeChatType(request.chatType());
        validatePhone(phone);

        WazzupContact existingContact = findContactById(contactId.trim());
        WazzupContact updatedContact = new WazzupContact(
                existingContact.id(),
                existingContact.responsibleUserId(),
                request.name().trim(),
                List.of(createContactData(chatType, phone)),
                existingContact.uri()
        );

        wazzupApiClient.saveContacts(List.of(updatedContact));
        return updatedContact;
    }

    private WazzupContact findContactById(String contactId) {
        try {
            return wazzupApiClient.getContactById(contactId);
        } catch (WazzupApiException exception) {
            throw new ContactNotFoundException(contactId);
        }
    }
}
