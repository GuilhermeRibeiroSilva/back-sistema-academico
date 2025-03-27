package com.academico.espacos.dto;

import com.academico.espacos.model.Usuario;

public class LoginResponse {
    private String token;
    private Long id;
    private String username;
    private String role;
    private Long professorId;
    private String professorNome;

    public LoginResponse() {
        // Construtor vazio
    }

    public LoginResponse(String token, Usuario usuario) {
        this.token = token;
        this.id = usuario.getId();
        this.username = usuario.getUsername();
        this.role = usuario.getRole();
        
        if (usuario.getProfessor() != null) {
            this.professorId = usuario.getProfessor().getId();
            this.professorNome = usuario.getProfessor().getNome();
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
    }

    public String getProfessorNome() {
        return professorNome;
    }

    public void setProfessorNome(String professorNome) {
        this.professorNome = professorNome;
    }
}