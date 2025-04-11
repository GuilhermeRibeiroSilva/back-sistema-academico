package com.academico.espacos.dto;

/**
 * Classe DTO para representar respostas de erro.
 * Utilizada para enviar mensagens de erro padronizadas para o cliente.
 */
public class ErrorResponse {
    private String message;
    
    /**
     * Construtor padrão.
     */
    public ErrorResponse() {}
    
    /**
     * Construtor com mensagem de erro.
     * 
     * @param message a mensagem de erro
     */
    public ErrorResponse(String message) {
        this.message = message;
    }
    
    /**
     * Retorna a mensagem de erro.
     * 
     * @return a mensagem de erro
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Define a mensagem de erro.
     * 
     * @param message a nova mensagem de erro
     */
    public void setMessage(String message) {
        this.message = message;
    }
}