package com.app.preorder.common.type;

public enum DuplicateCheckType {
    LOGIN_ID, EMAIL, PHONE;

    public static DuplicateCheckType from(String value) {
        return DuplicateCheckType.valueOf(value.toUpperCase());
    }
}