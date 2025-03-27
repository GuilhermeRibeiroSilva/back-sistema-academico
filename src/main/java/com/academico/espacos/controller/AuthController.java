package com.academico.espacos.controller;

import com.academico.espacos.dto.LoginRequest;
import com.academico.espacos.dto.LoginResponse;
import com.academico.espacos.model.Usuario;
import com.academico.espacos.model.Professor;
import com.academico.espacos.service.AuthService;
import com.academico.espacos.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Erro ao autenticar: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/criar")
    public ResponseEntity<?> criarAdmin(@RequestBody CriarUsuarioRequest request) {
        try {
            Usuario usuario = authService.criarUsuarioAdmin(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(new UsuarioResponse(usuario));
        } catch (AuthenticationException e) {
            return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/professor/criar")
    public ResponseEntity<?> criarProfessor(@RequestBody CriarProfessorRequest request) {
        try {
            Professor professor = new Professor();
            professor.setNome(request.getNome());
            professor.setEscola(request.getEscola());
            
            Usuario usuario = authService.criarUsuarioProfessor(
                request.getUsername(), 
                request.getPassword(),
                professor
            );
            return ResponseEntity.ok(new UsuarioResponse(usuario));
        } catch (AuthenticationException e) {
            return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/alterar-senha")
    public ResponseEntity<?> alterarSenha(@RequestBody AlterarSenhaRequest request) {
        try {
            authService.alterarSenha(
                request.getUsuarioId(),
                request.getSenhaAtual(),
                request.getNovaSenha()
            );
            return ResponseEntity.ok(new SuccessResponse("Senha alterada com sucesso"));
        } catch (AuthenticationException e) {
            return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authService.logout(token);
                return ResponseEntity.ok(new SuccessResponse("Logout realizado com sucesso"));
            } else {
                return ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse("Token inválido ou ausente"));
            }
        } catch (Exception e) {
            e.printStackTrace();  // Para debug
            return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("Erro ao realizar logout: " + e.getMessage()));
        }
    }

    // Classes DTO
    public static class CriarUsuarioRequest {
        private String username;
        private String password;

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
    }

    public static class CriarProfessorRequest {
        private String username;
        private String password;
        private String nome;
        private String escola;

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

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getEscola() {
            return escola;
        }

        public void setEscola(String escola) {
            this.escola = escola;
        }
    }

    public static class AlterarSenhaRequest {
        private Long usuarioId;
        private String senhaAtual;
        private String novaSenha;

        public Long getUsuarioId() {
            return usuarioId;
        }

        public void setUsuarioId(Long usuarioId) {
            this.usuarioId = usuarioId;
        }

        public String getSenhaAtual() {
            return senhaAtual;
        }

        public void setSenhaAtual(String senhaAtual) {
            this.senhaAtual = senhaAtual;
        }

        public String getNovaSenha() {
            return novaSenha;
        }

        public void setNovaSenha(String novaSenha) {
            this.novaSenha = novaSenha;
        }
    }

    public static class RecuperacaoSenhaRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ConfirmarRecuperacaoRequest {
        private String token;
        private String novaSenha;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNovaSenha() {
            return novaSenha;
        }

        public void setNovaSenha(String novaSenha) {
            this.novaSenha = novaSenha;
        }
    }

    public static class UsuarioResponse {
        private Long id;
        private String username;
        private String role;

        public UsuarioResponse(Usuario usuario) {
            this.id = usuario.getId();
            this.username = usuario.getUsername();
            this.role = usuario.getRole();
        }

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

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class SuccessResponse {
        private String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}