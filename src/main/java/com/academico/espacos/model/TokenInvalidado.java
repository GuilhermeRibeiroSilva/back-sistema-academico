package com.academico.espacos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens_invalidados", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_expiracao", columnList = "dataExpiracao")
})
public class TokenInvalidado {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 1000)
    private String token;
    
    private LocalDateTime dataInvalidacao;
    
    private LocalDateTime dataExpiracao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getDataInvalidacao() {
        return dataInvalidacao;
    }

    public void setDataInvalidacao(LocalDateTime dataInvalidacao) {
        this.dataInvalidacao = dataInvalidacao;
    }
    
    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }
    
    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }
}