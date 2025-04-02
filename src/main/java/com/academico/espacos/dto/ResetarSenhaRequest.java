package com.academico.espacos.dto;

public class ResetarSenhaRequest {
    private String novaSenha;
    
    public ResetarSenhaRequest() {}
    
    public String getNovaSenha() {
        return novaSenha;
    }
    
    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
}