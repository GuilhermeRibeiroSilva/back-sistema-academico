package com.academico.espacos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.academico.espacos.model.EspacoAcademico;
import java.util.List;

public interface EspacoAcademicoRepository extends JpaRepository<EspacoAcademico, Long> {
    List<EspacoAcademico> findByDisponivelTrue();
}