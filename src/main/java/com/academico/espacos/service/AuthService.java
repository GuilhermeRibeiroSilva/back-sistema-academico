package com.academico.espacos.service;

import com.academico.espacos.model.*;
import com.academico.espacos.repository.*;
import com.academico.espacos.dto.LoginRequest;
import com.academico.espacos.dto.LoginResponse;
import com.academico.espacos.exception.AuthenticationException;
import com.academico.espacos.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.time.ZoneId;

@Service
public class AuthService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private TokenInvalidadoRepository tokenInvalidadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Usuario autenticar(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new AuthenticationException("Usuário não encontrado"));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new AuthenticationException("Senha inválida");
        }

        return usuario;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = autenticar(request.getUsername(), request.getPassword());
        String token = jwtTokenProvider.gerarToken(usuario);
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setId(usuario.getId());
        response.setUsername(usuario.getUsername());
        response.setRole(usuario.getRole());
        
        // Adicionar informações do professor apenas se o usuário for professor
        if (usuario.isProfessor() && usuario.getProfessor() != null) {
            response.setProfessorId(usuario.getProfessor().getId());
            response.setProfessorNome(usuario.getProfessor().getNome());
        }
        
        return response;
    }

    @Transactional
    public Usuario criarUsuarioAdmin(String username, String password) {
        validarNovoUsername(username);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRole("ROLE_ADMIN");

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario criarUsuarioProfessor(String username, String password, Professor professor) {
        validarNovoUsername(username);

        professor = professorRepository.save(professor);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRole("ROLE_PROFESSOR");
        usuario.setProfessor(professor);

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new AuthenticationException("Usuário não encontrado"));

        if (!passwordEncoder.matches(senhaAtual, usuario.getPassword())) {
            throw new AuthenticationException("Senha atual inválida");
        }

        usuario.setPassword(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    private void validarNovoUsername(String username) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new AuthenticationException("Username já existe");
        }
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            try {
                // Verificar se o token já existe na base antes de tentar inserir
                boolean tokenJaExiste = tokenInvalidadoRepository.existsByToken(token);
                
                if (!tokenJaExiste) {
                    TokenInvalidado tokenInvalidado = new TokenInvalidado();
                    tokenInvalidado.setToken(token);
                    tokenInvalidado.setExpiracaoToken(jwtTokenProvider.getExpiracaoToken(token));
                    tokenInvalidadoRepository.save(tokenInvalidado);
                }
            } catch (Exception e) {
                // Log detalhado do erro, mas sem falhar o processo de logout
                System.err.println("Erro ao salvar token invalidado: " + e.getMessage());
                // Continuar com o processo de logout mesmo em caso de falha no salvamento do token
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Executa a cada hora
    public void limparTokensExpirados() {
        Date agora = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        tokenInvalidadoRepository.limparTokensExpirados(agora);
    }

    public boolean isAdmin(Usuario usuario) {
        return "ROLE_ADMIN".equals(usuario.getRole());
    }

    public boolean isProfessor(Usuario usuario) {
        return "ROLE_PROFESSOR".equals(usuario.getRole());
    }
}