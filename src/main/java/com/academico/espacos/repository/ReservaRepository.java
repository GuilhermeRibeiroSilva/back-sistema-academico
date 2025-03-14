package com.academico.espacos.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.academico.espacos.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

	@Query("SELECT r FROM Reserva r WHERE r.espacoAcademico.id = :espacoId " +
	           "AND r.data = :data " +
	           "AND ((r.horaInicial < :horaFinal AND r.horaFinal > :horaInicial)) " +
	           "AND r.utilizado = false")
	    List<Reserva> findReservasConflitantes(
	        Long espacoId, 
	        LocalDate data, 
	        LocalTime horaInicial, 
	        LocalTime horaFinal
	    );
	List<Reserva> findByEspacoAcademicoId(Long espacoAcademicoId);
	 List<Reserva> findByProfessorId(Long professorId);
	 boolean existsByIdAndUtilizadoTrue(Long id);
    
}