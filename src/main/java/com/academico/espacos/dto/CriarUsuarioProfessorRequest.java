package com.academico.espacos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criar um usuário associado a um professor no sistema.
 */
@Data
@NoArgsConstructor
public class CriarUsuarioProfessorRequest {
    
    @NotBlank(message = "O nome de usuário é obrigatório")
    private String username;
    
    @NotBlank(message = "A senha é obrigatória")
    private String password;
    
    @NotNull(message = "O ID do professor é obrigatório")
    private Long professorId;
}