package ru.kirzhq.wazzup.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.kirzhq.wazzup.dto.LoginRequest;
import ru.kirzhq.wazzup.dto.LoginResponse;
import ru.kirzhq.wazzup.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request.phone());
    }
}