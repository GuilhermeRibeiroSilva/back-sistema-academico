package com.academico.espacos.exception;

/**
 * Exceção lançada quando regras de negócio são violadas no sistema acadêmico.
 * Estende RuntimeException para permitir propagação não-verificada.
 */
public class BusinessException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constrói uma nova exceção com a mensagem especificada.
     * 
     * @param message a mensagem de detalhe (que é salva para posterior recuperação
     *                pelo método {@link #getMessage()})
     */
    public BusinessException(String message) {
        super(message);
    }
    
    /**
     * Constrói uma nova exceção com a mensagem e causa especificadas.
     * 
     * @param message a mensagem de detalhe (que é salva para posterior recuperação
     *                pelo método {@link #getMessage()})
     * @param cause a causa (que é salva para posterior recuperação pelo método
     *              {@link #getCause()})
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}