package br.com.centavo.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    // Corpo de request que o Jackson nao consegue desserializar (enum fora do dominio,
    // JSON malformado, tipo incompativel). Isso estoura ANTES da bean validation, entao nao
    // vira MethodArgumentNotValidException — sem este handler cairia no fallback e o cliente
    // receberia 500, tratando erro DELE como erro do servidor.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setProperty("code", ErrorCode.VALIDATION_ERROR.name());

        // Valor fora de um enum: devolve o campo e os valores aceitos como DADO, nunca como
        // frase pronta — quem monta o texto localizado e o frontend, a partir do code (§1.2).
        if (ex.getCause() instanceof InvalidFormatException invalidFormat
                && invalidFormat.getTargetType() != null
                && invalidFormat.getTargetType().isEnum()) {

            String field = invalidFormat.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));

            List<String> accepted = Arrays.stream(invalidFormat.getTargetType().getEnumConstants())
                    .map(String::valueOf)
                    .toList();

            problem.setProperty("errors", List.of(Map.of(
                    "field", field,
                    "accepted", accepted
            )));
        }
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
