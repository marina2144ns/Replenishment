package ru.stockmann.replenishment.services.dwhexcelload.core;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Common validation and normalization for the optional session initiator. */
public final class DWHRequestedBy {

    public static final int MAX_LENGTH = 100;

    private DWHRequestedBy() {
    }

    public static String normalizeAndValidate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("requestedBy must not be longer than 100 characters");
        }
        return value;
    }

    public static String fromCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return normalizeAndValidate(attributes.getRequest().getParameter("requestedBy"));
        }
        return null;
    }
}
