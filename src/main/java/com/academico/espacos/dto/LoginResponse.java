package com.academico.espacos.dto;

import com.academico.espacos.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de login contendo informações do usuário autenticado
 * e seu token de acesso.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long id;
    private String username;
    private String role;
    private Long professorId;
    private String professorNome;

    /**
     * Cria uma resposta de login com token e dados do usuário
     *
     * @param token Token de autenticação
     * @param usuario Usuário autenticado
     */
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
}