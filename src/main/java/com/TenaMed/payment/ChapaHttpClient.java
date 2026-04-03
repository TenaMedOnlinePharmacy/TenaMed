package com.TenaMed.payment;

import com.TenaMed.payment.config.ChapaConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ChapaHttpClient {

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final MediaType TEXT_PLAIN = MediaType.parse("text/plain");

    private final OkHttpClient client;
    private final ChapaConfig chapaConfig;

    public ChapaHttpClient(ChapaConfig chapaConfig) {
        this.client = new OkHttpClient();
        this.chapaConfig = chapaConfig;
    }

    public String initializeTransaction(String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(buildUrl("/initialize"))
                .post(body)
                .addHeader("Authorization", "Bearer " + chapaConfig.getSECRET_KEY())
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body() == null ? "" : response.body().string();
        }
    }

    public String verifyTransaction(String txRef) throws IOException {

        Request request = new Request.Builder()
                .url(buildUrl("/verify/" + txRef))
                .get() // ✅ correct
                .addHeader("Authorization", "Bearer " + chapaConfig.getSECRET_KEY())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body() == null ? "" : response.body().string();
        }
    }

    public String cancelTransaction(String txRef) throws IOException {
        RequestBody body = RequestBody.create("", TEXT_PLAIN);
        String encodedTxRef = URLEncoder.encode(txRef, StandardCharsets.UTF_8);

        Request request = new Request.Builder()
                .url(buildUrl("/cancel/" + encodedTxRef))
                .put(body)
                .addHeader("Authorization", "Bearer " + chapaConfig.getSECRET_KEY())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body() == null ? "" : response.body().string();
        }
    }

    private String buildUrl(String suffix) {
        String base = chapaConfig.getBASE_URL();
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1) + suffix;
        }
        return base + suffix;
    }
}
