package com.app.preorder.common.util;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Response {

    private String status;
    private String message;
    private Object data;

}

