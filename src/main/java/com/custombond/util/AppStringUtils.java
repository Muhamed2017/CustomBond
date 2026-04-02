package com.custombond.util;

import org.apache.commons.lang3.StringUtils;

public final class AppStringUtils {

    public AppStringUtils() {
    }

    public static boolean isNullOrBlank(String value) {
        return StringUtils.isBlank(value);
    }

    public static String capitalize(String value) {
        return StringUtils.capitalize(value);
    }

    public static String sanitize(String value) {
        return value != null ? value.trim() : null;
    }

    public static String maskSensitive(String value) {
        if (StringUtils.isBlank(value) || value.length() <= 4)
            return "****";
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }

    public static String maskNameWithFirstLetter(String fullName) {

        if (fullName == null || fullName.trim().isEmpty()) {
            return fullName;
        }

        StringBuilder result = new StringBuilder();

        for (String name : fullName.trim().split("\\s+")) {

            if (name.length() == 1) {
                result.append(name);
            } else {
                result.append(name.charAt(0));
                result.append("*".repeat(name.length() - 1));
            }

            result.append(" ");
        }

        return result.toString().trim();
    }

}
