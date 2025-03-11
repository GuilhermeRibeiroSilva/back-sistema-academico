package com.academico.espacos.service;

import com.academico.espacos.model.*;
import com.academico.espacos.repository.*;
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

@Service
public class AuthService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private RecuperacaoSenhaRepository recuperacaoSenhaRepository;

    @Autowired
    private TokenInvalidadoRepository tokenInvalidadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

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

    @Transactional
    public void solicitarRecuperacaoSenha(String email) {
        Usuario usuario = usuarioRepository.findByUsername(email)
            .orElseThrow(() -> new AuthenticationException("Email não encontrado"));

        String token = gerarTokenRecuperacao();
        
        RecuperacaoSenha recuperacao = new RecuperacaoSenha();
        recuperacao.setToken(token);
        recuperacao.setUsuario(usuario);
        recuperacao.setExpiracao(LocalDateTime.now().plusHours(24));
        
        recuperacaoSenhaRepository.save(recuperacao);

        enviarEmailRecuperacao(usuario.getUsername(), token);
    }

    @Transactional
    public void recuperarSenha(String token, String novaSenha) {
        RecuperacaoSenha recuperacao = recuperacaoSenhaRepository
            .findByTokenAndUtilizadoFalse(token)
            .orElseThrow(() -> new AuthenticationException("Token inválido ou já utilizado"));

        if (recuperacao.getExpiracao().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("Token expirado");
        }

        Usuario usuario = recuperacao.getUsuario();
        usuario.setPassword(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        recuperacao.setUtilizado(true);
        recuperacaoSenhaRepository.save(recuperacao);
    }

    @Transactional
    public void logout(String token) {
        TokenInvalidado tokenInvalidado = new TokenInvalidado();
        tokenInvalidado.setToken(token);
        tokenInvalidado.setDataInvalidacao(LocalDateTime.now());
        tokenInvalidado.setDataExpiracao(jwtTokenProvider.getExpiracaoFromToken(token));
        
        tokenInvalidadoRepository.save(tokenInvalidado);
        
        limparTokensExpirados();
    }

    @Scheduled(cron = "0 0 * * * *") // Executa a cada hora
    public void limparTokensExpirados() {
        tokenInvalidadoRepository.limparTokensExpirados(LocalDateTime.now());
    }

    private void validarNovoUsername(String username) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new AuthenticationException("Username já existe");
        }
    }

    private String gerarTokenRecuperacao() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void enviarEmailRecuperacao(String email, String token) {
        String linkRecuperacao = "http://seu-site.com/recuperar-senha?token=" + token;
        String conteudoEmail = String.format(
            "Olá,\n\nPara recuperar sua senha, acesse o link: %s\n\nO link expira em 24 horas.",
            linkRecuperacao
        );

        emailService.enviarEmail(
            email,
            "Recuperação de Senha",
            conteudoEmail
        );
    }

    public boolean isAdmin(Usuario usuario) {
        return "ROLE_ADMIN".equals(usuario.getRole());
    }

    public boolean isProfessor(Usuario usuario) {
        return "ROLE_PROFESSOR".equals(usuario.getRole());
    }
}