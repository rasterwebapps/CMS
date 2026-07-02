package com.cms.config;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.cms.service.TokenRevocationService;

@Component
public class RevocationJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final TokenRevocationService revocationService;

    public RevocationJwtValidator(TokenRevocationService revocationService) {
        this.revocationService = revocationService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(@NonNull Jwt token) {
        String jti = token.getId();
        if (jti != null && revocationService.isRevoked(jti)) {
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Token has been revoked", null)
            );
        }
        return OAuth2TokenValidatorResult.success();
    }
}
