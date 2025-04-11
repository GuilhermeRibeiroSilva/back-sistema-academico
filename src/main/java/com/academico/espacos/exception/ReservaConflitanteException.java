package com.academico.espacos.exception;

public class ReservaConflitanteException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ReservaConflitanteException(String message) {
        super(message);
    }
}