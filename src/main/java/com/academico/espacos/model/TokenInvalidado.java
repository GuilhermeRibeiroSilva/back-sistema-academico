package com.academico.espacos.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "tokens_invalidados")
public class TokenInvalidado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Date expiracaoToken;

    // Getters e Setters
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

    public Date getExpiracaoToken() {
        return expiracaoToken;
    }

    public void setExpiracaoToken(Date expiracaoToken) {
        this.expiracaoToken = expiracaoToken;
    }
}