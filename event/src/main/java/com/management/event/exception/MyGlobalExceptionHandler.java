package com.management.event.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class MyGlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> myMethodArgumentNotValidException(MethodArgumentNotValidException e){
        Map<String,String> response = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach(err -> {
            String fieldName = ((FieldError)err).getField();
            String massage = err.getDefaultMessage();
            response.put(fieldName,massage);
        });
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> myResourceNotFoundException(ResourceNotFoundException e){
        String message = e.getMessage();
        ApiResponse apiResponse = new ApiResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);

    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse> myApiException(ApiException e){
        String message = e.getMessage();
        ApiResponse apiResponse = new ApiResponse(message, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse> myBadRequestException(BadRequestException e){
        ApiResponse apiResponse = new ApiResponse(e.getMessage(), false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse> myUnauthorizedException(UnauthorizedException e){
        ApiResponse apiResponse = new ApiResponse(e.getMessage(), false);
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse> myForbiddenException(ForbiddenException e){
        ApiResponse apiResponse = new ApiResponse(e.getMessage(), false);
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse> myAccessDeniedException(org.springframework.security.access.AccessDeniedException e){
        ApiResponse apiResponse = new ApiResponse("Forbidden", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse> myAuthenticationException(org.springframework.security.core.AuthenticationException e){
        ApiResponse apiResponse = new ApiResponse("Unauthorized", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> myHttpMessageNotReadableException(HttpMessageNotReadableException e){
        ApiResponse apiResponse = new ApiResponse("Malformed JSON request", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiResponse> myMultipartException(Exception e){
        ApiResponse apiResponse = new ApiResponse("Invalid multipart request", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse> myMissingServletRequestPartException(MissingServletRequestPartException e){
        ApiResponse apiResponse = new ApiResponse("Missing multipart part: " + e.getRequestPartName(), false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse> myHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e){
        ApiResponse apiResponse = new ApiResponse("Unsupported Content-Type", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse> myHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e){
        ApiResponse apiResponse = new ApiResponse("Method not allowed", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> myMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e){
        String msg = "Invalid value for parameter: " + e.getName();
        ApiResponse apiResponse = new ApiResponse(msg, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> myMissingServletRequestParameterException(MissingServletRequestParameterException e){
        ApiResponse apiResponse = new ApiResponse("Missing request parameter: " + e.getParameterName(), false);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse> myNoResourceFoundException(NoResourceFoundException e){
        ApiResponse apiResponse = new ApiResponse("Not found", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CalendarConflictException.class)
    public ResponseEntity<Map<String, Object>> myCalendarConflictException(CalendarConflictException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("conflict", true);
        response.put("message", e.getMessage());
        response.put("conflicts", e.getConflicts() == null ? List.of() : e.getConflicts());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> myDataIntegrityViolationException(DataIntegrityViolationException e){
        String raw = mostSpecificMessage(e);
        String friendly = mapIntegrityMessage(raw);
        ApiResponse apiResponse = new ApiResponse(friendly, false);
        return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> myUnhandledException(Exception e){
        // Stable shape for frontend; keep message generic to avoid leaking internals.
        ApiResponse apiResponse = new ApiResponse("Internal server error", false);
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static String mostSpecificMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        String m = cur.getMessage();
        return m == null ? "" : m;
    }

    private static String mapIntegrityMessage(String raw) {
        String m = raw == null ? "" : raw.toLowerCase();

        // Clubs
        if (m.contains("club_name") || m.contains("club name")) return "Club name already exists";

        // Users
        if (m.contains("reg_number") || m.contains("reg number")) return "Registration number already exists";
        if (m.contains("email")) return "Email is already taken";

        // Club executive assignment (unique constraints)
        // DB messages vary by provider; keep this mapping conservative.
        if (m.contains("club_executive")) {
            if (m.contains("user_reg_number") || m.contains("user reg number")) return "This user is already assigned to a club executive role";
            if (m.contains("club_id") || m.contains("club id")) return "This club already has an executive assigned for that role";
        }

        // Generic duplicates / FK constraints
        if (m.contains("duplicate") || m.contains("unique")) return "Duplicate value violates a unique constraint";
        if (m.contains("foreign key")) return "Operation violates a foreign key constraint";

        return "Request violates a data integrity constraint";
    }
}
