package com.contenthub.dto;

import java.time.LocalDateTime;

public class CommonDtos {

    public record MessageResponse(String message) {}

    public record ApiErrorResponse(
            int status,
            String error,
            String message,
            LocalDateTime timestamp
    ) {
        public static ApiErrorResponse of(int status, String error, String message) {
            return new ApiErrorResponse(status, error, message, LocalDateTime.now());
        }
    }
}
