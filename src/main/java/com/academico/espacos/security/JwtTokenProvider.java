package com.academico.espacos.security;

import io.jsonwebtoken.*;
import com.academico.espacos.model.Usuario;
import com.academico.espacos.repository.TokenInvalidadoRepository;
import com.academico.espacos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;

import java.util.Date;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    @Autowired
    private TokenInvalidadoRepository tokenInvalidadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date dataExpiracao = new Date(agora.getTime() + jwtExpirationInMs);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(usuario.getId().toString())
                .claim("username", usuario.getUsername())
                .claim("role", usuario.getRole())
                .claim("professorId", usuario.getProfessor() != null ? usuario.getProfessor().getId() : null)
                .setIssuedAt(agora)
                .setExpiration(dataExpiracao)
                .signWith(key)
                .compact();
    }

    /**
     * Extrai as claims do token JWT.
     * @throws JwtException se o token for inválido
     */
    public Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public Usuario getUsuarioFromToken(String token) {
        Long userId = getUserIdFromToken(token);
        return usuarioRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    public String getRoleFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extrai o nome de usuário do token JWT.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * Valida um token JWT.
     */
    public boolean validateToken(String token) {
        try {
            // Verificar se o token foi invalidado (logout)
            if (tokenInvalidadoRepository.findByToken(token).isPresent()) {
                return false;
            }
            
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Date getExpiracaoToken(String token) {
        Claims claims = extractClaims(token);
        return claims.getExpiration();
    }
}