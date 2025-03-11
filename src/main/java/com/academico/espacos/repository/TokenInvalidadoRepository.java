package com.academico.espacos.repository;

import com.academico.espacos.model.TokenInvalidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface TokenInvalidadoRepository extends JpaRepository<TokenInvalidado, Long> {
    boolean existsByToken(String token);
    
    @Query("DELETE FROM TokenInvalidado t WHERE t.dataExpiracao < :agora")
    void limparTokensExpirados(LocalDateTime agora);
}