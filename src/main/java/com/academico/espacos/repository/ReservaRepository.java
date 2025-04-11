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

/**
 * Repositório para gerenciamento das entidades Reserva
 */
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // ========== CONSULTAS DE CONFLITO DE HORÁRIOS ==========
    
    /**
     * Verifica se existe conflito de horário para um espaço acadêmico específico
     */
    @Query("SELECT COUNT(r) > 0 FROM Reserva r " +
           "WHERE r.espacoAcademico.id = :espacoId " +
           "AND r.data = :data " +
           "AND r.status != 'CANCELADO' " +
           "AND ((r.horaInicial < :horaFinal AND r.horaFinal > :horaInicial))")
    boolean existsConflitoPorEspaco(
        @Param("espacoId") Long espacoId, 
        @Param("data") LocalDate data, 
        @Param("horaInicial") LocalTime horaInicial, 
        @Param("horaFinal") LocalTime horaFinal
    );
    
    /**
     * Retorna as reservas conflitantes para um determinado espaço e horário
     */
    @Query("SELECT r FROM Reserva r WHERE r.espacoAcademico.id = :espacoId " + 
           "AND r.data = :data " +
           "AND ((r.horaInicial < :horaFinal AND r.horaFinal > :horaInicial)) " + 
           "AND r.status != 'CANCELADO'")
    List<Reserva> findReservasConflitantes(
        @Param("espacoId") Long espacoId, 
        @Param("data") LocalDate data, 
        @Param("horaInicial") LocalTime horaInicial, 
        @Param("horaFinal") LocalTime horaFinal
    );
    
    /**
     * Verifica se existe conflito de horário para um professor específico
     */
    @Query("SELECT COUNT(r) > 0 FROM Reserva r " +
           "WHERE r.professor.id = :professorId " +
           "AND r.data = :data " +
           "AND r.status != 'CANCELADO' " +
           "AND ((r.horaInicial < :horaFinal AND r.horaFinal > :horaInicial))")
    boolean existsConflitoPorProfessor(
        @Param("professorId") Long professorId, 
        @Param("data") LocalDate data, 
        @Param("horaInicial") LocalTime horaInicial, 
        @Param("horaFinal") LocalTime horaFinal
    );
    
    /**
     * Conta reservas que conflitam com os parâmetros informados (permitindo excluir uma reserva específica)
     */
    @Query("SELECT COUNT(r) FROM Reserva r " +
           "WHERE r.data = :data " +
           "AND r.espacoAcademico.id = :espacoId " +
           "AND r.status != 'CANCELADO' " +
           "AND ((r.horaInicial < :horaFinal AND r.horaFinal > :horaInicial)) " +
           "AND (r.id != :reservaIdExcluir OR :reservaIdExcluir IS NULL)")
    long countConflitos(
        @Param("data") LocalDate data,
        @Param("horaInicial") LocalTime horaInicial,
        @Param("horaFinal") LocalTime horaFinal,
        @Param("espacoId") Long espacoId,
        @Param("reservaIdExcluir") Long reservaIdExcluir
    );

    // ========== CONSULTAS POR ORDENAÇÃO ==========
    
    /**
     * Retorna todas as reservas ordenadas por data e hora
     */
    @Query("SELECT r FROM Reserva r ORDER BY r.data ASC, r.horaInicial ASC")
    List<Reserva> findAllOrderByDataAndHoraInicial();
    
    /**
     * Retorna todas as reservas ativas (não canceladas) ordenadas por data e hora
     */
    @Query("SELECT r FROM Reserva r WHERE r.status != 'CANCELADO' ORDER BY r.data ASC, r.horaInicial ASC")
    List<Reserva> findAllActiveOrderedByDateTime();

    // ========== CONSULTAS POR DATA/HORA ==========
    
    /**
     * Busca reservas para uma data específica dentro de um intervalo de horas
     */
    @Query("SELECT r FROM Reserva r " +
           "WHERE r.data = :data " +
           "AND r.status != 'CANCELADO' " +
           "AND ((r.horaInicial < :horaFinal AND r.horaFinal > :horaInicial))")
    List<Reserva> findReservasEmIntervalo(
        @Param("data") LocalDate data,
        @Param("horaInicial") LocalTime horaInicial,
        @Param("horaFinal") LocalTime horaFinal
    );
    
    /**
     * Busca reservas em andamento (com status especificado, na data atual, 
     * onde a hora está entre o início e fim da reserva)
     */
    List<Reserva> findByStatusAndDataAndHoraInicialLessThanEqualAndHoraFinalGreaterThan(
        StatusReserva status, 
        LocalDate data, 
        LocalTime horaAtual, 
        LocalTime horaAtual2
    );
    
    /**
     * Busca reservas finalizadas (com status especificado, na data atual,
     * onde a hora fim já passou)
     */
    List<Reserva> findByStatusAndDataAndHoraFinalLessThanEqual(
        StatusReserva status, 
        LocalDate data, 
        LocalTime horaAtual
    );

    // ========== CONSULTAS POR RELACIONAMENTOS ==========
    
    /**
     * Busca reservas por professor e data
     */
    List<Reserva> findByProfessorIdAndData(Long professorId, LocalDate data);
    
    /**
     * Busca reservas por espaço acadêmico
     */
    List<Reserva> findByEspacoAcademicoId(Long espacoAcademicoId);
    
    /**
     * Busca reservas por professor
     */
    List<Reserva> findByProfessorId(Long professorId);
    
    /**
     * Busca reservas por espaço acadêmico e data
     */
    List<Reserva> findByEspacoAcademicoIdAndData(Long espacoId, LocalDate data);

    // ========== CONSULTAS POR STATUS ==========
    
    /**
     * Busca reservas por data e status
     */
    @Query("SELECT r FROM Reserva r WHERE r.data = :data AND r.status = :status")
    List<Reserva> findByDataAndStatus(LocalDate data, StatusReserva status);
    
    /**
     * Busca reservas por data e múltiplos status
     */
    @Query("SELECT r FROM Reserva r WHERE r.data = :data AND r.status IN (:status)")
    List<Reserva> findByDataAndStatusIn(LocalDate data, List<StatusReserva> status);
    
    /**
     * Busca reservas que não têm o status informado
     */
    List<Reserva> findByStatusNot(StatusReserva status);
    
    /**
     * Busca reservas de um professor específico que não tenham o status informado
     */
    List<Reserva> findByProfessorAndStatusNot(Professor professor, StatusReserva status);
    
    /**
     * Busca reservas por status
     */
    List<Reserva> findByStatus(StatusReserva status);
    
    /**
     * Busca reservas por data
     */
    List<Reserva> findByData(LocalDate data);
    
    /**
     * Busca reservas por status que tenham data anterior à data informada
     */
    List<Reserva> findByDataBeforeAndStatus(LocalDate data, StatusReserva status);

    // ========== VERIFICAÇÃO DE EXISTÊNCIA ==========
    
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

    // ========== OPERAÇÕES DE MODIFICAÇÃO ==========
    
    /**
     * Atualiza o status de uma reserva
     */
    @Query("UPDATE Reserva r SET r.status = :novoStatus WHERE r.id = :id")
    @Modifying
    void atualizarStatus(@Param("id") Long id, @Param("novoStatus") StatusReserva novoStatus);

    /**
     * Retorna as reservas conflitantes para um determinado espaço e horário
     */
    @Query("SELECT r FROM Reserva r WHERE r.espacoAcademico.id = :espacoId " + 
           "AND r.data = :data " +
           "AND ((r.horaInicial < :horaFinal AND r.horaFinal > :horaInicial)) " + 
           "AND r.status != 'CANCELADO'")
    List<Reserva> findConflictingReservations(
        @Param("espacoId") Long espacoId, 
        @Param("data") LocalDate data, 
        @Param("horaInicial") LocalTime horaInicial, 
        @Param("horaFinal") LocalTime horaFinal
    );
}