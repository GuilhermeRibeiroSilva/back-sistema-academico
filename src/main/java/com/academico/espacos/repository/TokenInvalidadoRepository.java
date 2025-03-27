package com.academico.espacos.repository;

import com.academico.espacos.model.TokenInvalidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface TokenInvalidadoRepository extends JpaRepository<TokenInvalidado, Long> {
    boolean existsByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenInvalidado t WHERE t.dataExpiracao < :agora")
    void limparTokensExpirados(@Param("agora") LocalDateTime agora);
}