package com.academico.espacos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisições de redefinição de senha.
 */
public class ResetarSenhaRequest {
    
    @NotBlank(message = "A nova senha não pode estar em branco")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
    private String novaSenha;
    
    /**
     * Construtor padrão.
     */
    public ResetarSenhaRequest() {}
    
    /**
     * Construtor com parâmetros.
     * 
     * @param novaSenha a nova senha a ser definida
     */
    public ResetarSenhaRequest(String novaSenha) {
        this.novaSenha = novaSenha;
    }
    
    /**
     * Retorna a nova senha.
     * 
     * @return a nova senha
     */
    public String getNovaSenha() {
        return novaSenha;
    }
    
    /**
     * Define a nova senha.
     * 
     * @param novaSenha a nova senha a ser definida
     */
    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
}