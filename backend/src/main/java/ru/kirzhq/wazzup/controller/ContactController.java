package ru.kirzhq.wazzup.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.kirzhq.wazzup.dto.CreateContactRequest;
import ru.kirzhq.wazzup.dto.WazzupContact;
import ru.kirzhq.wazzup.dto.WazzupContactsResponse;
import ru.kirzhq.wazzup.service.ContactService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.kirzhq.wazzup.dto.RenameContactRequest;
import ru.kirzhq.wazzup.dto.UpdateContactRequest;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public WazzupContactsResponse getContacts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone
    ) {
        return contactService.getContacts(name, phone);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WazzupContact createContact(
            @Valid @RequestBody CreateContactRequest request
    ) {
        return contactService.createContact(request);
    }

    @PatchMapping("/{contactId}/name")
    public WazzupContact renameContact(
            @PathVariable String contactId,
            @Valid @RequestBody RenameContactRequest request
    ) {
        return contactService.renameContact(
                contactId,
                request
        );
    }

    @PatchMapping("/{contactId}")
    public WazzupContact updateContact(
            @PathVariable String contactId,
            @Valid @RequestBody UpdateContactRequest request
    ) {
        return contactService.updateContact(contactId, request);
    }

    @DeleteMapping("/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContact(@PathVariable String contactId) {
        contactService.deleteContact(contactId);
    }
}
