package com.app.preorder.common.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response {

    private String status;
    private String message;
    private Object data;

}

