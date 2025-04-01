package com.academico.espacos.controller;

import com.academico.espacos.dto.UsuarioResponse;
import com.academico.espacos.dto.CriarUsuarioProfessorRequest;
import com.academico.espacos.dto.ResetarSenhaRequest;
import com.academico.espacos.dto.SuccessResponse;
import com.academico.espacos.dto.ErrorResponse;
import com.academico.espacos.model.Usuario;
import com.academico.espacos.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioController {
    
    @Autowired
    private UsuarioService usuarioService;
    
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        List<Usuario> usuarios = usuarioService.listarTodos();
        List<UsuarioResponse> response = usuarios.stream()
            .map(u -> {
                UsuarioResponse dto = new UsuarioResponse();
                dto.setId(u.getId());
                dto.setUsername(u.getUsername());
                dto.setRole(u.getRole());
                if (u.getProfessor() != null) {
                    dto.setProfessorId(u.getProfessor().getId());
                    dto.setProfessorNome(u.getProfessor().getNome());
                }
                return dto;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<?> criarUsuarioProfessor(@RequestBody CriarUsuarioProfessorRequest request) {
        try {
            Usuario usuario = usuarioService.criarUsuarioProfessor(
                request.getUsername(), 
                request.getPassword(),
                request.getProfessorId()
            );
            
            UsuarioResponse response = new UsuarioResponse();
            response.setId(usuario.getId());
            response.setUsername(usuario.getUsername());
            response.setRole(usuario.getRole());
            if (usuario.getProfessor() != null) {
                response.setProfessorId(usuario.getProfessor().getId());
                response.setProfessorNome(usuario.getProfessor().getNome());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/resetar-senha")
    public ResponseEntity<?> resetarSenha(@PathVariable Long id, @RequestBody ResetarSenhaRequest request) {
        try {
            usuarioService.resetarSenha(id, request.getNovaSenha());
            return ResponseEntity.ok(new SuccessResponse("Senha resetada com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> desativarUsuario(@PathVariable Long id) {
        try {
            usuarioService.excluirUsuario(id);
            return ResponseEntity.ok(new SuccessResponse("Usuário excluído com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}