package com.academico.espacos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO para transferência de dados de usuário entre camadas da aplicação.
 * Contém informações básicas do usuário e do professor associado, quando aplicável.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {
    private Long id;
    private String username;
    private String role;
    private Long professorId;
    private String professorNome;
}