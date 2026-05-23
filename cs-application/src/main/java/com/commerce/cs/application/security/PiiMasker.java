package com.commerce.cs.application.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PiiMasker {

    private static final Pattern PHONE = Pattern.compile("(\\d{3})-?(\\d{3,4})-?(\\d{4})");
    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern RESIDENT_REGISTRATION_NUMBER = Pattern.compile("\\d{6}-[1-4]\\d{6}");

    private PiiMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = maskResidentRegistrationNumber(text);
        masked = maskPhone(masked);
        masked = maskEmail(masked);
        return masked;
    }

    private static String maskResidentRegistrationNumber(String text) {
        return RESIDENT_REGISTRATION_NUMBER.matcher(text).replaceAll("******-*******");
    }

    private static String maskPhone(String text) {
        Matcher matcher = PHONE.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, matcher.group(1) + "-****-" + matcher.group(3));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String maskEmail(String text) {
        Matcher matcher = EMAIL.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, maskEmailLocalPart(matcher.group(1)) + "@" + matcher.group(2));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String maskEmailLocalPart(String localPart) {
        if (localPart.length() <= 2) {
            return "*".repeat(localPart.length());
        }
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + localPart.charAt(localPart.length() - 1);
    }
}
