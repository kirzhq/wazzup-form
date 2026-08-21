package ru.kirzhq.wazzup.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kirzhq.wazzup.service.PartnerWebhookService;

import java.util.Map;

@RestController
@RequestMapping("/api/partner/webhook")
public class PartnerWebhookController {
    private final PartnerWebhookService webhookService;

    public PartnerWebhookController(PartnerWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Void> accept(@RequestBody Map<String, Object> payload) {
        webhookService.accept(payload);
        return ResponseEntity.ok().build();
    }
}
