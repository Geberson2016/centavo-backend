package br.com.centavo.exception;

import org.springframework.http.HttpStatus;

// Codigos de erro estaveis da API. O nome do enum e o "code" que vai na resposta
// RFC 7807 e que o frontend usa para resolver a mensagem localizada (i18n).
public enum ErrorCode {
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
    TRANSACTION_CATEGORY_TYPE_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
