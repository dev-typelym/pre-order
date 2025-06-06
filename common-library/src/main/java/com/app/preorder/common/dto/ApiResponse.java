package com.app.preorder.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ApiResponse<T> {

    private boolean success;   // 성공 여부
    private String message;    // 응답 메시지
    private String errorCode;  // 에러 코드
    private T data;            // 실제 데이터

    //  성공 응답 헬퍼
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, null, data);
    }

    //  실패 응답 헬퍼
    public static <T> ApiResponse<T> failure(String message, String errorCode) {
        return new ApiResponse<>(false, message, errorCode, null);
    }
}
