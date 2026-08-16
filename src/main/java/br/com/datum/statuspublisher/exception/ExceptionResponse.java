package br.com.datum.statuspublisher.exception;

import java.time.Instant;

public record ExceptionResponse(Instant timestamp, String message, String path) {
}
