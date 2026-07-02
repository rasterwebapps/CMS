package com.cms.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.service.TokenRevocationService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final TokenRevocationService revocationService;

    public AuthController(TokenRevocationService revocationService) {
        this.revocationService = revocationService;
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeToken(JwtAuthenticationToken authentication) {
        String jti       = authentication.getToken().getId();
        Instant expiresAt = authentication.getToken().getExpiresAt();

        if (jti != null && expiresAt != null) {
            revocationService.revoke(jti, expiresAt);
        }
        return ResponseEntity.ok().build();
    }
}
