package com.app.preorder.common.type;

import java.util.Arrays;

public enum DuplicateCheckType {
    LOGIN_ID("아이디"),
    EMAIL("이메일"),
    PHONE("전화번호");

    private final String displayName;

    DuplicateCheckType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DuplicateCheckType from(String type) {
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 타입입니다: " + type));
    }
}