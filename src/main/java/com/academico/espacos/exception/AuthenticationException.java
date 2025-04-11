package com.academico.espacos.exception;

/**
 * Exceção lançada para indicar falhas na autenticação do usuário.
 */
public class AuthenticationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constrói uma nova exceção de autenticação com a mensagem especificada.
     *
     * @param message A mensagem detalhando o erro de autenticação
     */
    public AuthenticationException(String message) {
        super(message);
    }
    
    /**
     * Constrói uma nova exceção de autenticação com a mensagem e causa especificadas.
     *
     * @param message A mensagem detalhando o erro de autenticação
     * @param cause A causa da exceção
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}