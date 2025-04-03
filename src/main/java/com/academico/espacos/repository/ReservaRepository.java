package com.academico.espacos.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.enums.StatusReserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("SELECT r FROM Reserva r WHERE r.espacoAcademico.id = :espacoId " + 
           "AND r.data = :data " +
           "AND ((r.horaInicial < :horaFinal AND r.horaFinal > :horaInicial)) " + 
           "AND r.status != 'CANCELADO'")
    List<Reserva> findReservasConflitantes(@Param("espacoId") Long espacoId, @Param("data") LocalDate data, @Param("horaInicial") LocalTime horaInicial, @Param("horaFinal") LocalTime horaFinal);

    // Método que aceita strings para horários
    @Query("SELECT r FROM Reserva r WHERE r.espacoAcademico.id = :espacoId " + 
           "AND r.data = :data " +
           "AND r.horaInicial < FUNCTION('STR_TO_DATE', :horaFinal, '%H:%i') " +
           "AND r.horaFinal > FUNCTION('STR_TO_DATE', :horaInicial, '%H:%i') " + 
           "AND r.status != 'CANCELADO'")
    List<Reserva> findConflictingReservations(@Param("espacoId") Long espacoId, @Param("data") LocalDate data, @Param("horaInicial") String horaInicial, @Param("horaFinal") String horaFinal);

    @Query("SELECT r FROM Reserva r ORDER BY r.data ASC, r.horaInicial ASC")
    List<Reserva> findAllOrderByDataAndHoraInicial();

    List<Reserva> findByProfessorIdAndData(Long professorId, LocalDate data);

    List<Reserva> findByEspacoAcademicoId(Long espacoAcademicoId);

    List<Reserva> findByProfessorId(Long professorId);

    boolean existsByIdAndUtilizadoTrue(Long id);

    @Query("SELECT r FROM Reserva r WHERE r.data = :data AND r.status = :status")
    List<Reserva> findByDataAndStatus(LocalDate data, StatusReserva status);

    @Query("SELECT r FROM Reserva r WHERE r.data = :data AND r.status IN (:status)")
    List<Reserva> findByDataAndStatusIn(LocalDate data, List<StatusReserva> status);

    @Query("UPDATE Reserva r SET r.status = :novoStatus WHERE r.id = :id")
    @Modifying
    void atualizarStatus(@Param("id") Long id, @Param("novoStatus") StatusReserva novoStatus);

    // Adicionar método para encontrar reservas por status diferente de um valor
    List<Reserva> findByStatusNot(StatusReserva status);

    // Método para encontrar reservas ativas (não canceladas)
    @Query("SELECT r FROM Reserva r WHERE r.status != 'CANCELADO' ORDER BY r.data ASC, r.horaInicial ASC")
    List<Reserva> findAllActiveOrderedByDateTime();

    /**
     * Busca reservas por espaço acadêmico e data
     */
    List<Reserva> findByEspacoAcademicoIdAndData(Long espacoAcademicoId, LocalDate data);

    /**
     * Verifica se existe alguma reserva para o espaço acadêmico na data/hora especificada
     */
    @Query("SELECT COUNT(r) > 0 FROM Reserva r " +
           "WHERE r.espacoAcademico.id = :espacoId " +
           "AND r.data = :data " +
           "AND r.status != 'CANCELADO' " +
           "AND (" +
           "  (r.horaInicial <= :horaInicial AND r.horaFinal > :horaInicial) OR " +
           "  (r.horaInicial < :horaFinal AND r.horaFinal >= :horaFinal) OR " +
           "  (r.horaInicial >= :horaInicial AND r.horaFinal <= :horaFinal)" +
           ")")
    boolean existsReservaConflitante(
        @Param("espacoId") Long espacoId, 
        @Param("data") LocalDate data, 
        @Param("horaInicial") LocalTime horaInicial, 
        @Param("horaFinal") LocalTime horaFinal
    );

    /**
     * Verifica se existe alguma reserva para o professor na data/hora especificada
     */
    @Query("SELECT COUNT(r) > 0 FROM Reserva r " +
           "WHERE r.professor.id = :professorId " +
           "AND r.data = :data " +
           "AND r.status != 'CANCELADO' " +
           "AND (" +
           "  (r.horaInicial <= :horaInicial AND r.horaFinal > :horaInicial) OR " +
           "  (r.horaInicial < :horaFinal AND r.horaFinal >= :horaFinal) OR " +
           "  (r.horaInicial >= :horaInicial AND r.horaFinal <= :horaFinal)" +
           ")")
    boolean existsReservaProfessorConflitante(
        @Param("professorId") Long professorId, 
        @Param("data") LocalDate data, 
        @Param("horaInicial") LocalTime horaInicial, 
        @Param("horaFinal") LocalTime horaFinal
    );

    /**
     * Busca reservas por status
     */
    List<Reserva> findByStatus(StatusReserva status);

    /**
     * Busca reservas por data
     */
    List<Reserva> findByData(LocalDate data);

    /**
     * Busca reservas para uma data específica
     * que estão dentro de um intervalo de horas
     */
    @Query("SELECT r FROM Reserva r " +
           "WHERE r.data = :data " +
           "AND r.status != 'CANCELADO' " +
           "AND (" +
           "  (r.horaInicial >= :horaInicial AND r.horaInicial < :horaFinal) OR " +
           "  (r.horaFinal > :horaInicial AND r.horaFinal <= :horaFinal) OR " +
           "  (r.horaInicial <= :horaInicial AND r.horaFinal >= :horaFinal)" +
           ")")
    List<Reserva> findReservasEmIntervalo(
        @Param("data") LocalDate data,
        @Param("horaInicial") LocalTime horaInicial,
        @Param("horaFinal") LocalTime horaFinal
    );
}