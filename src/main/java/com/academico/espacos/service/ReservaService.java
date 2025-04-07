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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ReservaService.class);
    
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
     * Método para atualizar automaticamente o status das reservas
     * Corrigido para lidar corretamente com a mudança de status
     */
    @Scheduled(fixedRate = 60000) // Verifica a cada minuto
    @Transactional
    public void atualizarStatusReservas() {
        try {
            LocalDate hoje = LocalDate.now();
            LocalTime agora = LocalTime.now();
            
            logger.info("Atualizando status das reservas em: {}", LocalDateTime.now());
            
            // Buscar todas as reservas pendentes para hoje
            List<Reserva> reservasPendentes = reservaRepository.findByDataAndStatus(hoje, StatusReserva.PENDENTE);
            
            for (Reserva reserva : reservasPendentes) {
                // Obter os horários que já são LocalTime na entidade
                LocalTime horaInicial = reserva.getHoraInicial();
                LocalTime horaFinal = reserva.getHoraFinal();
                
                // Verificar se hora inicial foi invertida com hora final na base
                if (horaInicial.isAfter(horaFinal)) {
                    // Corrigir a inversão
                    LocalTime temp = horaInicial;
                    reserva.setHoraInicial(horaFinal);
                    reserva.setHoraFinal(temp);
                    
                    // Atualizar referências
                    horaInicial = reserva.getHoraInicial();
                    horaFinal = reserva.getHoraFinal();
                }
                
                // Se chegou na hora inicial, mudar para EM_USO
                if (agora.isAfter(horaInicial) || agora.equals(horaInicial)) {
                    reserva.setStatus(StatusReserva.EM_USO);
                    reserva.setDataAtualizacao(LocalDateTime.now());
                    logger.info("Reserva #{} atualizada para EM_USO", reserva.getId());
                    reservaRepository.save(reserva);
                }
            }
            
            // Buscar todas as reservas em uso para hoje
            List<Reserva> reservasEmUso = reservaRepository.findByDataAndStatus(hoje, StatusReserva.EM_USO);
            
            for (Reserva reserva : reservasEmUso) {
                // Obter o horário final que já é LocalTime na entidade
                LocalTime horaFinal = reserva.getHoraFinal();
                
                // Se passou da hora final, mudar para AGUARDANDO_CONFIRMACAO
                if (agora.isAfter(horaFinal) || agora.equals(horaFinal)) {
                    // Primeiro verificamos se a entidade está correta antes de alterar
                    // o status para evitar violação da restrição
                    if (reserva.getHoraInicial() != null && reserva.getHoraFinal() != null &&
                        reserva.getEspacoAcademico() != null && reserva.getProfessor() != null) {
                        
                        // Verificar novamente a ordem das horas
                        LocalTime horaInicial = reserva.getHoraInicial();
                        if (horaInicial.isAfter(horaFinal)) {
                            LocalTime temp = horaInicial;
                            reserva.setHoraInicial(horaFinal);
                            reserva.setHoraFinal(temp);
                        }
                        
                        // Atualizar status
                        reserva.setStatus(StatusReserva.AGUARDANDO_CONFIRMACAO);
                        reserva.setDataAtualizacao(LocalDateTime.now());
                        logger.info("Reserva #{} atualizada para AGUARDANDO_CONFIRMACAO", reserva.getId());
                        reservaRepository.save(reserva);
                    } else {
                        logger.error("Não foi possível atualizar a reserva #{} para AGUARDANDO_CONFIRMACAO devido a dados inválidos", reserva.getId());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao atualizar status das reservas: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Busca reservas por espaço acadêmico e data
     */
    public List<Reserva> buscarPorEspacoEData(Long espacoId, LocalDate data) {
        // Verificar se o espaço existe
        EspacoAcademico espaco = espacoRepository.findById(espacoId)
                .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));
        
        // Buscar reservas
        return reservaRepository.findByEspacoAcademicoIdAndData(espacoId, data);
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
        
        if (reserva.getEspacoAcademico() != null) {
            dto.setEspacoAcademico(reserva.getEspacoAcademico());
            dto.setEspacoAcademicoId(reserva.getEspacoAcademico().getId());
        }
        
        if (reserva.getProfessor() != null) {
            dto.setProfessor(reserva.getProfessor());
            dto.setProfessorId(reserva.getProfessor().getId());
        }
        
        dto.setData(reserva.getData());
        
        // Converter LocalTime para String
        if (reserva.getHoraInicial() != null) {
            dto.setHoraInicial(reserva.getHoraInicial().toString());
        }
        
        if (reserva.getHoraFinal() != null) {
            dto.setHoraFinal(reserva.getHoraFinal().toString());
        }
        
        dto.setFinalidade(reserva.getFinalidade());
        dto.setStatus(reserva.getStatus());
        dto.setDataCriacao(reserva.getDataCriacao());
        dto.setDataAtualizacao(reserva.getDataAtualizacao());
        dto.setDataUtilizacao(reserva.getDataUtilizacao());
        
        return dto;
    }
    
    /**
     * Cria uma nova reserva a partir de um DTO
     */
    @Transactional
    public Reserva criarReserva(ReservaDTO reservaDTO) {
        // Validar e converter dados do DTO
        Reserva reserva = new Reserva();
        
        // Buscar entidades relacionadas
        EspacoAcademico espaco = espacoRepository.findById(reservaDTO.getEspacoAcademicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));
        
        Professor professor = professorRepository.findById(reservaDTO.getProfessorId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
        
        // Preencher a reserva
        reserva.setEspacoAcademico(espaco);
        reserva.setProfessor(professor);
        reserva.setData(reservaDTO.getData());
        
        // Converter String para LocalTime
        if (reservaDTO.getHoraInicial() != null) {
            reserva.setHoraInicial(LocalTime.parse(reservaDTO.getHoraInicial()));
        }
        
        if (reservaDTO.getHoraFinal() != null) {
            reserva.setHoraFinal(LocalTime.parse(reservaDTO.getHoraFinal()));
        }
        
        reserva.setFinalidade(reservaDTO.getFinalidade());
        reserva.setStatus(StatusReserva.PENDENTE); // Nova reserva sempre começa como pendente
        reserva.setDataCriacao(LocalDateTime.now());
        reserva.setDataAtualizacao(LocalDateTime.now());
        
        // Verificar se já existe reserva para o mesmo espaço, data e horário que se sobreponha
        verificarDisponibilidade(reserva);
        
        // Salvar e retornar
        return reservaRepository.save(reserva);
    }
}