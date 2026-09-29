package com.vdt.finq.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class InvalidRequestException extends AppException {
    
    public InvalidRequestException(String message) {
        super(400, message);
    }
}
