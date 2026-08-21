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
import ru.kirzhq.wazzup.dto.WazzupUser;
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

    // Ограничение защищает от бесконечного цикла при некорректной пагинации.
    private static final int MAX_PAGES = 10_000;

    private final WazzupApiClient wazzupApiClient;

    public ContactService(WazzupApiClient wazzupApiClient) {
        this.wazzupApiClient = wazzupApiClient;
    }

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

        wazzupApiClient.saveContacts(List.of(contact));

        return getCreatedContact(contactId);
    }

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

            offset += page.size();
            pageNumber++;

            if (!totalCountKnown && page.size() < PAGE_SIZE) {
                break;
            }
        }

        return contacts;
    }

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

    private boolean matchesPhone(
            WazzupContact contact,
            String searchPhone
    ) {
        if (contact == null
                || searchPhone.isBlank()
                || contact.contactData() == null) {
            return false;
        }

        boolean directMatch = contact.contactData()
                .stream()
                .anyMatch(data ->
                        data != null
                                && (
                                containsPhone(
                                        data.phone(),
                                        searchPhone
                                )
                                        || (isPhoneChatId(data.chatType()) && containsPhone(
                                        data.chatId(),
                                        searchPhone
                                ))
                        )
                );
        if (directMatch) return true;

        for (String chatType : List.of("telegram", "max")) {
            List<String> chatIds = contact.contactData().stream()
                    .filter(java.util.Objects::nonNull)
                    .filter(data -> chatType.equalsIgnoreCase(data.chatType()))
                    .map(WazzupContactData::chatId)
                    .filter(StringUtils::hasText)
                    .toList();
            if (chatIds.size() >= 2 && chatIds.stream().anyMatch(value ->
                    isLikelyImportedPhone(value) && containsPhone(value, searchPhone))) {
                return true;
            }
        }
        return false;
    }

    private boolean isLikelyImportedPhone(String value) {
        String phone = normalizePhone(value);
        return phone.length() == 11 && phone.startsWith("7");
    }

    private boolean isPhoneChatId(String chatType) {
        return "whatsapp".equalsIgnoreCase(chatType)
                || "viber".equalsIgnoreCase(chatType);
    }

    private boolean containsPhone(
            String value,
            String searchPhone
    ) {
        if (value == null) {
            return false;
        }

        return normalizePhone(value).contains(searchPhone);
    }

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

    private void validatePhone(String phone) {
        if (phone.length() < 10 || phone.length() > 15) {
            throw new IllegalArgumentException(
                    "Номер телефона должен содержать от 10 до 15 цифр"
            );
        }
    }

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
        if (!phone.isBlank()) validatePhone(phone);
        if (("whatsapp".equals(chatType) || "viber".equals(chatType)) && phone.isBlank()) {
            throw new IllegalArgumentException("Для WhatsApp и Viber номер телефона обязателен");
        }

        WazzupContact existingContact = findContactById(contactId.trim());
        WazzupContactData existingData = existingContact.contactData() == null
                ? null
                : existingContact.contactData().stream()
                .filter(data -> data != null && chatType.equalsIgnoreCase(data.chatType()))
                .findFirst().orElse(null);
        String preservedChatId = StringUtils.hasText(request.chatId())
                ? request.chatId().trim()
                : existingData == null ? phone : existingData.chatId();
        WazzupContactData updatedData = "telegram".equals(chatType) || "max".equals(chatType)
                ? new WazzupContactData(
                chatType,
                preservedChatId,
                existingData == null ? null : existingData.username(),
                phone.isBlank() ? null : phone
        )
                : createContactData(chatType, phone);
        WazzupContact updatedContact = new WazzupContact(
                existingContact.id(),
                existingContact.responsibleUserId(),
                request.name().trim(),
                List.of(updatedData),
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

    public synchronized boolean ensureChatContact(
            String chatType,
            String chatId,
            String username,
            String phone,
            String name
    ) {
        return ensureChatContacts(List.of(new ChatContactCandidate(
                chatType, chatId, username, phone, name
        ))) > 0;
    }

    public synchronized int ensureChatContacts(List<ChatContactCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return 0;

        Set<String> existingKeys = new java.util.HashSet<>();
        java.util.Map<String, WazzupContact> existingByChatKey = new java.util.HashMap<>();
        java.util.Map<String, WazzupContact> emptyContactsByName = new java.util.HashMap<>();
        for (WazzupContact contact : getAllContacts()) {
            if (contact == null || contact.contactData() == null) continue;
            if (contact.contactData().isEmpty() && StringUtils.hasText(contact.name())) {
                emptyContactsByName.putIfAbsent(
                        contact.name().trim().toLowerCase(Locale.ROOT), contact
                );
            }
            for (WazzupContactData data : contact.contactData()) {
                if (data == null || !StringUtils.hasText(data.chatType())) continue;
                if (StringUtils.hasText(data.chatId())) {
                    String key = data.chatType().toLowerCase(Locale.ROOT) + ":" + data.chatId();
                    existingKeys.add(key);
                    existingByChatKey.putIfAbsent(key, contact);
                }
                String dataPhone = normalizePhone(data.phone());
                if (!dataPhone.isBlank()) {
                    existingKeys.add(data.chatType().toLowerCase(Locale.ROOT) + ":phone:" + dataPhone);
                }
            }
        }

        List<WazzupContact> newContacts = new ArrayList<>();
        String responsibleUserId = null;
        for (ChatContactCandidate candidate : candidates) {
            String chatType = candidate.chatType();
            String chatId = candidate.chatId();
            if (!StringUtils.hasText(chatType) || !StringUtils.hasText(chatId)) continue;

            String normalizedType = normalizeImportedChatType(chatType);
            if (normalizedType.endsWith("group")) continue;
            String normalizedChatId = chatId.trim();
            String normalizedPhone = normalizePhone(candidate.phone());
            String chatKey = normalizedType + ":" + normalizedChatId;
            String phoneKey = normalizedType + ":phone:" + normalizedPhone;
            String contactName = StringUtils.hasText(candidate.name())
                    ? candidate.name().trim()
                    : (StringUtils.hasText(candidate.username())
                    ? candidate.username().trim() : null);
            if (!isUsableImportedName(contactName, normalizedChatId, normalizedPhone)) continue;
            WazzupContact existingContact = existingByChatKey.get(chatKey);
            if (existingContact != null) {
                continue;
            }
            if (!normalizedPhone.isBlank() && existingKeys.contains(phoneKey)) continue;

            WazzupContact emptyContact = emptyContactsByName.remove(
                    contactName.toLowerCase(Locale.ROOT)
            );
            if (emptyContact == null && responsibleUserId == null) {
                WazzupUser[] users = wazzupApiClient.getUsers();
                if (users == null || users.length == 0 || !StringUtils.hasText(users[0].id())) {
                    throw new WazzupApiException("В Wazzup нет сотрудника для новых контактов");
                }
                responsibleUserId = users[0].id();
            }
            newContacts.add(new WazzupContact(
                    emptyContact == null ? UUID.randomUUID().toString() : emptyContact.id(),
                    emptyContact == null ? responsibleUserId : emptyContact.responsibleUserId(),
                contactName,
                List.of(new WazzupContactData(
                        normalizedType,
                        normalizedChatId,
                            StringUtils.hasText(candidate.username()) ? candidate.username().trim() : null,
                        normalizedPhone.isBlank() ? null : normalizedPhone
                )),
                emptyContact == null ? null : emptyContact.uri()
            ));
            existingKeys.add(chatKey);
            if (!normalizedPhone.isBlank()) existingKeys.add(phoneKey);
        }

        for (int from = 0; from < newContacts.size(); from += 100) {
            wazzupApiClient.saveContacts(newContacts.subList(from, Math.min(from + 100, newContacts.size())));
        }
        return newContacts.size();
    }

    public Set<String> getExistingChatKeys() {
        Set<String> keys = new java.util.HashSet<>();
        for (WazzupContact contact : getAllContacts()) {
            if (contact == null || contact.contactData() == null) continue;
            for (WazzupContactData data : contact.contactData()) {
                if (data == null || !StringUtils.hasText(data.chatType())
                        || !StringUtils.hasText(data.chatId())) continue;
                keys.add(normalizeImportedChatType(data.chatType()) + ":" + data.chatId().trim());
            }
        }
        return keys;
    }

    private String normalizeImportedChatType(String chatType) {
        String normalized = chatType.trim().toLowerCase(Locale.ROOT);
        return "tgapi".equals(normalized) ? "telegram" : normalized;
    }

    private boolean isUsableImportedName(String name, String chatId, String phone) {
        if (!StringUtils.hasText(name)) return false;
        String normalizedName = name.trim();
        String nameDigits = normalizedName.replaceAll("\\D", "");
        return !normalizedName.equals(chatId)
                && !(nameDigits.length() >= 7
                && (nameDigits.equals(chatId) || nameDigits.equals(phone)))
                && !normalizedName.matches("[0-9_\\-]{5,}");
    }

    public record ChatContactCandidate(
            String chatType, String chatId, String username, String phone, String name
    ) {}
}
