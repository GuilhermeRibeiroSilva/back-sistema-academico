package com.academico.espacos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.academico.espacos.model.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
}