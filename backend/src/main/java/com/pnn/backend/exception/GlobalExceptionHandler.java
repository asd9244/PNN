package com.pnn.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API 전역 예외 처리. 유효하지 않은 요청·비즈니스 예외 시 400 Bad Request를 ProblemDetail 형식으로
 * 반환.
 * 
 * @RestControllerAdvice == 모든 Controller에서 발생한 예외를 여기서 하고, Controller마다
 *                       try-catch를 넣지 않아도, 이 클래스가 예외를 받아서 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation 실패 시 (MethodArgumentNotValidException)
     * 필드별 오류 메시지를 errors 맵에 담아 400 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException validationException) {
        Map<String, String> errors = validationException.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "유효하지 않은 값",
                        (first, duplicate) -> first, // 중복 키 시 첫 번째 메시지 유지
                        LinkedHashMap::new));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 데이터가 유효하지 않습니다.");
        problem.setTitle("Bad Request");
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * drugId 미존재 등 비즈니스 예외 (IllegalArgumentException)
     * detail에 예외 메시지를 담아 400 반환
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException businessException) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, businessException.getMessage());
        problem.setTitle("Bad Request");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
}
