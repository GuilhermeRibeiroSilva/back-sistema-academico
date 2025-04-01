package com.academico.espacos.security;

import io.jsonwebtoken.*;
import com.academico.espacos.model.Usuario;
import com.academico.espacos.repository.TokenInvalidadoRepository;
import com.academico.espacos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs;

    @Autowired
    private TokenInvalidadoRepository tokenInvalidadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date dataExpiracao = new Date(agora.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(usuario.getId().toString())
                .claim("username", usuario.getUsername())
                .claim("role", usuario.getRole())
                .claim("professorId", usuario.getProfessor() != null ? usuario.getProfessor().getId() : null)
                .setIssuedAt(agora)
                .setExpiration(dataExpiracao)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    public Usuario getUsuarioFromToken(String token) {
        Long userId = getUserIdFromToken(token);
        return usuarioRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }

    /**
     * Extrai o nome de usuário do token JWT.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
                
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
            
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            // Assinatura inválida
            return false;
        } catch (MalformedJwtException ex) {
            // Token mal formado
            return false;
        } catch (ExpiredJwtException ex) {
            // Token expirado
            return false;
        } catch (UnsupportedJwtException ex) {
            // Token não suportado
            return false;
        } catch (IllegalArgumentException ex) {
            // Claims vazio
            return false;
        }
    }

    public boolean validarToken(String token) {
        try {
            // Verificar se o token foi invalidado (logout)
            if (tokenInvalidadoRepository.findByToken(token).isPresent()) {
                return false;
            }

            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            // Assinatura inválida
            return false;
        } catch (MalformedJwtException ex) {
            // Token mal formado
            return false;
        } catch (ExpiredJwtException ex) {
            // Token expirado
            return false;
        } catch (UnsupportedJwtException ex) {
            // Token não suportado
            return false;
        } catch (IllegalArgumentException ex) {
            // Claims vazio
            return false;
        }
    }

    public Date getExpiracaoToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }
}