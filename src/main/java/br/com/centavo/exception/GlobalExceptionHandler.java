package br.com.centavo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

// Handler global de erros: converte excecoes em respostas RFC 7807 (application/problem+json)
// com um "code" estavel. As mensagens localizadas (pt-BR/en) sao resolvidas no frontend a partir do code.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ProblemDetail problem = ProblemDetail.forStatus(errorCode.getStatus());
        problem.setProperty("code", errorCode.name());
        // Detalhe opcional para debug; so quando difere do code padrao.
        if (ex.getMessage() != null && !ex.getMessage().equals(errorCode.name())) {
            problem.setDetail(ex.getMessage());
        }
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setProperty("code", ErrorCode.VALIDATION_ERROR.name());
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Map.of(
                        "field", fieldError.getField(),
                        "message", fieldError.getDefaultMessage() == null ? "" : fieldError.getDefaultMessage()
                ))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        // Fallback: nao vaza detalhes internos para o cliente.
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setProperty("code", ErrorCode.INTERNAL_ERROR.name());
        return problem;
    }
}
