package com.atlas.explorer.features.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("auth")
public record AuthProperties(String username,String password) {
    public AuthProperties {
        if(username==null||username.isBlank()||username.length()>100)throw new IllegalArgumentException("APP_LOGIN_USERNAME is required (max 100 characters).");
        if(password==null||password.length()<16||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("APP_LOGIN_PASSWORD must contain at least 16 characters and at most 72 UTF-8 bytes.");
    }
    @Override public String toString(){return "AuthProperties[credentials=REDACTED]";}
}
