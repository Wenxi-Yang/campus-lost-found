package cn.campus.lostfound;

import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiError {

  public static class Failure extends RuntimeException {

    final int status;

    Failure(int status, String message) {
      super(message);
      this.status = status;
    }
  }

  public static Failure bad(String s) {
    return new Failure(400, s);
  }

  public static Failure denied() {
    return new Failure(403, "没有操作权限");
  }

  @ExceptionHandler(Failure.class)
  public ResponseEntity<?> failure(Failure e) {
    return ResponseEntity
      .status(e.status)
      .body(Collections.singletonMap("message", e.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> conflict(Exception e) {
    return ResponseEntity
      .status(409)
      .body(
        Collections.singletonMap(
          "message",
          "数据重复或被其他记录引用，请检查学号、手机号、分类或认领记录"
        )
      );
  }

  @ExceptionHandler(
    {
      HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class,
      IllegalArgumentException.class,
    }
  )
  public ResponseEntity<?> invalid(Exception e) {
    return ResponseEntity
      .badRequest()
      .body(Collections.singletonMap("message", "请求格式不正确，请检查字段和时间"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(ApiError.class).error("Request failed", e);
    return ResponseEntity
      .status(500)
      .body(Collections.singletonMap("message", "服务器处理失败，请稍后重试"));
  }
}
