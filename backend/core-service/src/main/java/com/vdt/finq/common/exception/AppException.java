package com.vdt.finq.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class AppException extends RuntimeException {

    private int errorCode; 

    public AppException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
