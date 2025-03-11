package com.academico.espacos.repository;

import com.academico.espacos.model.RecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {
    Optional<RecuperacaoSenha> findByTokenAndUtilizadoFalse(String token);
}