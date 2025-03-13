package com.academico.espacos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.academico.espacos.model.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("SELECT r FROM Reserva r WHERE r.espacoAcademico.id = :espacoId " +
           "AND r.data = :data " +
           "AND ((r.horaInicial <= :horaFinal AND r.horaFinal >= :horaInicial))")
    List<Reserva> findReservasConflitantes(
        Long espacoId, 
        LocalDate data, 
        LocalTime horaInicial, 
        LocalTime horaFinal
    );
	List<Reserva> findByEspacoAcademicoId(Long espacoAcademicoId);
    
}