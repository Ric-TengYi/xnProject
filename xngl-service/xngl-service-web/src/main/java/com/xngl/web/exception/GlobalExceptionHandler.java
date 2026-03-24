package com.xngl.web.exception;

import com.xngl.infrastructure.persistence.entity.system.ErrorLog;
import com.xngl.infrastructure.persistence.entity.organization.User;
import com.xngl.manager.contract.ContractServiceException;
import com.xngl.manager.log.ErrorLogService;
import com.xngl.manager.project.ProjectPaymentException;
import com.xngl.manager.user.UserService;
import com.xngl.web.dto.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private final ErrorLogService errorLogService;
  private final UserService userService;

  public GlobalExceptionHandler(ErrorLogService errorLogService, UserService userService) {
    this.errorLogService = errorLogService;
    this.userService = userService;
  }

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

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
    return ApiResult.fail(400, "请求体格式错误");
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResult<?> handleMissingParam(MissingServletRequestParameterException e) {
    return ApiResult.fail(400, "缺少请求参数: " + e.getParameterName());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResult<?> handleOther(Exception e, HttpServletRequest request) {
    log.error("Unhandled exception", e);
    saveErrorLog(e, request);
    return ApiResult.fail(500, "系统异常，请稍后重试");
  }

  private void saveErrorLog(Exception e, HttpServletRequest request) {
    try {
      ErrorLog errorLog = new ErrorLog();
      String userId = request != null ? (String) request.getAttribute("userId") : null;
      if (userId != null && !userId.isBlank()) {
        try {
          Long numericUserId = Long.parseLong(userId);
          errorLog.setUserId(numericUserId);
          User currentUser = userService.getById(numericUserId);
          if (currentUser != null) {
            errorLog.setTenantId(currentUser.getTenantId());
            errorLog.setUsername(currentUser.getUsername());
          }
        } catch (NumberFormatException ignored) {
          // ignore invalid user id from token
        }
      }
      errorLog.setLevel("ERROR");
      errorLog.setExceptionType(e.getClass().getName());
      errorLog.setErrorMessage(truncate(e.getMessage(), 500));
      errorLog.setRequestUri(request != null ? request.getRequestURI() : null);
      errorLog.setHttpMethod(request != null ? request.getMethod() : null);
      errorLog.setIp(request != null ? request.getRemoteAddr() : null);
      errorLog.setStackTrace(truncate(stackTraceOf(e), 10000));
      errorLogService.save(errorLog);
    } catch (Exception logEx) {
      log.warn("Failed to persist error log: {}", logEx.getMessage());
    }
  }

  private String stackTraceOf(Exception e) {
    StringWriter writer = new StringWriter();
    PrintWriter printWriter = new PrintWriter(writer);
    e.printStackTrace(printWriter);
    printWriter.flush();
    return writer.toString();
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
