package ru.kirzhq.wazzup.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.kirzhq.wazzup.dto.OauthStartResponse;
import ru.kirzhq.wazzup.dto.OauthCompleteRequest;
import ru.kirzhq.wazzup.dto.PartnerStatusResponse;
import ru.kirzhq.wazzup.service.PartnerOauthService;
import ru.kirzhq.wazzup.service.PartnerMessagesService;

@RestController
@RequestMapping("/api/partner")
public class PartnerController {
    private final PartnerOauthService partnerOauthService;
    private final PartnerMessagesService partnerMessagesService;

    public PartnerController(PartnerOauthService partnerOauthService,
                             PartnerMessagesService partnerMessagesService) {
        this.partnerOauthService = partnerOauthService;
        this.partnerMessagesService = partnerMessagesService;
    }

    @GetMapping("/status")
    public PartnerStatusResponse getStatus() {
        return partnerOauthService.getStatus();
    }

    @GetMapping("/oauth/start")
    public OauthStartResponse startOauth() {
        return partnerOauthService.startAuthorization();
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> completeOauth(
            @RequestParam String code,
            @RequestParam String state
    ) {
        partnerOauthService.completeAuthorization(code, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/contacts?partner=connected")
                .build();
    }

    @PostMapping("/oauth/complete")
    public void completeOauth(@Valid @RequestBody OauthCompleteRequest request) {
        partnerOauthService.completeAuthorization(request.code(), request.state());
    }

    @PostMapping("/sync")
    public void synchronizeNow() {
        partnerMessagesService.requestSynchronization();
    }

}
