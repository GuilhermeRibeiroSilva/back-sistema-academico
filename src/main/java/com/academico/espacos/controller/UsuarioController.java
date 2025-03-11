package com.academico.espacos.controller;

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
            .map(UsuarioResponse::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/professor")
    public ResponseEntity<?> criarUsuarioProfessor(@RequestBody CriarUsuarioProfessorRequest request) {
        try {
            Usuario usuario = usuarioService.criarUsuarioProfessor(
                request.getUsername(),
                request.getPassword(),
                request.getProfessorId()
            );
            return ResponseEntity.ok(new UsuarioResponse(usuario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/resetar-senha")
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
            usuarioService.desativarUsuario(id);
            return ResponseEntity.ok(new SuccessResponse("Usuário desativado com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // Classes DTO
    public static class UsuarioResponse {
        private Long id;
        private String username;
        private String role;
        private Long professorId;
        private String professorNome;
        
        public UsuarioResponse(Usuario usuario) {
            this.id = usuario.getId();
            this.username = usuario.getUsername();
            this.role = usuario.getRole();
            if (usuario.getProfessor() != null) {
                this.professorId = usuario.getProfessor().getId();
                this.professorNome = usuario.getProfessor().getNome();
            }
        }
        
        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public Long getProfessorId() { return professorId; }
        public String getProfessorNome() { return professorNome; }
    }
    
    public static class CriarUsuarioProfessorRequest {
        private String username;
        private String password;
        private Long professorId;
        
        // Getters e Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Long getProfessorId() { return professorId; }
        public void setProfessorId(Long professorId) { this.professorId = professorId; }
    }
    
    public static class ResetarSenhaRequest {
        private String novaSenha;
        
        // Getter e Setter
        public String getNovaSenha() { return novaSenha; }
        public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
    }
    
    public static class SuccessResponse {
        private String message;
        
        public SuccessResponse(String message) {
            this.message = message;
        }
        
        // Getter
        public String getMessage() { return message; }
    }
    
    public static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        // Getter
        public String getMessage() { return message; }
    }
}