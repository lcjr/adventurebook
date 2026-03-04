package com.test.adventurebook.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SectionType {
    BEGIN,
    NODE,
    END;

    @JsonCreator
    public static SectionType fromString(String value) {
        if (value == null) return null;
        return SectionType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}