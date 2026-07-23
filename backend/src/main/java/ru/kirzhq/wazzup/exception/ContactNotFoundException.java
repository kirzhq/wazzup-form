package ru.kirzhq.wazzup.exception;

public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException(String contactId) {
        super("Контакт с ID " + contactId + " не найден");
    }
}