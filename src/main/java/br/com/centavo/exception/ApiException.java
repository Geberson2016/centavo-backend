package br.com.centavo.exception;

// Excecao de dominio tipada. Carrega um ErrorCode estavel (que define o codigo e o
// status HTTP) em vez de uma mensagem pronta. Substitui o uso de RuntimeException cru.
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
