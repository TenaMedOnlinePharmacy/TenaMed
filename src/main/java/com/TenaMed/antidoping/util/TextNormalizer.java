package com.TenaMed.antidoping.util;

import java.util.Locale;

public class TextNormalizer {

    private TextNormalizer() {
        // Utility class
    }

    public static String normalize(String input) {
        if (input == null) return null;
        return input.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9 ]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
