package com.academico.espacos.model;

import com.academico.espacos.model.enums.Perfil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "usuarios")
public class Usuario {

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

    public Perfil getPerfil() {
        if (role != null && role.contains("ADMIN")) {
            return Perfil.ADMIN;
        } else {
            return Perfil.PROFESSOR;
        }
    }

    public void setPerfil(Perfil perfil) {
        if (perfil == Perfil.ADMIN) {
            this.role = "ROLE_ADMIN";
        } else {
            this.role = "ROLE_PROFESSOR";
        }
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
        return this.getPerfil() == Perfil.PROFESSOR;
    }

    /**
     * Verifica se o usuário é um administrador
     * @return true se o usuário tiver o perfil de ADMIN
     */
    public boolean isAdmin() {
        return this.getPerfil() == Perfil.ADMIN;
    }
}