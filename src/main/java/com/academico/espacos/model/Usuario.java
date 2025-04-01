package com.academico.espacos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN ou ROLE_PROFESSOR

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    // Getters e Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

    // Métodos de verificação de autorização
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(this.role);
    }

    public boolean isProfessor() {
        return "ROLE_PROFESSOR".equals(this.role);
    }

    // Verificação de propriedade de dados
    public boolean isOwner(Professor professor) {
        return this.professor != null && this.professor.getId().equals(professor.getId());
    }

    // Verifica se tem acesso a uma reserva específica
    public boolean canAccess(Reserva reserva) {
        // Admins podem acessar qualquer reserva
        // Professores só acessam suas próprias reservas
        return isAdmin() || 
               (isProfessor() && professor != null && 
                reserva.getProfessor() != null && 
                reserva.getProfessor().getId().equals(professor.getId()));
    }
}