package com.academico.espacos.repository;

import com.academico.espacos.model.Usuario;
import com.academico.espacos.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByProfessor(Professor professor);
}