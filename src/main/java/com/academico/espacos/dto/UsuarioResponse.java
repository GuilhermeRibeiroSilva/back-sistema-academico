package com.academico.espacos.dto;

public class UsuarioResponse {
    private Long id;
    private String username;
    private String role;
    private Long professorId;
    private String professorNome;
    
    public UsuarioResponse() {}
    
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