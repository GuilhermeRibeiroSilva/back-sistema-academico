package com.academico.espacos.dto;

import lombok.Data;

@Data
public class CriarUsuarioRequest {
    private String username;
    private String password;
}