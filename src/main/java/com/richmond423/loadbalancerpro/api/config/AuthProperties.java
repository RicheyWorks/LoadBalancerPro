package com.richmond423.loadbalancerpro.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "loadbalancerpro.auth")
public class AuthProperties {
    private Mode mode = Mode.API_KEY;
    private boolean docsPublic = false;
    private OAuth2 oauth2 = new OAuth2();
    private RequiredRole requiredRole = new RequiredRole();

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.API_KEY : mode;
    }

    public boolean isDocsPublic() {
        return docsPublic;
    }

    public void setDocsPublic(boolean docsPublic) {
        this.docsPublic = docsPublic;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2 oauth2) {
        this.oauth2 = oauth2 == null ? new OAuth2() : oauth2;
    }

    public RequiredRole getRequiredRole() {
        return requiredRole;
    }

    public void setRequiredRole(RequiredRole requiredRole) {
        this.requiredRole = requiredRole == null ? new RequiredRole() : requiredRole;
    }

    public boolean isOAuth2Mode() {
        return mode == Mode.OAUTH2;
    }

    public boolean isApiKeyMode() {
        return mode == Mode.API_KEY;
    }

    public boolean isNoneMode() {
        return mode == Mode.NONE;
    }

    public void validateApiKeyMode(String apiKey) {
        if (isApiKeyMode() && !StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Application refuses to start with loadbalancerpro.auth.mode=api-key "
                    + "because loadbalancerpro.api.key is missing or blank; configure a key, select OAuth2, "
                    + "or explicitly set loadbalancerpro.auth.mode=none for bounded local development");
        }
    }

    public void validateOAuth2Mode() {
        if (!isOAuth2Mode()) {
            return;
        }
        if (!StringUtils.hasText(oauth2.issuerUri) && !StringUtils.hasText(oauth2.jwkSetUri)) {
            throw new IllegalStateException("OAuth2 auth mode requires loadbalancerpro.auth.oauth2.issuer-uri "
                    + "or loadbalancerpro.auth.oauth2.jwk-set-uri");
        }
        normalizedLaseShadowRole();
        normalizedAllocationRole();
        normalizedAdminRole();
    }

    public String normalizedLaseShadowRole() {
        return normalizeRequiredRole(requiredRole.laseShadow, "loadbalancerpro.auth.required-role.lase-shadow");
    }

    public String normalizedAllocationRole() {
        return normalizeRequiredRole(requiredRole.allocation, "loadbalancerpro.auth.required-role.allocation");
    }

    public String normalizedAdminRole() {
        return normalizeRequiredRole(requiredRole.admin, "loadbalancerpro.auth.required-role.admin");
    }

    private static String normalizeRequiredRole(String role, String propertyName) {
        if (!StringUtils.hasText(role)) {
            throw new IllegalStateException(propertyName + " must not be blank when OAuth2 auth mode is active");
        }
        String trimmed = role.trim();
        return trimmed.startsWith("ROLE_") ? trimmed.substring("ROLE_".length()) : trimmed;
    }

    public enum Mode {
        API_KEY,
        OAUTH2,
        NONE
    }

    public static final class OAuth2 {
        private String issuerUri = "";
        private String jwkSetUri = "";

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }
    }

    public static final class RequiredRole {
        private String laseShadow = "observer";
        private String allocation = "operator";
        private String admin = "admin";

        public String getLaseShadow() {
            return laseShadow;
        }

        public void setLaseShadow(String laseShadow) {
            this.laseShadow = laseShadow;
        }

        public String getAllocation() {
            return allocation;
        }

        public void setAllocation(String allocation) {
            this.allocation = allocation;
        }

        public String getAdmin() {
            return admin;
        }

        public void setAdmin(String admin) {
            this.admin = admin;
        }
    }
}
