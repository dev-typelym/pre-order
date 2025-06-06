package com.app.preorder.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ApiResponse<T> {

    private boolean success;   // 성공 여부
    private String message;    // 응답 메시지
    private String errorCode;  // 에러 코드
    private T data;            // 실제 데이터

}
