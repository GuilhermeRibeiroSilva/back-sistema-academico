package com.academico.espacos.repository;

import com.academico.espacos.model.Professor;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.enums.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("SELECT r FROM Reserva r WHERE r.espacoAcademico.id = :espacoId " + 
           "AND r.data = :data " +
           "AND ((r.horaInicial < :horaFinal AND r.horaFinal > :horaInicial)) " + 
           "AND r.status != 'CANCELADO'")
    List<Reserva> findReservasConflitantes(@Param("espacoId") Long espacoId, @Param("data") LocalDate data, @Param("horaInicial") LocalTime horaInicial, @Param("horaFinal") LocalTime horaFinal);

    // Método corrigido para PostgreSQL
    @Query(value = "SELECT * FROM reservas r " +
           "WHERE r.espaco_id = :espacoId " +
           "AND r.data = :data " +
           "AND r.hora_inicial < CAST(:horaFinal AS TIME) " +
           "AND r.hora_final > CAST(:horaInicial AS TIME) " +
           "AND r.status <> 'CANCELADO'", 
           nativeQuery = true)
    List<Reserva> findConflictingReservations(
        @Param("espacoId") Long espacoId,
        @Param("data") LocalDate data,
        @Param("horaInicial") String horaInicial,
        @Param("horaFinal") String horaFinal
    );

    @Query("SELECT r FROM Reserva r ORDER BY r.data ASC, r.horaInicial ASC")
    List<Reserva> findAllOrderByDataAndHoraInicial();

    List<Reserva> findByProfessorIdAndData(Long professorId, LocalDate data);

    List<Reserva> findByEspacoAcademicoId(Long espacoAcademicoId);

    List<Reserva> findByProfessorId(Long professorId);

    @Query("SELECT r FROM Reserva r WHERE r.data = :data AND r.status = :status")
    List<Reserva> findByDataAndStatus(LocalDate data, StatusReserva status);

    @Query("SELECT r FROM Reserva r WHERE r.data = :data AND r.status IN (:status)")
    List<Reserva> findByDataAndStatusIn(LocalDate data, List<StatusReserva> status);

    @Query("UPDATE Reserva r SET r.status = :novoStatus WHERE r.id = :id")
    @Modifying
    void atualizarStatus(@Param("id") Long id, @Param("novoStatus") StatusReserva novoStatus);

    /**
     * Busca reservas que não têm o status informado
     */
    List<Reserva> findByStatusNot(StatusReserva status);

    /**
     * Busca reservas de um professor específico que não tenham o status informado
     */
    List<Reserva> findByProfessorAndStatusNot(Professor professor, StatusReserva status);

    /**
     * Verifica se uma reserva existe e tem status UTILIZADO
     */
    @Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.id = :id AND r.status = 'UTILIZADO'")
    boolean existsByIdAndStatusUtilizado(@Param("id") Long id);

    /**
     * Verifica se existem reservas utilizadas para o espaço fornecido
     */
    @Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.espacoAcademico.id = :espacoId AND r.status = 'UTILIZADO'")
    boolean existsByEspacoAcademicoIdAndStatusUtilizado(@Param("espacoId") Long espacoId);

    /**
     * Implementação manual do método para compatibilidade
     */
    @Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.espacoAcademico.id = :id AND r.status = 'UTILIZADO'")
    boolean existsByIdAndUtilizadoTrue(@Param("id") Long id);

    // Método para encontrar reservas ativas (não canceladas)
    @Query("SELECT r FROM Reserva r WHERE r.status != 'CANCELADO' ORDER BY r.data ASC, r.horaInicial ASC")
    List<Reserva> findAllActiveOrderedByDateTime();


    /**
     * Busca reservas por espaço acadêmico e data
     */
    List<Reserva> findByEspacoAcademicoIdAndData(Long espacoId, LocalDate data);

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
     * Conta reservas que conflitam com os parâmetros informados (para verificar disponibilidade)
     */
    @Query("SELECT COUNT(r) FROM Reserva r " +
           "WHERE r.data = :data " +
           "AND r.espacoAcademico.id = :espacoId " +
           "AND r.status != 'CANCELADO' " +
           "AND ((r.horaInicial <= :horaFinal AND r.horaFinal >= :horaInicial)) " +
           "AND (r.id != :reservaIdExcluir OR :reservaIdExcluir IS NULL)")
    long countConflitos(
        @Param("data") LocalDate data,
        @Param("horaInicial") LocalTime horaInicial,
        @Param("horaFinal") LocalTime horaFinal,
        @Param("espacoId") Long espacoId,
        @Param("reservaIdExcluir") Long reservaIdExcluir
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

    /**
     * Busca reservas com um determinado status, na data especificada, 
     * onde a hora inicial é menor ou igual à hora atual e a hora final é maior que a hora atual
     */
    List<Reserva> findByStatusAndDataAndHoraInicialLessThanEqualAndHoraFinalGreaterThan(
        StatusReserva status, 
        LocalDate data, 
        LocalTime horaAtualInicio, 
        LocalTime horaAtualFim
    );

    /**
     * Busca reservas com um determinado status, na data especificada, 
     * onde a hora final já passou
     */
    List<Reserva> findByStatusAndDataAndHoraFinalLessThanEqual(
        StatusReserva status, 
        LocalDate data, 
        LocalTime horaAtual
    );
}