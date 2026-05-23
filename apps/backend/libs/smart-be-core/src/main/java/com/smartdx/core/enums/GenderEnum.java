package com.smartdx.core.enums;

import lombok.Getter;

/**
 * Gender Enum
 */
@Getter
public enum GenderEnum {

    UNKNOWN(0, "Unknown"),
    MALE(1, "Male"),
    FEMALE(2, "Female");

    private final int code;
    private final String label;

    GenderEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static GenderEnum fromCode(int code) {
        for (GenderEnum gender : values()) {
            if (gender.code == code) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}
