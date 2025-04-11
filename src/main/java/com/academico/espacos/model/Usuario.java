package com.academico.espacos.model;

import com.academico.espacos.model.enums.Perfil;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

/**
 * Entidade que representa um usuário do sistema.
 * Cada usuário pode ter um perfil específico (ADMIN ou PROFESSOR).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_PROFESSOR = "ROLE_PROFESSOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @ManyToOne(fetch = FetchType.EAGER)
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

    /**
     * Obtém o perfil do usuário com base no role definido
     * @return Perfil do usuário (ADMIN ou PROFESSOR)
     */
    public Perfil getPerfil() {
        return ROLE_ADMIN.equals(role) ? Perfil.ADMIN : Perfil.PROFESSOR;
    }

    /**
     * Define o perfil do usuário, atualizando o role correspondente
     * @param perfil Perfil a ser atribuído ao usuário
     */
    public void setPerfil(Perfil perfil) {
        this.role = perfil == Perfil.ADMIN ? ROLE_ADMIN : ROLE_PROFESSOR;
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

    /**
     * Verifica se o usuário é um professor
     * @return true se o usuário tiver o perfil de PROFESSOR
     */
    public boolean isProfessor() {
        return getPerfil() == Perfil.PROFESSOR;
    }

    /**
     * Verifica se o usuário é um administrador
     * @return true se o usuário tiver o perfil de ADMIN
     */
    public boolean isAdmin() {
        return getPerfil() == Perfil.ADMIN;
    }
    
    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", perfil=" + getPerfil() +
                '}';
    }
}