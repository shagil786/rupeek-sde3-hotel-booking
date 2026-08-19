package com.rupeek.hotelbooking.adapter.in.web;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status; private final String code;
    public ApiException(HttpStatus status, String code, String message) { super(message); this.status=status; this.code=code; }
    public HttpStatus getStatus(){return status;} public String getCode(){return code;}
    public static ApiException badRequest(String c,String m){return new ApiException(HttpStatus.BAD_REQUEST,c,m);}
    public static ApiException notFound(String c,String m){return new ApiException(HttpStatus.NOT_FOUND,c,m);}
    public static ApiException conflict(String c,String m){return new ApiException(HttpStatus.CONFLICT,c,m);}
    public static ApiException forbidden(String c,String m){return new ApiException(HttpStatus.FORBIDDEN,c,m);}
}
