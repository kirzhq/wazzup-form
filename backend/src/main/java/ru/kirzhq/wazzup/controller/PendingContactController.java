package ru.kirzhq.wazzup.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.kirzhq.wazzup.dto.ApprovePendingContactRequest;
import ru.kirzhq.wazzup.dto.PendingContactResponse;
import ru.kirzhq.wazzup.service.PendingContactService;

import java.util.List;

@RestController
@RequestMapping("/api/partner/pending-contacts")
public class PendingContactController {
    private final PendingContactService service;

    public PendingContactController(PendingContactService service) {
        this.service = service;
    }

    @GetMapping
    public List<PendingContactResponse> getPending() {
        return service.getPending();
    }

    @PostMapping("/{id}/approve")
    public void approve(
            @PathVariable String id,
            @Valid @RequestBody ApprovePendingContactRequest request
    ) {
        service.approve(id, request.name());
    }

    @PostMapping("/{id}/dismiss")
    public void dismiss(@PathVariable String id) {
        service.dismiss(id);
    }
}
