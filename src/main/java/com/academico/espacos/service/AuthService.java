package com.academico.espacos.service;

import com.academico.espacos.model.*;
import com.academico.espacos.repository.*;
import com.academico.espacos.dto.LoginRequest;
import com.academico.espacos.dto.LoginResponse;
import com.academico.espacos.exception.AuthenticationException;
import com.academico.espacos.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.Date;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_PROFESSOR = "ROLE_PROFESSOR";
    
    private final UsuarioRepository usuarioRepository;
    private final ProfessorRepository professorRepository;
    private final TokenInvalidadoRepository tokenInvalidadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UsuarioRepository usuarioRepository,
            ProfessorRepository professorRepository,
            TokenInvalidadoRepository tokenInvalidadoRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.professorRepository = professorRepository;
        this.tokenInvalidadoRepository = tokenInvalidadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public Usuario autenticar(String username, String password) {
        Objects.requireNonNull(username, "Username não pode ser nulo");
        Objects.requireNonNull(password, "Password não pode ser nulo");
        
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new AuthenticationException("Usuário não encontrado"));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new AuthenticationException("Senha inválida");
        }

        return usuario;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Objects.requireNonNull(request, "Login request não pode ser nulo");
        
        Usuario usuario = autenticar(request.getUsername(), request.getPassword());
        String token = jwtTokenProvider.gerarToken(usuario);
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setId(usuario.getId());
        response.setUsername(usuario.getUsername());
        response.setRole(usuario.getRole());
        
        adicionarInfoProfessorSeNecessario(usuario, response);
        
        return response;
    }
    
    private void adicionarInfoProfessorSeNecessario(Usuario usuario, LoginResponse response) {
        if (isProfessor(usuario) && usuario.getProfessor() != null) {
            response.setProfessorId(usuario.getProfessor().getId());
            response.setProfessorNome(usuario.getProfessor().getNome());
        }
    }

    @Transactional
    public Usuario criarUsuarioAdmin(String username, String password) {
        validarParametrosUsuario(username, password);
        validarNovoUsername(username);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRole(ROLE_ADMIN);

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario criarUsuarioProfessor(String username, String password, Professor professor) {
        validarParametrosUsuario(username, password);
        validarNovoUsername(username);
        Objects.requireNonNull(professor, "Professor não pode ser nulo");

        professor = professorRepository.save(professor);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRole(ROLE_PROFESSOR);
        usuario.setProfessor(professor);

        return usuarioRepository.save(usuario);
    }
    
    private void validarParametrosUsuario(String username, String password) {
        Objects.requireNonNull(username, "Username não pode ser nulo");
        Objects.requireNonNull(password, "Password não pode ser nulo");
        
        if (username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username não pode estar vazio");
        }
        
        if (password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password não pode estar vazio");
        }
    }

    @Transactional
    public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        Objects.requireNonNull(usuarioId, "ID do usuário não pode ser nulo");
        Objects.requireNonNull(senhaAtual, "Senha atual não pode ser nula");
        Objects.requireNonNull(novaSenha, "Nova senha não pode ser nula");
        
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
        if (token == null || token.isEmpty()) {
            logger.warn("Tentativa de logout com token nulo ou vazio");
            return;
        }

        try {
            TokenInvalidado tokenInvalidado = new TokenInvalidado();
            tokenInvalidado.setToken(token);
            
            Claims claims = jwtTokenProvider.extractClaims(token);
            // Conversão correta de Date para LocalDateTime
            LocalDateTime expiracao = claims.getExpiration().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            tokenInvalidado.setExpiracaoToken(expiracao);
            
            tokenInvalidadoRepository.save(tokenInvalidado);
            logger.info("Logout realizado. Token invalidado.");
        } catch (Exception e) {
            logger.error("Erro ao invalidar token: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Executa a cada hora
    @Transactional
    public void limparTokensExpirados() {
        try {
            Date agora = new Date();
            tokenInvalidadoRepository.limparTokensExpirados(agora); // Remova o tipo de retorno int
            logger.info("Limpeza de tokens expirados concluída");
        } catch (Exception e) {
            logger.error("Erro ao limpar tokens expirados: {}", e.getMessage(), e);
        }
    }

    public boolean isAdmin(Usuario usuario) {
        return ROLE_ADMIN.equals(usuario.getRole());
    }

    public boolean isProfessor(Usuario usuario) {
        return ROLE_PROFESSOR.equals(usuario.getRole());
    }
}