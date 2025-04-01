package com.academico.espacos.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import com.academico.espacos.model.Usuario;
import com.academico.espacos.repository.TokenInvalidadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey jwtSecret;
    private final long jwtExpirationInMs;

    @Autowired
    private TokenInvalidadoRepository tokenInvalidadoRepository;

    public JwtTokenProvider(
            @Value("${jwt.secret:defaultSecretKeyForDevelopmentPurposesOnlyDoNotUseInProduction}") String jwtSecret,
            @Value("${jwt.expiration:86400000}") long jwtExpirationInMs) {
        this.jwtSecret = Keys.hmacShaKeyFor(Base64.getEncoder().encode(jwtSecret.getBytes()));
        this.jwtExpirationInMs = jwtExpirationInMs;
    }

    public String gerarToken(Usuario usuario) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(usuario.getUsername())
                .claim("role", usuario.getRole())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecret)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }

    public boolean validateToken(String token) {
        if (token == null) {
            return false;
        }
        
        try {
            if (tokenInvalidadoRepository.existsByToken(token)) {
                return false;
            }
            
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
                
            // Verificar se o token expirou
            return !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            // Log específico para token expirado
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            // Log para outros problemas com token
            return false;
        }
    }
}