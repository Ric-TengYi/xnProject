package com.xngl.web.exception;

import com.xngl.manager.contract.ContractServiceException;
import com.xngl.manager.project.ProjectPaymentException;
import com.xngl.web.dto.ApiResult;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BizException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResult<?> handleBiz(BizException e) {
    return ApiResult.fail(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(ContractServiceException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResult<?> handleContract(ContractServiceException e) {
    return ApiResult.fail(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(ProjectPaymentException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResult<?> handleProjectPayment(ProjectPaymentException e) {
    return ApiResult.fail(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<?> handleValidation(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return ApiResult.fail(400, message);
  }

  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<?> handleBind(BindException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return ApiResult.fail(400, message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<?> handleConstraint(ConstraintViolationException e) {
    String message = e.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
        .collect(Collectors.joining("; "));
    return ApiResult.fail(400, message);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResult<?> handleOther(Exception e) {
    return ApiResult.fail(500, e.getMessage() != null ? e.getMessage() : "internal error");
  }
}
