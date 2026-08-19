package com.rupeek.hotelbooking.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ProblemDetail api(ApiException e) { ProblemDetail p=ProblemDetail.forStatusAndDetail(e.getStatus(),e.getMessage()); p.setTitle(e.getCode()); p.setProperty("code",e.getCode()); return p; }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) { ProblemDetail p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Request validation failed"); p.setTitle("VALIDATION_ERROR"); var details=new HashMap<String,String>(); e.getBindingResult().getFieldErrors().forEach(x->details.put(x.getField(),x.getDefaultMessage())); p.setProperty("errors",details); return p; }
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    ProblemDetail optimistic() { return api(ApiException.conflict("OPTIMISTIC_LOCK_CONFLICT","The resource changed; retry the request.")); }
}
