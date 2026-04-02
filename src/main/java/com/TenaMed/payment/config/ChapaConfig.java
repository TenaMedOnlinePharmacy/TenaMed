package com.TenaMed.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChapaConfig {

    private final String BASE_URL;
    private final String SECRET_KEY;

    public ChapaConfig(
            @Value("${chapa.base-url}") String baseUrl,
            @Value("${chapa.secret-key}") String secretKey
    ) {
        this.BASE_URL = baseUrl;
        this.SECRET_KEY = secretKey;
    }

    public String getBASE_URL() {
        return BASE_URL;
    }

    public String getSECRET_KEY() {
        return SECRET_KEY;
    }
}
