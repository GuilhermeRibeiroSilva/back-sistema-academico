package com.academico.espacos.repository;

import com.academico.espacos.model.TokenInvalidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

public interface TokenInvalidadoRepository extends JpaRepository<TokenInvalidado, Long> {
    Optional<TokenInvalidado> findByToken(String token);
    
    boolean existsByToken(String token);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenInvalidado t WHERE t.expiracaoToken < :agora")
    void limparTokensExpirados(@Param("agora") Date agora);
}