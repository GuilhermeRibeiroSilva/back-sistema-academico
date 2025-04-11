package com.academico.espacos.dto;

/**
 * DTO para representar uma resposta de sucesso de uma operação.
 * Utilizado para padronizar as mensagens de retorno da API.
 */
public class SuccessResponse {
    private String message;
    
    /**
     * Construtor padrão.
     */
    public SuccessResponse() {}
    
    /**
     * Construtor com mensagem de sucesso.
     * 
     * @param message A mensagem de sucesso
     */
    public SuccessResponse(String message) {
        this.message = message;
    }
    
    /**
     * Obtém a mensagem de sucesso.
     * 
     * @return A mensagem de sucesso
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Define a mensagem de sucesso.
     * 
     * @param message A mensagem de sucesso
     */
    public void setMessage(String message) {
        this.message = message;
    }
}