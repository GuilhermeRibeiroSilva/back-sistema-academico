package com.academico.espacos.service;

import com.academico.espacos.dto.ReservaDTO;
import com.academico.espacos.dto.ReservaInputDTO;
import com.academico.espacos.exception.BusinessException;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.model.Professor;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.enums.StatusReserva;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.repository.ProfessorRepository;
import com.academico.espacos.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservaService {
    
    @Autowired
    private ReservaRepository reservaRepository;
    
    @Autowired
    private EspacoAcademicoRepository espacoRepository;
    
    @Autowired
    private ProfessorRepository professorRepository;
    
    /**
     * Busca todas as reservas ativas (não canceladas)
     */
    public List<ReservaDTO> buscarTodasAtivas() {
        return reservaRepository.findByStatusNot(StatusReserva.CANCELADO)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Busca reservas de um professor específico
     */
    public List<ReservaDTO> buscarPorProfessor(Long professorId) {
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
                
        return reservaRepository.findByProfessorAndStatusNot(professor, StatusReserva.CANCELADO)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Busca uma reserva por ID
     */
    public ReservaDTO buscarPorId(Long id) {
        return toDTO(getReservaById(id));
    }
    
    /**
     * Busca uma entidade Reserva por ID (auxiliar)
     */
    private Reserva getReservaById(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
    }
    
    /**
     * Cria uma nova reserva
     */
    @Transactional
    public ReservaDTO criar(ReservaInputDTO input) {
        validarDados(input, null);
        
        Reserva reserva = new Reserva();
        preencherDadosReserva(reserva, input);
        reserva.setStatus(StatusReserva.PENDENTE);
        
        return toDTO(reservaRepository.save(reserva));
    }
    
    /**
     * Atualiza uma reserva existente
     */
    @Transactional
    public ReservaDTO atualizar(Long id, ReservaInputDTO input) {
        Reserva reserva = getReservaById(id);
        
        if (!reserva.podeSerEditada()) {
            throw new BusinessException("Esta reserva não pode mais ser alterada");
        }
        
        validarDados(input, id);
        preencherDadosReserva(reserva, input);
        
        return toDTO(reservaRepository.save(reserva));
    }
    
    /**
     * Cancela uma reserva
     */
    @Transactional
    public void cancelar(Long id) {
        Reserva reserva = getReservaById(id);
        
        if (!reserva.getStatus().podeSerCancelada()) {
            throw new BusinessException("Esta reserva não pode ser cancelada");
        }
        
        reserva.cancelar();
        reservaRepository.save(reserva);
    }
    
    /**
     * Confirma a utilização de uma reserva
     */
    @Transactional
    public void confirmarUtilizacao(Long id) {
        Reserva reserva = getReservaById(id);
        
        if (!reserva.getStatus().podeConfirmarUtilizacao()) {
            throw new BusinessException("Não é possível confirmar a utilização desta reserva");
        }
        
        reserva.confirmarUtilizacao();
        reservaRepository.save(reserva);
    }
    
    /**
     * Verifica disponibilidade para uma nova reserva
     */
    public boolean verificarDisponibilidade(LocalDate data, LocalTime horaInicial, LocalTime horaFinal, Long espacoId, Long reservaIdExcluir) {
        return reservaRepository.countConflitos(data, horaInicial, horaFinal, espacoId, reservaIdExcluir) == 0;
    }
    
    /**
     * Job que atualiza automaticamente os status das reservas
     * Executa a cada 10 minutos
     */
    @Scheduled(fixedRate = 600000) // 10 minutos
    @Transactional
    public void atualizarStatusReservas() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDate hoje = agora.toLocalDate();
        LocalTime horaAtual = agora.toLocalTime();
        
        // Atualiza reservas PENDENTES para EM_USO
        List<Reserva> reservasPendentes = reservaRepository.findByStatusAndDataAndHoraInicialLessThanEqualAndHoraFinalGreaterThan(
                StatusReserva.PENDENTE, hoje, horaAtual, horaAtual);
                
        for (Reserva r : reservasPendentes) {
            r.setStatus(StatusReserva.EM_USO);
            reservaRepository.save(r);
        }
        
        // Atualiza reservas EM_USO para AGUARDANDO_CONFIRMACAO
        List<Reserva> reservasEmUso = reservaRepository.findByStatusAndDataAndHoraFinalLessThanEqual(
                StatusReserva.EM_USO, hoje, horaAtual);
                
        for (Reserva r : reservasEmUso) {
            r.setStatus(StatusReserva.AGUARDANDO_CONFIRMACAO);
            reservaRepository.save(r);
        }
    }
    
    /**
     * Preenche os dados de uma reserva a partir de um DTO
     */
    private void preencherDadosReserva(Reserva reserva, ReservaInputDTO input) {
        EspacoAcademico espaco = espacoRepository.findById(input.getEspacoAcademicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));
                
        Professor professor = professorRepository.findById(input.getProfessorId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
                
        reserva.setEspacoAcademico(espaco);
        reserva.setProfessor(professor);
        reserva.setData(input.getData());
        reserva.setHoraInicial(input.getHoraInicial());
        reserva.setHoraFinal(input.getHoraFinal());
        reserva.setFinalidade(input.getFinalidade());
    }
    
    /**
     * Valida os dados de uma reserva
     */
    private void validarDados(ReservaInputDTO input, Long idReservaAtual) {
        // Validação de horários
        if (input.getHoraInicial().isAfter(input.getHoraFinal()) || input.getHoraInicial().equals(input.getHoraFinal())) {
            throw new BusinessException("Hora inicial deve ser anterior à hora final");
        }
        
        // Validação de data
        if (input.getData().isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível criar reservas para datas passadas");
        }
        
        // Verificar disponibilidade
        if (!verificarDisponibilidade(
                input.getData(), 
                input.getHoraInicial(), 
                input.getHoraFinal(), 
                input.getEspacoAcademicoId(), 
                idReservaAtual)) {
            throw new BusinessException("Já existe uma reserva para este espaço no horário solicitado");
        }
    }
    
    /**
     * Converte entidade para DTO
     */
    private ReservaDTO toDTO(Reserva reserva) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(reserva.getId());
        dto.setEspacoAcademico(reserva.getEspacoAcademico());
        dto.setProfessor(reserva.getProfessor());
        dto.setData(reserva.getData());
        dto.setHoraInicial(reserva.getHoraInicial());
        dto.setHoraFinal(reserva.getHoraFinal());
        dto.setFinalidade(reserva.getFinalidade());
        dto.setStatus(reserva.getStatus());
        dto.setDataCriacao(reserva.getDataCriacao());
        dto.setDataAtualizacao(reserva.getDataAtualizacao());
        dto.setDataUtilizacao(reserva.getDataUtilizacao());
        return dto;
    }
}