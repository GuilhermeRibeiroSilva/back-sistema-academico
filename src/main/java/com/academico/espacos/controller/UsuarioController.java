package com.academico.espacos.controller;

import com.academico.espacos.dto.UsuarioResponse;
import com.academico.espacos.dto.CriarUsuarioProfessorRequest;
import com.academico.espacos.dto.ResetarSenhaRequest;
import com.academico.espacos.dto.SuccessResponse;
// Corrigir importação de ErrorResponse
import com.academico.espacos.exception.ErrorResponse;
import com.academico.espacos.model.Usuario;
import com.academico.espacos.service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/usuarios")
@Tag(name = "Usuários", description = "API para gerenciamento de usuários do sistema")
@Validated
public class UsuarioController {
    
    private final UsuarioService usuarioService;
    
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }
    
    @GetMapping
    @Operation(summary = "Listar todos os usuários", description = "Retorna todos os usuários cadastrados no sistema")
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        List<Usuario> usuarios = usuarioService.listarTodos();
        List<UsuarioResponse> response = usuarios.stream()
            .map(this::converterParaResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/professor")
    @Operation(summary = "Criar usuário para professor", description = "Cria uma conta de usuário vinculada a um professor existente")
    public ResponseEntity<?> criarUsuarioProfessor(@Valid @RequestBody CriarUsuarioProfessorRequest request) {
        try {
            Usuario usuario = usuarioService.criarUsuarioProfessor(
                request.getUsername(), 
                request.getPassword(),
                request.getProfessorId()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(converterParaResponse(usuario));
        } catch (Exception e) {
            // Corrigir construtor para usar os parâmetros corretos
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro ao criar usuário: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PutMapping("/{id}/resetar-senha")
    @Operation(summary = "Resetar senha de usuário", description = "Altera a senha de um usuário existente")
    public ResponseEntity<?> resetarSenha(@PathVariable Long id, @Valid @RequestBody ResetarSenhaRequest request) {
        try {
            usuarioService.resetarSenha(id, request.getNovaSenha());
            return ResponseEntity.ok(new SuccessResponse("Senha resetada com sucesso"));
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário", description = "Remove um usuário do sistema")
    public ResponseEntity<?> excluirUsuario(@PathVariable Long id) {
        try {
            usuarioService.excluirUsuario(id);
            return ResponseEntity.ok(new SuccessResponse("Usuário excluído com sucesso"));
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Usuário não encontrado"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
    
    /**
     * Converte uma entidade Usuario para DTO UsuarioResponse
     */
    private UsuarioResponse converterParaResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(usuario.getId());
        response.setUsername(usuario.getUsername());
        response.setRole(usuario.getRole());
        
        if (usuario.getProfessor() != null) {
            response.setProfessorId(usuario.getProfessor().getId());
            response.setProfessorNome(usuario.getProfessor().getNome());
        }
        
        return response;
    }
}